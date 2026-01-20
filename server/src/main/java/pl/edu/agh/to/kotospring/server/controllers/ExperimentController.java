package pl.edu.agh.to.kotospring.server.controllers;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import pl.edu.agh.to.kotospring.server.exceptions.NotAllPartsFinishedException;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.mappers.ExperimentMapper;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;
import io.swagger.v3.oas.annotations.Operation;
import pl.edu.agh.to.kotospring.server.entities.ExperimentGroup;
import java.util.stream.Collectors;

import java.time.OffsetDateTime;

@RestController
@RequestMapping(path = "/experiments")
public final class ExperimentController {
    private final ExperimentService experimentService;
    private final ExperimentMapper experimentMapper;

    public ExperimentController(ExperimentService experimentService, ExperimentMapper experimentMapper) {
        this.experimentService = experimentService;
        this.experimentMapper = experimentMapper;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateExperimentRequest body) {
        var experiment = experimentService.createExperiment(body);
        return ResponseEntity.ok(experimentMapper.mapToCreateResponse(experiment));
    }

    @GetMapping
    public ResponseEntity<?> getExperiments(
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String problem,
            @RequestParam(required = false) String indicator,
            @RequestParam(required = false) ExperimentStatus status,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {

        var experiments = experimentService.getExperiments(algorithm, problem, indicator, status, startTime, endTime);
        return ResponseEntity.ok(experimentMapper.mapToGetExperimentsResponse(experiments));
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getExperiment(
            @PathVariable long id,
            @RequestParam(required = false) ExperimentRunStatus runStatus) {
        return experimentService.getExperiment(id, runStatus)
                .map(experimentMapper::mapToGetExperimentResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @Operation(summary = "Get experiment aggregations")
    @GetMapping("{id}/aggregate")
    public ResponseEntity<GetExperimentAggregateResponse> getExperimentAggregate(
            @PathVariable long id) {
        return ResponseEntity.ok(experimentService.getExperimentAggregate(id));
    }

    @GetMapping("runs")
    public ResponseEntity<GetExperimentRunsResponse> getExperimentRuns(
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String problem,
            @RequestParam(required = false) String indicator,
            @RequestParam(required = false) ExperimentRunStatus status,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id.runNo"));
        var runs = experimentService.getExperimentRuns(algorithm, problem, indicator, status, startTime, endTime,
                pageable);
        return ResponseEntity.ok(experimentMapper.mapToGetExperimentRunsResponse(runs));
    }

    @GetMapping("{id}/runs/{runNo}")
    public ResponseEntity<?> getExperimentRun(
            @PathVariable long id,
            @PathVariable long runNo,
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String problem,
            @RequestParam(required = false) String indicator,
            @RequestParam(required = false) ExperimentPartStatus status) {

        return experimentService.getExperimentRun(id, runNo, algorithm, problem, indicator, status)
                .map(experimentMapper::mapToGetExperimentRunResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment run not found"));
    }

    @GetMapping("{id}/runs/{runNo}/parts/{partId}")
    public ResponseEntity<?> getExperimentPart(@PathVariable long id, @PathVariable long runNo,
            @PathVariable long partId) {
        return experimentService.getExperimentPart(id, runNo, partId)
                .map(experimentMapper::mapToGetExperimentPartResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @GetMapping(value = "{id}/runs/{runNo}/parts/{partId}/plot", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getExperimentPartPlot(
            @PathVariable long id,
            @PathVariable long runNo,
            @PathVariable long partId) {
        return experimentService.getExperimentPart(id, runNo, partId)
                .map(part -> {
                    byte[] plotImage = part.getPlotImage();
                    if (plotImage == null) {
                        throw new NotFoundException("Plot image not found for this part");
                    }
                    return ResponseEntity.ok(plotImage);
                })
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @GetMapping("{id}/status")
    public ResponseEntity<?> getExperimentStatus(@PathVariable long id) {
        return experimentService.getExperimentStatus(id)
                .map(experimentMapper::mapToGetExperimentStatusResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id}/runs/{runNo}/status")
    public ResponseEntity<?> getExperimentRunStatus(@PathVariable long id, @PathVariable long runNo) {
        return experimentService.getExperimentRunStatus(id, runNo)
                .map(experimentMapper::mapToGetExperimentRunStatusResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment run not found"));
    }

    @GetMapping("{id}/runs/{runNo}/parts/{partId}/status")
    public ResponseEntity<?> getExperimentPartStatus(@PathVariable long id, @PathVariable long runNo,
            @PathVariable long partId) {
        return experimentService.getExperimentPartStatus(id, runNo, partId)
                .map(experimentMapper::mapToGetExperimentPartStatusResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @GetMapping("{id}/result")
    public ResponseEntity<?> getExperimentResult(@PathVariable long id) {
        return experimentService.getExperimentResult(id)
                .map(exp -> {
                    if (exp.getStatus() == ExperimentStatus.QUEUED ||
                            exp.getStatus() == ExperimentStatus.IN_PROGRESS) {
                        throw new NotAllPartsFinishedException();
                    }
                    return exp;
                })
                .map(experimentMapper::mapToExperimentResultResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id}/runs/{runNo}/result")
    public ResponseEntity<?> getExperimentRunResult(
            @PathVariable long id,
            @PathVariable long runNo) {

        return experimentService.getExperimentRunResult(id, runNo)
                .map(run -> {
                    if (run.getStatus() == ExperimentRunStatus.QUEUED ||
                            run.getStatus() == ExperimentRunStatus.IN_PROGRESS) {
                        throw new NotAllPartsFinishedException();
                    }
                    return run;
                })
                .map(experimentMapper::mapToExperimentRunResultResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment " + id + " or Run " + runNo + " not found"));
    }

    @GetMapping("{id}/runs/{runNo}/parts/{partId}/result")
    public ResponseEntity<?> getExperimentPartResult(@PathVariable long id, @PathVariable long runNo,
            @PathVariable long partId) {
        return experimentService.getExperimentPartResult(id, runNo, partId)
                .map(part -> {
                    if (part.getStatus() == ExperimentPartStatus.QUEUED ||
                            part.getStatus() == ExperimentPartStatus.RUNNING) {
                        throw new NotAllPartsFinishedException();
                    }
                    return part;
                })
                .map(experimentMapper::mapToExperimentPartResultResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping(value = "{id}/runs/{runNo}/parts/{partId}/csv", produces = "text/csv")
    public ResponseEntity<String> getExperimentPartCsv(
            @PathVariable long id,
            @PathVariable long runNo,
            @PathVariable long partId) {
        return experimentService.getExperimentPartCsv(id, runNo, partId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteExperiment(@PathVariable long id) {
        if (experimentService.deleteExperiment(id))
            return ResponseEntity.noContent().build();
        throw new NotFoundException("Experiment not found");
    }

    @DeleteMapping("{id}/runs/{runNo}")
    public ResponseEntity<?> deleteExperimentRun(@PathVariable long id, @PathVariable long runNo) {
        if (experimentService.deleteExperimentRun(id, runNo))
            return ResponseEntity.noContent().build();
        throw new NotFoundException("Experiment run not found");
    }

    @PostMapping("groups")
    public ResponseEntity<?> createExperimentGroup(@RequestBody CreateExperimentGroupRequest request) {
        ExperimentGroup group = experimentService.createExperimentGroup(request.name());
        return ResponseEntity.ok(experimentMapper.mapToGroupResponse(group));
    }

    @GetMapping("groups")
    public ResponseEntity<?> getExperimentGroups() {
        List<ExperimentGroup> groups = experimentService.getExperimentGroups();
        return ResponseEntity.ok(groups.stream()
                .map(experimentMapper::mapToGroupResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping("{id}/runs/{runNo}/groups/{groupId}")
    public ResponseEntity<?> addRunToExperimentGroup(
            @PathVariable Long groupId,
            @PathVariable Long id,
            @PathVariable Long runNo) {
        ExperimentGroup group = experimentService.addRunToExperimentGroup(groupId, id, runNo);
        return ResponseEntity.ok(experimentMapper.mapToGroupResponse(group));
    }

    @DeleteMapping("{id}/runs/{runNo}/groups/{groupId}")
    public ResponseEntity<?> deleteRunFromExperimentGroup(
            @PathVariable Long groupId,
            @PathVariable Long id,
            @PathVariable Long runNo) {
        ExperimentGroup group = experimentService.deleteRunFromExperimentGroup(groupId, id, runNo);
        return ResponseEntity.ok(experimentMapper.mapToGroupResponse(group));
    }

    @GetMapping("groups/{id}")
    public ResponseEntity<?> getExperimentGroup(@PathVariable Long id) {
        return experimentService.getExperimentGroup(id)
                .map(experimentMapper::mapToGroupResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment group not found"));
    }

    @DeleteMapping("groups/{id}")
    public ResponseEntity<?> deleteExperimentGroup(@PathVariable Long id) {
        if (experimentService.deleteExperimentGroup(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new NotFoundException("Experiment group not found");
    }

}