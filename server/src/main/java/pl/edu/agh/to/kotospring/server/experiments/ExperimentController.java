package pl.edu.agh.to.kotospring.server.experiments;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.problem.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.kotospring.server.algorithms.AlgorithmRegistry;
import pl.edu.agh.to.kotospring.server.indicators.IndicatorRegistry;
import pl.edu.agh.to.kotospring.server.problems.ProblemRegistry;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.List;

@RestController
@RequestMapping(path = "/experiments")
public class ExperimentController {
    private final ProblemRegistry problemRegistry;
    private final AlgorithmRegistry algorithmRegistry;
    private final IndicatorRegistry indicatorRegistry;

    public ExperimentController(ProblemRegistry problemRegistry,
                                AlgorithmRegistry algorithmRegistry,
                                IndicatorRegistry indicatorRegistry) {
        this.problemRegistry = problemRegistry;
        this.algorithmRegistry = algorithmRegistry;
        this.indicatorRegistry = indicatorRegistry;
    }

    @PostMapping
    public ResponseEntity<CreateExperimentResponse> create(@RequestBody CreateExperimentRequest body) {
        Problem problem = problemRegistry.getProblem(body.problem());
        NondominatedPopulation referenceSet = problemRegistry.getReferenceSet(body.problem());
        Algorithm algorithm = algorithmRegistry.getAlgorithm(body.algorithm(), body.algorithmParameters(), problem);
        Indicators indicators = indicatorRegistry.getIndicators(body.indicators(), problem, referenceSet);
        return ResponseEntity.ok(null);
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
    public ResponseEntity<List<GetExperimentResultResponse>> getExperimentResult(@PathVariable int id) {
        return null;
    }

    @DeleteMapping("{id:int}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable int id) {
        return null;
    }
}
