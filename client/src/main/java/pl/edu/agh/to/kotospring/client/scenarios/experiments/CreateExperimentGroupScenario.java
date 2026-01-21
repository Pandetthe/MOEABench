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
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentGroupRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@ScenarioComponent(name = "Create experiment group", type = ScenarioType.EXPERIMENT_MENU)
public class CreateExperimentGroupScenario extends Scenario {

    private final ExperimentClient client;
    private final ExperimentErrorHandler errorHandler;

    public CreateExperimentGroupScenario(ExperimentClient client, ExperimentErrorHandler errorHandler) {
        this.client = client;
        this.errorHandler = errorHandler;
    }

    @Override
    public View build() {
        InputForm form = new InputForm(this.getTerminalUI(), "Create experiment group");
        form.addInput("name", "Group Name");
        form.setSubmitAction("Create", this::handleCreateAction);

        configure(form);
        form.focusFirstInput();
        return form;
    }

    private void handleCreateAction(Map<String, String> data) {
        View resultView;
        try {
            String name = data.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Group name cannot be empty.");
            }

            CreateExperimentGroupRequest request = new CreateExperimentGroupRequest(name.trim());
            client.createExperimentGroup(request);

            resultView = new SimpleMessageView(
                    "Success",
                    "Experiment group '" + name + "' created successfully.");

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
