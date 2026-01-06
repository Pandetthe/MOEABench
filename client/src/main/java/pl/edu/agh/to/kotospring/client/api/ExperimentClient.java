package pl.edu.agh.to.kotospring.client.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.time.OffsetDateTime;

public interface ExperimentClient {

    @GetExchange("/experiments")
    GetExperimentsResponse getExperiments(
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String problem,
            @RequestParam(required = false) String indicator,
            @RequestParam(required = false) ExperimentStatus status,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime);

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
    CreateExperimentResponse createExperiment(@RequestBody CreateExperimentRequest request);

    @DeleteExchange("/experiments/{id}")
    void deleteExperiment(@PathVariable("id") long id);

    @DeleteExchange("/experiments/{id}/{runNo}")
    void deleteExperimentRun(@PathVariable("id") long id, @PathVariable("runNo") long runNo);
}
