package pl.edu.agh.to.kotospring.server.experiments;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.problem.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.kotospring.server.algorithms.AlgorithmRegistryService;
import pl.edu.agh.to.kotospring.server.indicators.IndicatorRegistryService;
import pl.edu.agh.to.kotospring.server.problems.ProblemRegistryService;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.List;

@RestController
@RequestMapping(path = "/experiments")
public class ExperimentController {
    private final ProblemRegistryService problemRegistryService;
    private final AlgorithmRegistryService algorithmRegistryService;
    private final IndicatorRegistryService indicatorRegistryService;

    public ExperimentController(ProblemRegistryService problemRegistryService,
                                AlgorithmRegistryService algorithmRegistryService,
                                IndicatorRegistryService indicatorRegistryService) {
        this.problemRegistryService = problemRegistryService;
        this.algorithmRegistryService = algorithmRegistryService;
        this.indicatorRegistryService = indicatorRegistryService;
    }

    @PostMapping
    public ResponseEntity<CreateExperimentResponse> create(@RequestBody CreateExperimentRequest body) {
        Problem problem = problemRegistryService.getProblem(body.problem());
        NondominatedPopulation referenceSet = problemRegistryService.getReferenceSet(body.problem());
        Algorithm algorithm = algorithmRegistryService.getAlgorithm(body.algorithm(), body.algorithmParameters(), problem);
        Indicators indicators = indicatorRegistryService.getIndicators(body.indicators(), problem, referenceSet);
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
