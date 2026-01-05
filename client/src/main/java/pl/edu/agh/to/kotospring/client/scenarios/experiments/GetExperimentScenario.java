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
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponseData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get experiment by ID", type = ScenarioType.EXPERIMENT_MENU)
public class GetExperimentScenario extends Scenario {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ExperimentClient client;
    private InputForm inputForm;

    public GetExperimentScenario(ExperimentClient client) {
        this.client = client;
    }

    @Override
    public View build() {
        inputForm = new InputForm(getTerminalUI(), "Get Experiment by ID");
        inputForm.addInput("id", "Experiment ID");
        inputForm.setSubmitAction("Search", this::handleGetAction);

        configure(inputForm);
        return inputForm;
    }

    private void handleGetAction(Map<String, String> data) {
        View resultView;
        try {
            long id = Long.parseLong(data.get("id"));

            GetExperimentResponse response = client.getExperiment(id);

            List<String> headers = List.of("ID", "Status", "Algorithm", "Problem", "Budget", "Error");
            List<Integer> widths = List.of(5, 10, 14, 14, 10, 40);

            List<List<String>> rows = new ArrayList<>();

            for (GetExperimentResponseData exp : response.parts()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(exp.id()));
                row.add(exp.status().name());
                row.add(exp.algorithm());
                row.add(exp.problem());
                row.add(String.valueOf(exp.budget()));
                row.add(exp.errorMessage() == null ? "None" : exp.errorMessage());
                rows.add(row);
            }

            resultView = new SimpleTableView(headers, rows, widths);
            ((SimpleTableView) resultView).setTitle("Experiment " + id + " Details");

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
                    "Experiment ID must be a valid integer."
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
        navigate(ScenarioContext.of(resultView, () -> {
            if (finalResultView instanceof SimpleMessageView mv) {
                getTerminalUI().setFocus(mv.getContentList());
            } else if (finalResultView instanceof SimpleTableView tv) {
                getTerminalUI().setFocus(tv);
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