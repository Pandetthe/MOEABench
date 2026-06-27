package pl.edu.agh.to.kotospring.server.mappers;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.models.ExperimentRunView;
import pl.edu.agh.to.kotospring.server.models.PartStatusInfo;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ExperimentMapper {
    public CreateExperimentResponse mapToCreateResponse(Experiment experiment) {
        return new CreateExperimentResponse(experiment.getId());
    }

    public GetExperimentsResponse mapToGetExperimentsResponse(List<Experiment> experiments) {
        return new GetExperimentsResponse(experiments.stream()
                .map(experiment -> new GetExperimentsResponseData(
                        experiment.getId(),
                        experiment.getStatus(),
                        experiment.getQueuedAt(),
                        experiment.getStartedAt(),
                        experiment.getFinishedAt(),
                        experiment.getRunCount()))
                .toList());
    }

    public GetExperimentResponse mapToGetExperimentResponse(Experiment experiment) {
        List<GetExperimentResponseData> mappedRuns = experiment.getRuns().stream()
                .map(this::mapToGetExperimentResponseData)
                .toList();

        return new GetExperimentResponse(
                experiment.getStatus(),
                experiment.getQueuedAt(),
                experiment.getStartedAt(),
                experiment.getFinishedAt(),
                experiment.getRunCount(),
                mappedRuns);
    }

    private GetExperimentResponseData mapToGetExperimentResponseData(ExperimentRun run) {
        return new GetExperimentResponseData(
                run.getRunNo(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt());
    }

    public GetExperimentRunsResponse mapToGetExperimentRunsResponse(Page<ExperimentRun> runs) {
        List<GetExperimentRunsResponseData> content = runs.getContent().stream()
                .map(this::mapToGetExperimentRunsResponseData)
                .toList();

        PageMetadata metadata = new PageMetadata(
                runs.getSize(),
                runs.getTotalElements(),
                runs.getTotalPages(),
                runs.getNumber());

        return new GetExperimentRunsResponse(content, metadata);
    }

    private GetExperimentRunsResponseData mapToGetExperimentRunsResponseData(ExperimentRun run) {
        return new GetExperimentRunsResponseData(
                run.getExperimentId(),
                run.getRunNo(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt());
    }

    public GetExperimentRunResponse mapToGetExperimentRunResponse(ExperimentRunView runView) {
        List<GetExperimentRunResponseData> mappedParts = runView.parts().stream()
                .map(this::mapToGetExperimentRunResponseData)
                .toList();

        return new GetExperimentRunResponse(
                runView.status(),
                runView.startedAt(),
                runView.finishedAt(),
                mappedParts);
    }

    private GetExperimentRunResponseData mapToGetExperimentRunResponseData(ExperimentPartExecution part) {
        return new GetExperimentRunResponseData(
                part.getId(),
                part.getExperimentPart().getAlgorithm(),
                part.getExperimentPart().getProblem(),
                part.getExperimentPart().getBudget(),
                part.getStatus(),
                part.getErrorMessage(),
                part.getStartedAt(),
                part.getFinishedAt());
    }

    public GetExperimentPartResponse mapToGetExperimentPartResponse(ExperimentPartExecution part) {
        return new GetExperimentPartResponse(
                part.getExperimentPart().getAlgorithm(),
                part.getExperimentPart().getParameters().stream().collect(Collectors.toMap(
                        ExperimentPartAlgorithmParameter::getKey,
                        ExperimentPartAlgorithmParameter::getValue,
                        (existing, replacement) -> replacement,
                        HashMap::new)),
                part.getExperimentPart().getProblem(),
                part.getIndicators().stream().map(ExperimentPartIndicator::getName).collect(Collectors.toSet()),
                part.getExperimentPart().getBudget(),
                part.getStatus(),
                part.getErrorMessage(),
                part.getStartedAt(),
                part.getFinishedAt());
    }

    public GetExperimentStatusResponse mapToGetExperimentStatusResponse(ExperimentStatus experiment) {
        return new GetExperimentStatusResponse(experiment);
    }

    public GetExperimentRunStatusResponse mapToGetExperimentRunStatusResponse(ExperimentRunStatus experimentRun) {
        return new GetExperimentRunStatusResponse(experimentRun);
    }

    public GetExperimentPartStatusResponse mapToGetExperimentPartStatusResponse(PartStatusInfo part) {
        return new GetExperimentPartStatusResponse(part.status(), part.errorMessage());
    }

    public GetExperimentResultResponse mapToExperimentResultResponse(Experiment experiment) {
        return new GetExperimentResultResponse(experiment.getRuns().stream()
                .map(this::mapToExperimentResultResponseData)
                .toList());
    }

    private GetExperimentResultResponseData mapToExperimentResultResponseData(ExperimentRun run) {
        return new GetExperimentResultResponseData(
                run.getRunNo(),
                run.getParts().stream()
                        .map(this::mapToExperimentResultResponseDataPart)
                        .toList());
    }

    private GetExperimentResultResponseDataPart mapToExperimentResultResponseDataPart(ExperimentPartExecution part) {
        return new GetExperimentResultResponseDataPart(
                part.getId(),
                part.getSolutions().stream()
                        .map(solution -> new AlgorithmResult(
                                solution.getVariables(),
                                solution.getObjectives(),
                                solution.getConstraints()))
                        .toList(),
                part.getIndicators().stream()
                        .collect(Collectors.toMap(
                                ExperimentPartIndicator::getName,
                                ExperimentPartIndicator::getValue)));
    }

    public GetExperimentRunResultResponse mapToExperimentRunResultResponse(ExperimentRun experimentRun) {
        return new GetExperimentRunResultResponse(experimentRun.getParts().stream()
                .map(this::mapToExperimentRunResultResponseData)
                .toList());
    }

    private GetExperimentRunResultResponseData mapToExperimentRunResultResponseData(ExperimentPartExecution part) {
        return new GetExperimentRunResultResponseData(
                part.getId(),
                part.getSolutions().stream()
                        .map(solution -> new AlgorithmResult(
                                solution.getVariables(),
                                solution.getObjectives(),
                                solution.getConstraints()))
                        .toList(),
                part.getIndicators().stream()
                        .collect(Collectors.toMap(
                                ExperimentPartIndicator::getName,
                                ExperimentPartIndicator::getValue)));
    }

    public GetExperimentPartResultResponse mapToExperimentPartResultResponse(ExperimentPartExecution part) {
        List<AlgorithmResult> results = part.getSolutions().stream()
                .map(solution -> new AlgorithmResult(
                        solution.getVariables(),
                        solution.getObjectives(),
                        solution.getConstraints()))
                .toList();

        Map<String, Double> indicatorsValues = part.getIndicators().stream()
                .collect(Collectors.toMap(
                        ExperimentPartIndicator::getName,
                        ExperimentPartIndicator::getValue));

        return new GetExperimentPartResultResponse(results, indicatorsValues);
    }

    public GetExperimentGroupResponse mapToGroupResponse(ExperimentGroup group) {
        Set<ExperimentGroupRunResponse> runResponses = group.getRuns().stream()
                .map(run -> new ExperimentGroupRunResponse(run.getExperimentId(), run.getRunNo()))
                .collect(Collectors.toSet());

        return new GetExperimentGroupResponse(
                group.getId(),
                group.getName(),
                group.getProblems(),
                group.getAlgorithms(),
                runResponses);
    }

    public GetExperimentGroupsResponse mapToGroupsResponse(List<ExperimentGroup> groups) {
        return new GetExperimentGroupsResponse(groups.stream()
                .map(g -> new GetExperimentGroupsResponseData(g.getId(), g.getName()))
                .toList());
    }
}
