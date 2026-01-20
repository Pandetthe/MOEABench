package pl.edu.agh.to.kotospring.client.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        @GetExchange("/experiments/runs")
        GetExperimentRunsResponse getExperimentRuns(
                        @RequestParam(required = false) String algorithm,
                        @RequestParam(required = false) String problem,
                        @RequestParam(required = false) String indicator,
                        @RequestParam(required = false) ExperimentRunStatus status,
                        @RequestParam(required = false) OffsetDateTime startTime,
                        @RequestParam(required = false) OffsetDateTime endTime,
                        @RequestParam int page,
                        @RequestParam int size);

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

        @GetExchange("/experiments/{id}/runs/{runNo}/parts/{partId}/csv")
        String getExperimentPartCsv(
                        @PathVariable("id") long id,
                        @PathVariable("runNo") long runNo,
                        @PathVariable("partId") long partId);

        @GetExchange("/experiments/{id}/runs/{runNo}/parts/{partId}/plot")
        Optional<byte[]> getExperimentPartPlot(
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

        @PostExchange("/experiments/groups")
        CreateExperimentGroupRequest createExperimentGroup(@RequestBody CreateExperimentGroupRequest request);


        @GetExchange("/experiments/groups")
        GetExperimentGroupsResponse getExperimentGroups();

        @PostExchange("/experiments/{id}/runs/{runNo}/groups/{groupId}")
        ResponseEntity<?> addRunToExperimentGroup(
                @PathVariable Long groupId,
                @PathVariable Long id,
                @PathVariable Long runNo);

        @DeleteExchange("/experiments/{id}/runs/{runNo}/groups/{groupId}")
        void deleteRunFromExperimentGroup(
                @PathVariable Long groupId,
                @PathVariable Long id,
                @PathVariable Long runNo);

        @GetExchange("/experiments/groups/{id}")
        ResponseEntity<?> getExperimentGroup(@PathVariable Long id);

        @DeleteExchange("/experiments/groups/{id}")
        void deleteExperimentGroup(@PathVariable Long id);
    }

