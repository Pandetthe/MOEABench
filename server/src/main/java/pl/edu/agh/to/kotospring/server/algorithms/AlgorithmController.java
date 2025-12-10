package pl.edu.agh.to.kotospring.server.algorithms;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(path = "/algorithms")
public class AlgorithmController {
    private final AlgorithmRegistryService algorithmRegistryService;

    public AlgorithmController(AlgorithmRegistryService algorithmRegistryService) {
        this.algorithmRegistryService = algorithmRegistryService;
    }

    @GetMapping
    public Set<String> getAlgorithms() {
        return algorithmRegistryService.getAllRegisteredAlgorithms();
    }
}
