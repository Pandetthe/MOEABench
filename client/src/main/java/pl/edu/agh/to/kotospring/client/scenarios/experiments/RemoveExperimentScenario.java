package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.services.ExperimentErrorHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@ScenarioComponent(name = "Delete experiment", type = ScenarioType.EXPERIMENT_MENU)
public class RemoveExperimentScenario extends Scenario {
    private final ExperimentClient client;
    private final ExperimentErrorHandler errorHandler;

    public RemoveExperimentScenario(ExperimentClient client, ExperimentErrorHandler errorHandler) {
        this.client = client;
        this.errorHandler = errorHandler;
    }

    @Override
    public View build() {
        InputForm inputForm = new InputForm(getTerminalUI(), "Delete Experiment by ID");
        inputForm.addInput("id", "Experiment ID");
        inputForm.addInput("runId", "Experiment Run ID (optional)");
        inputForm.setSubmitAction("Delete", this::handleDeleteAction);
        configure(inputForm);
        inputForm.focusFirstInput();
        return inputForm;
    }

    private void handleDeleteAction(Map<String, String> data) {
        View resultView;
        try {
            long id = Long.parseLong(data.get("id"));
            String runIdRaw = data.getOrDefault("runId", "");
            Optional<Long> runId = runIdRaw.isBlank()
                    ? Optional.empty()
                    : Optional.of(Long.parseLong(runIdRaw));

            if (runId.isPresent()) {
                client.deleteExperimentRun(id, runId.get());
                resultView = new SimpleMessageView(
                        "Success",
                        "Run number " + runId.get() + " from experiment with ID " + id
                                + " has been deleted successfully.");
            } else {
                client.deleteExperiment(id);
                resultView = new SimpleMessageView(
                        "Success",
                        "Experiment with ID " + id + " has been deleted successfully.");
            }
            showResult(resultView, true);

        } catch (RestClientResponseException e) {
            resultView = errorHandler.httpErrorView(
                    e.getStatusCode().value(),
                    e.getStatusText(),
                    e.getResponseBodyAsString());
            showResult(resultView, false);
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            resultView = errorHandler.httpErrorView(e.getStatusCode().value(),
                    e.getStatusText(),
                    body);
            showResult(resultView, false);
        } catch (NumberFormatException e) {
            resultView = new SimpleMessageView(
                    "Invalid input",
                    "Experiment ID must be a valid integer.");
            showResult(resultView, false);
        } catch (Exception e) {
            resultView = new SimpleMessageView(
                    "Error",
                    "Unexpected error: " +
                            (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            showResult(resultView, false);
        }
    }

    private void showResult(View resultView, boolean replace) {
        configure(resultView);
        Runnable onStart = () -> getTerminalUI().setFocus(resultView);

        if (replace) {
            replace(resultView, onStart);
        } else {
            navigate(createContext(resultView, onStart));
        }
    }
}