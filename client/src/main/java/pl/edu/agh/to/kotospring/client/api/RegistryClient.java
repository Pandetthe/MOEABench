package pl.edu.agh.to.kotospring.client.api;

import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface RegistryClient {
    @GetExchange("/algorithms")
    List<String> getAlgorithms();

    @GetExchange("/problems")
    List<String> getProblems();

    @GetExchange("/indicators")
    List<String> getIndicators();
}
