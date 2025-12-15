package pl.edu.agh.to.kotospring.client.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RegistryApi {

    private final RestClient restClient;

    public RegistryApi(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<String> getAlgorithms() {
        return restClient.get()
                .uri("/algorithms")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<String> getProblems() {
        return restClient.get()
                .uri("/problems")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<String> getIndicators() {
        return restClient.get()
                .uri("/indicators")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
