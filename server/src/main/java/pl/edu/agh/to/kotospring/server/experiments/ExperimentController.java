package pl.edu.agh.to.kotospring.server.experiments;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.List;

@RestController
@RequestMapping(path = "/experiments")
public class ExperimentController {
    @PostMapping
    public ResponseEntity<CreateExperimentResponse> create(@RequestBody CreateExperimentRequest body) {
        return null;
    }

    @GetMapping
    public ResponseEntity<List<GetExperimentsResponse>> getExperiments() {
        return null;
    }

    @GetMapping("{id:int}")
    public ResponseEntity<GetExperimentResponse> getExperiment() {
        return null;
    }

    @GetMapping("{id:int}/status")
    public ResponseEntity<GetExperimentStatusResponse> getExperimentStatus(@PathVariable int id) {
        return null;
    }

    @GetMapping("{id:int}/result")
    public ResponseEntity<GetExperimentResultResponse> getExperimentResult(@PathVariable int id) {
        return null;
    }

    @DeleteMapping("{id:int}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable int id) {
        return null;
    }
}
