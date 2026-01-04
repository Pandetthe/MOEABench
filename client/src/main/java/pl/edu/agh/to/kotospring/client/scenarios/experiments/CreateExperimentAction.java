package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CreateExperimentAction implements ExperimentAction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String getMenuLabelAndTitle() {
        return "Create new Experiment";
    }

    @Override
    public void configureInput(InputForm form) {
        form.addInput("problems", "Problems");
        form.addInput("algorithms", "Algorithms");
        form.addInput("indicators", "Indicators");
        form.addInput("budget", "Budget");
    }

    @Override
    public View execute(Map<String, String> data, ExperimentClient client) {
        try {
            List<String> algorithmList = List.of(data.get("algorithms").split(","));
            List<String> problemList = List.of(data.get("problems").split(","));
            Set<String> indicatorSet = Set.of(data.get("indicators").split(","));
            int budget = Integer.parseInt(data.get("budget"));

            CreateExperimentRequest request = new CreateExperimentRequest();

            for (String algorithm : algorithmList) {
                for (String problem : problemList) {
                    Map<String, Object> algorithmParameters = Map.of();
                    CreateExperimentRequestData requestData =
                            new CreateExperimentRequestData(
                                    problem.trim(),
                                    algorithm.trim(),
                                    algorithmParameters,
                                    indicatorSet,
                                    budget
                            );
                    request.add(requestData);
                }
            }

            CreateExperimentResponse response = client.createExperiment(request);

            return new SimpleMessageView(
                    "Success",
                    "Experiment successfully created with ID: " + response.id()
            );

        } catch (RestClientResponseException e) {
            return httpErrorView(
                    e.getRawStatusCode(),
                    e.getStatusText(),
                    e.getResponseBodyAsString()
            );
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            return httpErrorView(e.getRawStatusCode(), e.getStatusText(), body);
        } catch (NumberFormatException e) {
            return new SimpleMessageView(
                    "Invalid input",
                    "Budget must be a valid integer."
            );
        } catch (Exception e) {
            return new SimpleMessageView(
                    "Error",
                    "Unexpected error: " +
                            (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
            );
        }
    }

    private View httpErrorView(int status, String statusText, String body) {
        String msg = extractServerMessage(body);

        String title = "HTTP " + status +
                (statusText == null || statusText.isBlank() ? "" : " " + statusText);

        if (msg == null || msg.isBlank()) {
            msg = "Request failed (no details returned by server).";
        }

        return new SimpleMessageView(title, msg);
    }

    private String extractServerMessage(String body) {
        if (body == null) return "";
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return "";

        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return trimmed;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(trimmed);
            if (node.hasNonNull("message")) return node.get("message").asText();
            if (node.hasNonNull("error")) return node.get("error").asText();
            if (node.hasNonNull("detail")) return node.get("detail").asText();
            return trimmed;
        } catch (Exception ignored) {
            return trimmed;
        }
    }
}
