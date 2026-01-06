package pl.edu.agh.to.kotospring.client.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

public interface ExperimentClient {

    @GetExchange("/experiments")
    GetExperimentsResponse getExperiments();

    @GetExchange("/experiments/{id}/status/")
    GetExperimentStatusResponse getExperimentStatus(@PathVariable("id") long id);

    @GetExchange("/experiments/{id}/status/{partId}")
    GetExperimentPartStatusResponse getExperimentPartStatus(
            @PathVariable("id") long id,
            @PathVariable("partId") long partId
    );

    @GetExchange("/experiments/{id}")
    GetExperimentResponse getExperiment(@PathVariable("id") long id);

    @GetExchange("/experiments/{id}/result")
    GetExperimentResultResponse getExperimentResult(@PathVariable("id") long id);

    @GetExchange("/experiments/{id}/result/{partId}")
    GetExperimentPartResultResponse getExperimentPartResult(
            @PathVariable("id") long id,
            @PathVariable("partId") long partId
    );

    @PostExchange("/experiments")
    CreateExperimentResponse createExperiment(@RequestBody CreateExperimentRequest request, @RequestParam("runsNo") int runsNo);

    @DeleteExchange("/experiments/{id}")
    void deleteFullExperiment(@PathVariable("id") long id);
}
