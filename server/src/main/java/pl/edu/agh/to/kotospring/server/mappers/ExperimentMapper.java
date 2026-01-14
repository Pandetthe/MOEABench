package pl.edu.agh.to.kotospring.server.mappers;

import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.server.models.PartStatusInfo;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExperimentMapper {
        public CreateExperimentResponse mapToCreateResponse(Experiment experiment) {
                return new CreateExperimentResponse(experiment.getId());
        }

        public GetExperimentsResponse mapToGetExperimentsResponse(List<Experiment> experiments) {
                return experiments.stream()
                                .map(experiment -> new GetExperimentsResponseData(
                                                experiment.getId(),
                                                experiment.getStatus(),
                                                experiment.getQueuedAt(),
                                                experiment.getStartedAt(),
                                                experiment.getFinishedAt(),
                                                experiment.getRunCount()))
                                .collect(Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                GetExperimentsResponse::new));
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

    public GetExperimentRunsResponse mapToGetExperimentRunsResponse(List<ExperimentRun> runs) {
        return runs.stream()
                .map(this::mapToGetExperimentRunsResponseData)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        GetExperimentRunsResponse::new));
    }

    private GetExperimentRunsResponseData mapToGetExperimentRunsResponseData(ExperimentRun run) {
        return new GetExperimentRunsResponseData(
                run.getExperimentId(),
                run.getRunNo(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt()
        );
    }

        public GetExperimentRunResponse mapToGetExperimentRunResponse(ExperimentRun run) {
                List<GetExperimentRunResponseData> mappedParts = run.getParts().stream()
                                .map(this::mapToGetExperimentRunResponse)
                                .toList();

                return new GetExperimentRunResponse(
                                run.getStatus(),
                                run.getStartedAt(),
                                run.getFinishedAt(),
                                mappedParts);
        }

        private GetExperimentRunResponseData mapToGetExperimentRunResponse(ExperimentPartExecution part) {
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
                                new HashSet<>(part.getIndicators().stream().map(ExperimentPartIndicator::getName)
                                                .toList()),
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
                return new GetExperimentPartStatusResponse(
                                part.status(),
                                part.errorMessage());
        }

        public GetExperimentResultResponse mapToExperimentResultResponse(Experiment experiment) {
                return experiment.getRuns().stream()
                                .map(this::mapToExperimentResultResponseData)
                                .collect(Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                GetExperimentResultResponse::new));
        }

        private GetExperimentResultResponseData mapToExperimentResultResponseData(ExperimentRun run) {
                return new GetExperimentResultResponseData(
                                run.getRunNo(),
                                run.getParts().stream()
                                                .map(this::mapToExperimentResultResponseDataPart)
                                                .toList());
        }

        private GetExperimentResultResponseDataPart mapToExperimentResultResponseDataPart(
                        ExperimentPartExecution part) {
                return new GetExperimentResultResponseDataPart(
                                part.getId(),
                                part.getSolutions().stream()
                                                .map(solution -> new AlgorithmResult(
                                                                solution.getVariables(),
                                                                solution.getObjectives(),
                                                                solution.getConstraints()))
                                                .collect(Collectors.toList()),
                                part.getIndicators().stream()
                                                .collect(Collectors.toMap(
                                                                ExperimentPartIndicator::getName,
                                                                ExperimentPartIndicator::getValue)));
        }

        public GetExperimentRunResultResponse mapToExperimentRunResultResponse(ExperimentRun experimentRun) {
                return experimentRun.getParts().stream()
                                .map(this::mapToExperimentRunResultResponseData)
                                .collect(Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                GetExperimentRunResultResponse::new));
        }

        private GetExperimentRunResultResponseData mapToExperimentRunResultResponseData(ExperimentPartExecution part) {
                return new GetExperimentRunResultResponseData(
                                part.getId(),
                                part.getSolutions().stream()
                                                .map(solution -> new AlgorithmResult(
                                                                solution.getVariables(),
                                                                solution.getObjectives(),
                                                                solution.getConstraints()))
                                                .collect(Collectors.toList()),
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
                                .collect(Collectors.toList());

                var indicatorsValues = part.getIndicators().stream()
                                .collect(Collectors.toMap(
                                                ExperimentPartIndicator::getName,
                                                ExperimentPartIndicator::getValue));

                return new GetExperimentPartResultResponse(results, indicatorsValues);
        }
}