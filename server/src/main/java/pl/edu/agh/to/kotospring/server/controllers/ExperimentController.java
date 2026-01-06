package pl.edu.agh.to.kotospring.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.mappers.ExperimentMapper;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

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
    public ResponseEntity<?> getExperiments() {
        var experiments = experimentService.getExperiments();
        return ResponseEntity.ok(experimentMapper.mapToGetExperimentsResponse(experiments));
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getExperiment(@PathVariable long id) {
        return experimentService.getExperiment(id)
                .map(experimentMapper::mapToGetExperimentResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id}/{runNo}")
    public ResponseEntity<?> getExperimentRun(@PathVariable long id, @PathVariable long runNo) {
        return experimentService.getExperimentRun(id, runNo)
                .map(experimentMapper::mapToGetExperimentRunResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment run not found"));
    }

    @GetMapping("{id}/{runNo}/{partId}")
    public ResponseEntity<?> getExperimentPart(@PathVariable long id, @PathVariable long runNo, @PathVariable long partId) {
        return experimentService.getExperimentPart(id, runNo, partId)
                .map(experimentMapper::mapToGetExperimentPartResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @GetMapping("{id}/status")
    public ResponseEntity<?> getExperimentStatus(@PathVariable long id) {
        return experimentService.getExperiment(id)
                .map(experimentMapper::mapToGetExperimentStatusResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id}/{runNo}/status")
    public ResponseEntity<?> getExperimentRunStatus(@PathVariable long id, @PathVariable long runNo) {
        return experimentService.getExperimentRun(id, runNo)
                .map(experimentMapper::mapToGetExperimentRunStatusResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment run not found"));
    }

    @GetMapping("{id}/{runNo}/{partId}/status")
    public ResponseEntity<?> getExperimentPartStatus(@PathVariable long id, @PathVariable long runNo, @PathVariable long partId) {
        return experimentService.getExperimentPart(id, runNo, partId)
                .map(experimentMapper::mapToGetExperimentPartStatusResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

//    @GetMapping("{id}/result")
//    public ResponseEntity<?> getExperimentResult(@PathVariable long id) {
//        return experimentService.getExperimentResult(id)
//                .map(experimentMapper::mapToResultResponse)
//                .map(ResponseEntity::ok)
//                .orElseThrow(() -> new NotFoundException("Experiment not found"));
//    }
//
//    @GetMapping("{id}/result/{partId}")
//    public ResponseEntity<?> getExperimentPartResult(@PathVariable long id, @PathVariable long partId) {
//        return experimentService.getExperimentPartResult(id, partId)
//                .map(part -> {
//                    if (part.getStatus() != ExperimentPartStatus.COMPLETED) {
//                        throw new NotFoundException("Experiment part not completed yet");
//                    }
//                    return experimentMapper.mapToPartResultResponse(part);
//                })
//                .map(ResponseEntity::ok)
//                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
//    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteExperiment(@PathVariable long id) {
        if (experimentService.deleteExperiment(id))
            return ResponseEntity.noContent().build();
        throw new NotFoundException("Experiment not found");
    }

    @DeleteMapping("{id}/{runNo}")
    public ResponseEntity<?> deleteExperimentRun(@PathVariable long id, @PathVariable long runNo) {
        if (experimentService.deleteExperimentRun(id, runNo))
            return ResponseEntity.noContent().build();
        throw new NotFoundException("Experiment run not found");
    }
}