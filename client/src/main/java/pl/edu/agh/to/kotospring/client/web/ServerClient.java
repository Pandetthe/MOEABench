package pl.edu.agh.to.kotospring.client.web;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.edu.agh.to.kotospring.shared.algorithms.contracts.GetAlgorithmsResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentResponse;
import pl.edu.agh.to.kotospring.shared.problems.contracts.GetProblemsResponse;

public interface ServerClient {
    @GetExchange("/algorithms")
    GetAlgorithmsResponse getAlgorithms();

    @GetExchange("/problems")
    GetProblemsResponse getProblems();

    @PostExchange("/experiments")
    CreateExperimentResponse createExperiment(CreateExperimentRequest request);
}
