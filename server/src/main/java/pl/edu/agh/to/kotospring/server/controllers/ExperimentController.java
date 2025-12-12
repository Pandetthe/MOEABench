package pl.edu.agh.to.kotospring.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentService;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

@RestController
@RequestMapping(path = "/experiments")
public final class ExperimentController {
    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateExperimentRequest body) {
        return ResponseEntity.ok(experimentService.createExperiment(body));
    }

    @GetMapping
    public ResponseEntity<?> getExperiments() {
        return ResponseEntity.ok(experimentService.getExperiments());
    }

    @GetMapping("{id:long}")
    public ResponseEntity<?> getExperiment(@PathVariable long id) {
        return experimentService.getExperiment(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id:long}/status/")
    public ResponseEntity<?> getExperimentStatus(@PathVariable long id) {
        return experimentService.getExperimentStatus(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id:long}/status/{partId:long}")
    public ResponseEntity<?> getExperimentStatus(@PathVariable long id, @PathVariable long partId) {
        return experimentService.getExperimentStatus(id, partId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @GetMapping("{id:long}/result")
    public ResponseEntity<?> getExperimentResult(@PathVariable long id) {
        return experimentService.getExperimentResult(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    @GetMapping("{id:long}/result/{partId:long}")
    public ResponseEntity<?> getExperimentPartResult(@PathVariable long id, @PathVariable long partId) {
        return experimentService.getExperimentResult(id, partId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Experiment part not found"));
    }

    @DeleteMapping("{id:long}")
    public ResponseEntity<?> deleteExperiment(@PathVariable long id) {
        if (experimentService.deleteExperiment(id))
            return ResponseEntity.noContent().build();
        throw new NotFoundException("Experiment not found");
    }
}
