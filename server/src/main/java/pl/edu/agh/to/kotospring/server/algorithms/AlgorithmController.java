package pl.edu.agh.to.kotospring.server.algorithms;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.kotospring.shared.algorithms.contracts.GetAlgorithmsResponse;

import java.util.List;

@RestController
@RequestMapping(path = "/algorithms")
public class AlgorithmController {
    @GetMapping
    public ResponseEntity<List<GetAlgorithmsResponse>> getAlgorithms() {
        return null;
    }
}
