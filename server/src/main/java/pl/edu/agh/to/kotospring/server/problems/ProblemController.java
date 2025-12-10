package pl.edu.agh.to.kotospring.server.problems;

import org.moeaframework.core.spi.ProblemFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(path = "/problems")
public class ProblemController {
    private final ProblemRegistryService problemRegistryService;

    public ProblemController(ProblemRegistryService problemRegistryService) {
        this.problemRegistryService = problemRegistryService;
    }

    @GetMapping
    public Set<String> getProblems() {
        return problemRegistryService.getAllRegisteredProblems();
    }
}
