package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.shell.component.view.control.View;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ScenarioComponent(name = "Create new Experiment", type = ScenarioType.EXPERIMENT_MENU, skipOnReturn = true)
public class CreateExperimentScenario extends Scenario {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ExperimentClient client;

    public CreateExperimentScenario(ExperimentClient client) {
        this.client = client;
    }

    @Override
    public View build() {
        InputForm form = new InputForm(this.getTerminalUI(), "Create Experiment");
        form.addInput("problems", "Problems (comma separated)");
        form.addInput("algorithms", "Algorithms (comma separated)");
        form.addInput("indicators", "Indicators (comma separated)");
        form.addInput("budget", "Budget (integer)");
        form.setSubmitAction("Create", this::handleCreateAction);

        configure(form);
        form.focusFirstInput();
        return form;
    }

    private void handleCreateAction(Map<String, String> data) {
        View resultView;
        try {
            List<String> algorithmList = List.of(data.get("algorithms").split(","));
            List<String> problemList = List.of(data.get("problems").split(","));
            Set<String> indicatorSet = Set.of(data.get("indicators").split(","));
            int budget = Integer.parseInt(data.get("budget"));
            CreateExperimentRequest request = new CreateExperimentRequest();

            for (String algorithm : algorithmList) {
                if (algorithm.isBlank()) continue;
                for (String problem : problemList) {
                    if (problem.isBlank()) continue;
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

            if (request.isEmpty()) {
                throw new IllegalArgumentException("No valid experiments defined. Check inputs.");
            }
            CreateExperimentResponse response = client.createExperiment(request);

            resultView = new SimpleMessageView(
                    "Success",
                    "Experiment successfully created with ID: " + response.id()
            );

        } catch (RestClientResponseException e) {
            resultView = httpErrorView(
                    e.getRawStatusCode(),
                    e.getStatusText(),
                    e.getResponseBodyAsString()
            );
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            resultView = httpErrorView(e.getRawStatusCode(), e.getStatusText(), body);
        } catch (NumberFormatException e) {
            resultView = new SimpleMessageView(
                    "Invalid input",
                    "Budget must be a valid integer."
            );
        } catch (Exception e) {
            resultView = new SimpleMessageView(
                    "Error",
                    "Unexpected error: " +
                            (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
            );
        }
        configure(resultView);

        View finalResultView = resultView;
        navigate(ScenarioContext.of(resultView, null, () -> {
            if (finalResultView instanceof SimpleMessageView mv) {
                getTerminalUI().setFocus(mv.getContentList());
            } else {
                getTerminalUI().setFocus(finalResultView);
            }
        }, null));
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