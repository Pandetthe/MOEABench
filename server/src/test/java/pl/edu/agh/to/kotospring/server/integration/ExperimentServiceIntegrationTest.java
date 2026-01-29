package pl.edu.agh.to.kotospring.server.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.Application;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentGroup;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartExecution;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartSolution;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentGroupRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartExecutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartSolutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRunRepository;
import pl.edu.agh.to.kotospring.server.services.implementation.AlgorithmRegistryServiceImpl;
import pl.edu.agh.to.kotospring.server.services.implementation.IndicatorRegistryServiceImpl;
import pl.edu.agh.to.kotospring.server.services.implementation.ProblemRegistryServiceImpl;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentExecutionService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ExperimentServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService experimentService;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ExperimentRunRepository experimentRunRepository;

    @Autowired
    private ExperimentPartRepository experimentPartRepository;

    @Autowired
    private ExperimentPartExecutionRepository experimentPartExecutionRepository;

    @Autowired
    private ExperimentPartSolutionRepository experimentPartSolutionRepository;

    @Autowired
    private ExperimentGroupRepository experimentGroupRepository;

    @MockBean
    private ProblemRegistryServiceImpl problemRegistry;

    @MockBean
    private AlgorithmRegistryServiceImpl algorithmRegistry;

    @MockBean
    private IndicatorRegistryServiceImpl indicatorRegistry;

    @MockBean
    private ExperimentExecutionService executionService;

    @BeforeEach
    void cleanDatabase() {
        // given
        experimentGroupRepository.deleteAll();
        experimentPartSolutionRepository.deleteAll();
        experimentPartExecutionRepository.deleteAll();
        experimentRunRepository.deleteAll();
        experimentPartRepository.deleteAll();
        experimentRepository.deleteAll();

        clearInvocations(problemRegistry, algorithmRegistry, indicatorRegistry, executionService);
    }

    private CreateExperimentRequest createRequestTwoParts(long runCount, String problem, String algorithm) {
        CreateExperimentRequestData firstPart = new CreateExperimentRequestData(
                problem,
                algorithm,
                Map.of("populationSize", 50),
                Set.of("HV", "IGD"),
                100
        );

        CreateExperimentRequestData secondPart = new CreateExperimentRequestData(
                problem,
                algorithm,
                Map.of(),
                Set.of("HV"),
                200
        );

        return new CreateExperimentRequest(List.of(firstPart, secondPart), runCount);
    }

    private CreateExperimentRequest createRequestTwoParts(long runCount) {
        return createRequestTwoParts(runCount, "ZDT1", "NSGAII");
    }

    private void givenValidRegistryMocks() {
        // given
        Problem mockProblem = mock(Problem.class);
        Algorithm mockAlgorithm = mock(Algorithm.class);
        Indicators mockIndicators = mock(Indicators.class);
        NondominatedPopulation mockReferenceSet = mock(NondominatedPopulation.class);

        when(problemRegistry.getProblem(anyString())).thenReturn(Optional.of(mockProblem));
        when(problemRegistry.getReferenceSet(anyString())).thenReturn(Optional.of(mockReferenceSet));

        when(algorithmRegistry.createTypedProperties(anyMap()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) inv.getArgument(0);
                    Properties props = new Properties();
                    props.putAll(params);
                    return new TypedProperties(props);
                });

        when(algorithmRegistry.getAlgorithm(anyString(), any(TypedProperties.class), eq(mockProblem)))
                .thenReturn(Optional.of(mockAlgorithm));

        when(indicatorRegistry.getIndicators(anySet(), eq(mockProblem), eq(mockReferenceSet)))
                .thenReturn(mockIndicators);
    }

    private List<ExperimentPartExecution> getExecutions(long experimentId, long runNo) {
        return experimentPartExecutionRepository.findAllByExperimentRunId(new RunId(experimentId, runNo));
    }

    @Test
    void createExperiment_persistsExperimentRunsPartsAndExecutions() {
        // given
        givenValidRegistryMocks();

        // when
        Experiment createdExperiment = experimentService.createExperiment(createRequestTwoParts(2));
        long experimentId = createdExperiment.getId();

        // then
        assertThat(experimentRepository.existsById(experimentId)).isTrue();
        assertThat(experimentPartRepository.findAllByExperimentId(experimentId)).hasSize(2);

        var runs = experimentRunRepository.findAllByIdExperimentId(experimentId);
        assertThat(runs).hasSize(2);

        long totalExecutions = runs.stream()
                .mapToLong(run -> getExecutions(experimentId, run.getRunNo()).size())
                .sum();
        assertThat(totalExecutions).isEqualTo(4);
    }

    @Test
    void createExperiment_enqueuesExecutionAfterCommit() {
        // given
        givenValidRegistryMocks();

        // when
        Experiment createdExperiment = experimentService.createExperiment(createRequestTwoParts(2));
        long experimentId = createdExperiment.getId();

        // then
        assertThat(experimentRepository.existsById(experimentId)).isTrue();
        verify(executionService, times(4)).enqueue(any());
    }

    @Test
    void createExperiment_rollsBackWhenReferenceSetMissing() {
        // given
        givenValidRegistryMocks();
        when(problemRegistry.getReferenceSet(anyString())).thenReturn(Optional.empty());

        long experimentsBefore = experimentRepository.count();
        long runsBefore = experimentRunRepository.count();
        long partsBefore = experimentPartRepository.count();
        long executionsBefore = experimentPartExecutionRepository.count();

        // when
        assertThatThrownBy(() -> experimentService.createExperiment(createRequestTwoParts(1)))
                .isInstanceOf(Exception.class);

        // then
        assertThat(experimentRepository.count()).isEqualTo(experimentsBefore);
        assertThat(experimentRunRepository.count()).isEqualTo(runsBefore);
        assertThat(experimentPartRepository.count()).isEqualTo(partsBefore);
        assertThat(experimentPartExecutionRepository.count()).isEqualTo(executionsBefore);
        verify(executionService, never()).enqueue(any());
    }

    @Test
    void createExperiment_rollsBackWhenProblemMissing() {
        // given
        givenValidRegistryMocks();
        when(problemRegistry.getProblem(anyString())).thenReturn(Optional.empty());

        long experimentsBefore = experimentRepository.count();

        // when
        assertThatThrownBy(() -> experimentService.createExperiment(createRequestTwoParts(1)))
                .isInstanceOf(ProblemNotFoundException.class);

        // then
        assertThat(experimentRepository.count()).isEqualTo(experimentsBefore);
        verify(executionService, never()).enqueue(any());
    }

    @Test
    void createExperiment_rollsBackWhenAlgorithmMissing() {
        // given
        givenValidRegistryMocks();
        when(algorithmRegistry.getAlgorithm(anyString(), any(TypedProperties.class), any(Problem.class)))
                .thenReturn(Optional.empty());

        long experimentsBefore = experimentRepository.count();

        // when
        assertThatThrownBy(() -> experimentService.createExperiment(createRequestTwoParts(1)))
                .isInstanceOf(Exception.class);

        // then
        assertThat(experimentRepository.count()).isEqualTo(experimentsBefore);
        verify(executionService, never()).enqueue(any());
    }

    @Test
    void deleteExperiment_removesAllRelatedData() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(2)).getId();

        // when
        boolean deleted = experimentService.deleteExperiment(experimentId);

        // then
        assertThat(deleted).isTrue();
        assertThat(experimentRepository.existsById(experimentId)).isFalse();
        assertThat(experimentRunRepository.findAllByIdExperimentId(experimentId)).isEmpty();
        assertThat(experimentPartRepository.findAllByExperimentId(experimentId)).isEmpty();
        assertThat(experimentPartExecutionRepository.count()).isZero();
    }

    @Test
    void deleteExperiment_returnsFalseWhenExperimentDoesNotExist() {
        // given
        givenValidRegistryMocks();

        // when
        boolean deleted = experimentService.deleteExperiment(1);

        // then
        assertThat(deleted).isFalse();
    }

    @Test
    void deleteExperimentRun_removesSingleRunWhenMultipleExist() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(2)).getId();

        // when
        boolean deleted = experimentService.deleteExperimentRun(experimentId, 1);

        // then
        assertThat(deleted).isTrue();
        assertThat(experimentRepository.existsById(experimentId)).isTrue();
        assertThat(experimentRunRepository.findAllByIdExperimentId(experimentId)).hasSize(1);
    }

    @Test
    void deleteExperimentRun_removesExperimentWhenLastRunDeleted() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();

        // when
        boolean deleted = experimentService.deleteExperimentRun(experimentId, 1);

        // then
        assertThat(deleted).isTrue();
        assertThat(experimentRepository.existsById(experimentId)).isFalse();
    }

    @Test
    void deleteExperimentRun_returnsFalseForInvalidRunNo() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(2)).getId();

        // when
        boolean deleted = experimentService.deleteExperimentRun(experimentId, 999);

        // then
        assertThat(deleted).isFalse();
        assertThat(experimentRepository.existsById(experimentId)).isTrue();
    }

    @Test
    void deleteExperimentRun_returnsFalseWhenExperimentDoesNotExist() {
        // given
        givenValidRegistryMocks();

        // when
        boolean deleted = experimentService.deleteExperimentRun(989, 1);

        // then
        assertThat(deleted).isFalse();
    }

    @Test
    void getExperiment_filtersRunsByStatus() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(3)).getId();

        var runs = experimentRunRepository.findAllByIdExperimentId(experimentId);
        runs.get(0).setStatus(ExperimentRunStatus.SUCCESS);
        runs.get(1).setStatus(ExperimentRunStatus.SUCCESS);
        runs.get(2).setStatus(ExperimentRunStatus.QUEUED);
        experimentRunRepository.saveAllAndFlush(runs);

        // when
        var experimentOpt = experimentService.getExperiment(experimentId, ExperimentRunStatus.SUCCESS);

        // then
        assertThat(experimentOpt).isPresent();
        assertThat(experimentOpt.get().getRuns()).hasSize(2);
        assertThat(experimentOpt.get().getRuns()).allMatch(r -> r.getStatus() == ExperimentRunStatus.SUCCESS);
    }

    @Test
    void getExperimentRun_filtersPartsByStatusAndIndicator() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();

        List<ExperimentPartExecution> partExecutions = getExecutions(experimentId, 1);
        ExperimentPartExecution firstPartExecution = partExecutions.get(0);
        ExperimentPartExecution secondPartExecution = partExecutions.get(1);

        firstPartExecution.setStatus(ExperimentPartStatus.COMPLETED);
        secondPartExecution.setStatus(ExperimentPartStatus.FAILED);
        experimentPartExecutionRepository.saveAllAndFlush(List.of(firstPartExecution, secondPartExecution));

        // when
        var runOpt = experimentService.getExperimentRun(
                experimentId,
                1,
                "NSGAII",
                "ZDT1",
                "HV",
                ExperimentPartStatus.COMPLETED
        );

        // then
        assertThat(runOpt).isPresent();
        assertThat(runOpt.get().getParts()).hasSize(1);
        assertThat(runOpt.get().getParts()).allMatch(p -> p.getStatus() == ExperimentPartStatus.COMPLETED);
    }

    @Test
    void getExperimentRun_returnsRunWithNoPartsWhenNoPartsMatchFilters() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();

        List<ExperimentPartExecution> partExecutions = getExecutions(experimentId, 1);
        partExecutions.forEach(p -> p.setStatus(ExperimentPartStatus.FAILED));
        experimentPartExecutionRepository.saveAllAndFlush(partExecutions);

        // when
        var runOpt = experimentService.getExperimentRun(
                experimentId,
                1L,
                "NSGAII",
                "ZDT1",
                "HV",
                ExperimentPartStatus.COMPLETED
        );

        // then
        assertThat(runOpt).isPresent();
        assertThat(runOpt.get().getParts()).isEmpty();
    }

    @Test
    void getExperimentRun_returnsEmptyWhenRunDoesNotExist() {
        // given
        givenValidRegistryMocks();

        // when
        var runOpt = experimentService.getExperimentRun(
                122,
                1,
                "NSGAII",
                "ZDT1",
                "HV",
                ExperimentPartStatus.COMPLETED
        );

        // then
        assertThat(runOpt).isEmpty();
    }

    @Test
    void getExperimentPartStatus_returnsStatusAndErrorMessage() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();

        ExperimentPartExecution partExecution = getExecutions(experimentId, 1).get(0);
        partExecution.setStatus(ExperimentPartStatus.FAILED);
        partExecution.setErrorMessage("xx");
        experimentPartExecutionRepository.saveAndFlush(partExecution);

        // when
        var statusInfoOpt = experimentService.getExperimentPartStatus(experimentId, 1, partExecution.getId());

        // then
        assertThat(statusInfoOpt).isPresent();
        assertThat(statusInfoOpt.get().status()).isEqualTo(ExperimentPartStatus.FAILED);
        assertThat(statusInfoOpt.get().errorMessage()).isEqualTo("xx");
    }

    @Test
    void getExperimentPartStatus_returnsEmptyForInvalidPartId() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();

        // when
        var statusInfoOpt = experimentService.getExperimentPartStatus(experimentId, 1, 12);

        // then
        assertThat(statusInfoOpt).isEmpty();
    }

    @Test
    void getExperimentAggregate_throwsNotFoundWhenExperimentDoesNotExist() {
        // given
        givenValidRegistryMocks();

        // when
        assertThatThrownBy(() -> experimentService.getExperimentAggregate(987))
                .isInstanceOf(NotFoundException.class);

        // then
        verifyNoInteractions(executionService);
    }

    @Test
    @Transactional
    void getExperimentPartCsv_generatesCsvFromStoredSolutions() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();

        ExperimentPartExecution partExecution = getExecutions(experimentId, 1).get(0);

        ExperimentPartSolution firstSolution = new ExperimentPartSolution(
                Map.of("x1", "0.1", "x2", "0.2"),
                Map.of("f1", 1.0, "f2", 2.0),
                Map.of("c1", 0.0)
        );
        firstSolution.setExperimentPart(partExecution);

        ExperimentPartSolution secondSolution = new ExperimentPartSolution(
                Map.of("x1", "0.3", "x2", "0.4"),
                Map.of("f1", 3.0, "f2", 4.0),
                Map.of("c1", 0.0)
        );
        secondSolution.setExperimentPart(partExecution);

        experimentPartSolutionRepository.saveAllAndFlush(List.of(firstSolution, secondSolution));

        // when
        var csvOpt = experimentService.getExperimentPartCsv(experimentId, 1, partExecution.getId());

        // then
        assertThat(csvOpt).isPresent();
        String csv = csvOpt.get();
        assertThat(csv).contains("Var:", "Obj:", "Const:");
        assertThat(csv.split("\\R").length).isGreaterThanOrEqualTo(3);
    }

    @Test
    @Transactional
    void getExperimentPartCsv_returnsEmptyStringWhenNoSolutions() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1)).getId();
        ExperimentPartExecution partExecution = getExecutions(experimentId, 1).get(0);

        // when
        var csvOpt = experimentService.getExperimentPartCsv(experimentId, 1, partExecution.getId());

        // then
        assertThat(csvOpt).isPresent();
        assertThat(csvOpt.get()).isEqualTo("");
    }

    @Test
    void createExperimentGroup_persistsGroup() {
        // given
        givenValidRegistryMocks();

        // when
        ExperimentGroup createdGroup = experimentService.createExperimentGroup("a");

        // then
        assertThat(createdGroup.getId()).isNotNull();
        assertThat(experimentGroupRepository.existsById(createdGroup.getId())).isTrue();
        assertThat(createdGroup.getName()).isEqualTo("a");
        assertThat(createdGroup.getRuns()).isEmpty();
        assertThat(createdGroup.getProblems()).isEmpty();
        assertThat(createdGroup.getAlgorithms()).isEmpty();
    }

    @Test
    void getExperimentGroups_returnsAllGroups() {
        // given
        givenValidRegistryMocks();
        experimentService.createExperimentGroup("a");
        experimentService.createExperimentGroup("b");

        // when
        List<ExperimentGroup> groups = experimentService.getExperimentGroups();

        // then
        assertThat(groups).hasSize(2);
        assertThat(groups).extracting(ExperimentGroup::getName).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void addRunToExperimentGroup_addsRunAndLocksCompatibility() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1, "ZDT1", "NSGAII")).getId();
        ExperimentGroup group = experimentService.createExperimentGroup("a");

        // when
        ExperimentGroup updatedGroup = experimentService.addRunToExperimentGroup(group.getId(), experimentId, 1L);

        // then
        assertThat(updatedGroup.getRuns()).hasSize(1);
        assertThat(updatedGroup.getProblems()).containsExactlyInAnyOrder("ZDT1");
        assertThat(updatedGroup.getAlgorithms()).containsExactlyInAnyOrder("NSGAII");
    }


    @Test
    void deleteRunFromExperimentGroup_removesRunAndClearsCompatibilityWhenEmpty() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1, "ZDT1", "NSGAII")).getId();
        ExperimentGroup group = experimentService.createExperimentGroup("a");
        experimentService.addRunToExperimentGroup(group.getId(), experimentId, 1L);

        // when
        ExperimentGroup updatedGroup = experimentService.deleteRunFromExperimentGroup(group.getId(), experimentId, 1L);

        // then
        assertThat(updatedGroup.getRuns()).isEmpty();
        assertThat(updatedGroup.getProblems()).isEmpty();
        assertThat(updatedGroup.getAlgorithms()).isEmpty();
    }

    @Test
    @Transactional
    void getExperimentGroup_returnsGroupWithRuns() {
        // given
        givenValidRegistryMocks();
        long experimentId = experimentService.createExperiment(createRequestTwoParts(1, "ZDT1", "NSGAII")).getId();
        ExperimentGroup group = experimentService.createExperimentGroup("a");
        experimentService.addRunToExperimentGroup(group.getId(), experimentId, 1L);

        // when
        Optional<ExperimentGroup> groupOpt = experimentService.getExperimentGroup(group.getId());

        // then
        assertThat(groupOpt).isPresent();
        assertThat(groupOpt.get().getRuns()).hasSize(1);
    }

    @Test
    void deleteExperimentGroup_returnsTrueWhenDeletedAndFalseWhenMissing() {
        // given
        givenValidRegistryMocks();
        ExperimentGroup group = experimentService.createExperimentGroup("a");

        // when
        boolean deleted = experimentService.deleteExperimentGroup(group.getId());
        boolean deletedAgain = experimentService.deleteExperimentGroup(group.getId());

        // then
        assertThat(deleted).isTrue();
        assertThat(deletedAgain).isFalse();
    }

}
