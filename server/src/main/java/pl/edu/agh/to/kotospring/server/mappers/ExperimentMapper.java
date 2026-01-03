package pl.edu.agh.to.kotospring.server.mappers;

import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartAlgorithmParameter;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartIndicator;
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
                        experiment.getFinishedAt()
                ))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        GetExperimentsResponse::new
                ));
    }

    public GetExperimentResponse mapToGetExperimentResponse(Experiment experiment) {
        return new GetExperimentResponse(
                experiment.getStatus(),
                experiment.getQueuedAt(),
                experiment.getStartedAt(),
                experiment.getFinishedAt(),
                experiment.getParts().stream().map(this::mapToPartData).toList()
        );
    }

    private GetExperimentResponseData mapToPartData(ExperimentPart part) {
        return new GetExperimentResponseData(
                part.getId(),
                part.getStatus(),
                part.getErrorMessage(),
                part.getProblem(),
                part.getAlgorithm(),
                part.getBudget(),
                part.getParameters().stream().collect(Collectors.toMap(
                        ExperimentPartAlgorithmParameter::getKey,
                        ExperimentPartAlgorithmParameter::getValue,
                        (existing, replacement) -> replacement,
                        HashMap::new)),
                new HashSet<>(part.getIndicators().stream().map(ExperimentPartIndicator::getName).toList()),
                part.getStartedAt(),
                part.getFinishedAt()
        );
    }

    public GetExperimentStatusResponse mapToStatusResponse(Experiment experiment) {
        return new GetExperimentStatusResponse(experiment.getStatus());
    }

    public GetExperimentPartStatusResponse mapToPartStatusResponse(ExperimentPart part) {
        return new GetExperimentPartStatusResponse(
                part.getStatus(),
                part.getErrorMessage()
        );
    }

    public GetExperimentResultResponse mapToResultResponse(Experiment experiment) {
        return experiment.getParts().stream()
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