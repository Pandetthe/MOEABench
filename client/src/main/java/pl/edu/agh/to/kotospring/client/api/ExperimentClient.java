package pl.edu.agh.to.kotospring.client.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
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

        @GetExchange("/experiments/{id}")
        GetExperimentResponse getExperiment(
                        @PathVariable long id,
                        @RequestParam(required = false) ExperimentRunStatus runStatus);

        @GetExchange("/experiments/{id}/runs/{runNo}")
        GetExperimentRunResponse getExperimentRun(
                        @PathVariable long id,
                        @PathVariable long runNo,
                        @RequestParam(required = false) String algorithm,
                        @RequestParam(required = false) String problem,
                        @RequestParam(required = false) String indicator,
                        @RequestParam(required = false) ExperimentPartStatus status);

        @GetExchange("/experiments/{id}/runs/{runNo}/parts/{partId}")
        GetExperimentPartResponse getExperimentPart(
                        @PathVariable long id,
                        @PathVariable long runNo,
                        @PathVariable long partId);

        @GetExchange("/experiments/{id}/status/")
        GetExperimentStatusResponse getExperimentStatus(@PathVariable("id") long id);

        @GetExchange("/experiments/{id}/runs/{runNo}/status")
        GetExperimentRunStatusResponse getExperimentRunStatus(
                        @PathVariable("id") long id,
                        @PathVariable("runNo") long runNo);

        @GetExchange("/experiments/{id}/runs/{runNo}/parts/{partId}/status")
        GetExperimentPartStatusResponse getExperimentPartStatus(
                        @PathVariable("id") long id,
                        @PathVariable("runNo") long runNo,
                        @PathVariable("partId") long partId);

        @GetExchange("/experiments/{id}/result")
        GetExperimentResultResponse getExperimentResult(@PathVariable("id") long id);

        @GetExchange("/experiments/{id}/runs/{runNo}/result")
        GetExperimentRunResultResponse getExperimentRunResult(
                        @PathVariable("id") long id,
                        @PathVariable("runNo") long runNo);

        @GetExchange("/experiments/{id}/runs/{runNo}/parts/{partId}/result")
        GetExperimentPartResultResponse getExperimentPartResult(
                        @PathVariable("id") long id,
                        @PathVariable("runNo") long runNo,
                        @PathVariable("partId") long partId);

        @GetExchange("/experiments/{id}/aggregate")
        GetExperimentAggregateResponse getExperimentAggregate(@PathVariable("id") long id);

        @PostExchange("/experiments")
        CreateExperimentResponse createExperiment(@RequestBody CreateExperimentRequest request);

        @DeleteExchange("/experiments/{id}")
        void deleteExperiment(@PathVariable("id") long id);

        @DeleteExchange("/experiments/{id}/runs/{runNo}")
        void deleteExperimentRun(@PathVariable("id") long id, @PathVariable("runNo") long runNo);
}
