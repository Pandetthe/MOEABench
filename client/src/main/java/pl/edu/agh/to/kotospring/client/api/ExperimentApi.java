package pl.edu.agh.to.kotospring.client.api;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

@Service
public class ExperimentApi {

    private final RestClient restClient;

    public ExperimentApi(RestClient restClient) {
        this.restClient = restClient;
    }

    public GetExperimentsResponse getExperiments() {
        return restClient.get()
                .uri("/experiments")
                .retrieve()
                .body(GetExperimentsResponse.class);
    }

    public GetExperimentStatusResponse getExperimentStatus(long id) {
        return restClient.get()
                .uri("/experiments/{id}/status/", id)
                .retrieve()
                .body(GetExperimentStatusResponse.class);
    }
    public GetExperimentPartStatusResponse getExperimentPartStatus(long id, long partId) {
        return restClient.get()
                .uri("/experiments/{id}/status/{partId}", id, partId)
                .retrieve()
                .body(GetExperimentPartStatusResponse.class);
    }


    public GetExperimentResponse getExperiment(long id) {
        return restClient.get()
                .uri("/experiments/{id}", id)
                .retrieve()
                .body(GetExperimentResponse.class);
    }

    public GetExperimentResultResponse getExperimentResult(long id) {
        return restClient.get()
                .uri("/experiments/{id}/result", id)
                .retrieve()
                .body(GetExperimentResultResponse.class);
    }
    public GetExperimentPartResultResponse getExperimentPartResult(long id, long partId) {
        return restClient.get()
                .uri("/experiments/{id}/result/{partId}", id, partId)
                .retrieve()
                .body(GetExperimentPartResultResponse.class);
    }

    public CreateExperimentResponse createExperiment(CreateExperimentRequest request) {
        return restClient.post()
                .uri("/experiments")
                .body(request)
                .retrieve()
                .body(CreateExperimentResponse.class);
    }
    public void deleteExperiment(long id) {
        restClient.delete()
                .uri("/experiments/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
