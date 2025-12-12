package pl.edu.agh.to.kotospring.server.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.kotospring.server.services.implementation.ProblemRegistryServiceImpl;

import java.util.Set;

@RestController
@RequestMapping(path = "/problems")
public class ProblemController {
    private final ProblemRegistryServiceImpl problemRegistryService;

    public ProblemController(ProblemRegistryServiceImpl problemRegistryService) {
        this.problemRegistryService = problemRegistryService;
    }

    @GetMapping
    public Set<String> getProblems() {
        return problemRegistryService.getAllRegisteredProblems();
    }
}
