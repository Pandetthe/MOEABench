package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.services.ExperimentErrorHandler;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@ScenarioComponent(name = "Delete experiment group", type = ScenarioType.EXPERIMENT_MENU)
public class RemoveExperimentGroupScenario extends Scenario {

    private final ExperimentClient client;
    private final ExperimentErrorHandler errorHandler;

    public RemoveExperimentGroupScenario(ExperimentClient client, ExperimentErrorHandler errorHandler) {
        this.client = client;
        this.errorHandler = errorHandler;
    }

    @Override
    public View build() {
        InputForm form = new InputForm(this.getTerminalUI(), "Delete experiment group");
        form.addInput("id", "Group ID");
        form.addInput("expId", "Experiment ID (optional)");
        form.addInput("runNo", "Run No (optional)");
        form.setSubmitAction("Delete", this::handleDeleteAction);

        configure(form);
        form.focusFirstInput();
        return form;
    }

    private void handleDeleteAction(Map<String, String> data) {
        View resultView;
        try {
            long id = Long.parseLong(data.get("id"));

            String expIdRaw = data.getOrDefault("expId", "");
            String runNoRaw = data.getOrDefault("runNo", "");

            boolean hasExpId = !expIdRaw.isBlank();
            boolean hasRunNo = !runNoRaw.isBlank();

            if (hasExpId && hasRunNo) {
                long expId = Long.parseLong(expIdRaw);
                long runNo = Long.parseLong(runNoRaw);

                client.deleteRunFromExperimentGroup(id, expId, runNo);
                resultView = new SimpleMessageView(
                        "Success",
                        "Run " + runNo + " of Experiment " + expId + " removed from Group " + id + " successfully.");
            } else {
                client.deleteExperimentGroup(id);
                resultView = new SimpleMessageView(
                        "Success",
                        "Experiment group with ID " + id + " has been deleted successfully.");
            }

            showResult(resultView, true);

        } catch (NumberFormatException e) {
            resultView = new SimpleMessageView(
                    "Invalid input",
                    "IDs must be valid integers.");
            showResult(resultView, false);
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
