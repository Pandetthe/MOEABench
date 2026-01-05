package pl.edu.agh.to.kotospring.server.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.kotospring.server.services.implementation.AlgorithmRegistryServiceImpl;

import java.util.Set;

@RestController
@RequestMapping(path = "/algorithms")
public class AlgorithmController {
    private final AlgorithmRegistryServiceImpl algorithmRegistryService;

    public AlgorithmController(AlgorithmRegistryServiceImpl algorithmRegistryService) {
        this.algorithmRegistryService = algorithmRegistryService;
    }

    @GetMapping
    public Set<String> getAlgorithms() {
        return algorithmRegistryService.getAllRegisteredAlgorithms();
    }
}
