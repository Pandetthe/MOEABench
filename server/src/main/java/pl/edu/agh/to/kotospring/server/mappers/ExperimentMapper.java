package pl.edu.agh.to.kotospring.server.mappers;

import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.server.entities.*;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
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
                        experiment.getRunCount()
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        GetExperimentsResponse::new
                ));
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
                mappedRuns
        );
    }

    private GetExperimentResponseData mapToGetExperimentResponseData(ExperimentRun run) {
        return new GetExperimentResponseData(
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
                mappedParts
        );
    }

    private GetExperimentRunResponseData mapToGetExperimentRunResponse(ExperimentPart part) {
        return new GetExperimentRunResponseData(
                part.getId(),
                part.getAlgorithm(),
                part.getProblem(),
                part.getBudget(),
                part.getStatus(),
                part.getErrorMessage(),
                part.getStartedAt(),
                part.getFinishedAt()
        );
    }

    public GetExperimentPartResponse mapToGetExperimentPartResponse(ExperimentPart part) {
        return new GetExperimentPartResponse(
                part.getAlgorithm(),
                part.getParameters().stream().collect(Collectors.toMap(
                        ExperimentPartAlgorithmParameter::getKey,
                        ExperimentPartAlgorithmParameter::getValue,
                        (existing, replacement) -> replacement,
                        HashMap::new)),
                part.getProblem(),
                new HashSet<>(part.getIndicators().stream().map(ExperimentPartIndicator::getName).toList()),
                part.getBudget(),
                part.getStatus(),
                part.getErrorMessage(),
                part.getStartedAt(),
                part.getFinishedAt()
        );
    }

    public GetExperimentStatusResponse mapToGetExperimentStatusResponse(Experiment experiment) {
        return new GetExperimentStatusResponse(experiment.getStatus());
    }

    public GetExperimentRunStatusResponse mapToGetExperimentRunStatusResponse(ExperimentRun experimentRun) {
        return new GetExperimentRunStatusResponse(experimentRun.getStatus());
    }

    public GetExperimentPartStatusResponse mapToGetExperimentPartStatusResponse(ExperimentPart part) {
        return new GetExperimentPartStatusResponse(
                part.getStatus(),
                part.getErrorMessage()
        );
    }

    public GetExperimentResultResponse mapToResultResponse(ExperimentRun experimentRun) {
        return experimentRun.getParts().stream()
                .filter(part -> part.getStatus() == ExperimentPartStatus.COMPLETED)
                .map(this::mapToResultResponseData)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        GetExperimentResultResponse::new
                ));
    }

    private GetExperimentResultResponseData mapToResultResponseData(ExperimentPart part) {
        return new GetExperimentResultResponseData(
                part.getId(),
                part.getSolutions().stream()
                        .map(solution -> new AlgorithmResult(
                                solution.getVariables(),
                                solution.getObjectives(),
                                solution.getConstraints()
                        ))
                        .collect(Collectors.toList()),
                part.getIndicators().stream()
                        .collect(Collectors.toMap(
                                ExperimentPartIndicator::getName,
                                ExperimentPartIndicator::getValue
                        ))
        );
    }

    public GetExperimentPartResultResponse mapToPartResultResponse(ExperimentPart part) {
        List<AlgorithmResult> results = part.getSolutions().stream()
                .map(solution -> new AlgorithmResult(
                        solution.getVariables(),
                        solution.getObjectives(),
                        solution.getConstraints()
                ))
                .collect(Collectors.toList());

        var indicatorsValues = part.getIndicators().stream()
                .collect(Collectors.toMap(
                        ExperimentPartIndicator::getName,
                        ExperimentPartIndicator::getValue
                ));

        return new GetExperimentPartResultResponse(results, indicatorsValues);
    }
}