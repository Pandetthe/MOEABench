package pl.edu.agh.to.kotospring.server.problems;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.kotospring.shared.problems.contracts.GetProblemsResponse;

import java.util.List;

@RestController
@RequestMapping(path = "/problems")
public class ProblemController {
    @GetMapping
    public ResponseEntity<List<GetProblemsResponse>> getProblems() {
        return null;
    }
}
