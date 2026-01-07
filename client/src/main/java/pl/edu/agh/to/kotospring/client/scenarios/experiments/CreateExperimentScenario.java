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
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ScenarioComponent(name = "Create new Experiment", type = ScenarioType.EXPERIMENT_MENU)
public class CreateExperimentScenario extends Scenario {

    private final ExperimentClient client;
    private final ExperimentErrorHandler errorHandler;

    public CreateExperimentScenario(ExperimentClient client, ExperimentErrorHandler errorHandler) {
        this.client = client;
        this.errorHandler = errorHandler;
    }

    @Override
    public View build() {
        InputForm form = new InputForm(this.getTerminalUI(), "Create Experiment");
        form.addInput("problems", "Problems (comma separated)");
        form.addInput("algorithms", "Algorithms (comma separated)");
        form.addInput("indicators", "Indicators (comma separated)");
        form.addInput("budget", "Budget (integer)");
        form.addInput("runCount", "Number of runs (integer)");
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
            int runCount = Integer.parseInt(data.get("runCount"));

            List<CreateExperimentRequestData> requestDataList = new ArrayList<>();
            CreateExperimentRequest request = new CreateExperimentRequest(requestDataList, runCount);

            for (String algorithm : algorithmList) {
                if (algorithm.isBlank())
                    continue;
                for (String problem : problemList) {
                    if (problem.isBlank())
                        continue;
                    Map<String, Object> algorithmParameters = Map.of();
                    CreateExperimentRequestData requestData = new CreateExperimentRequestData(
                            problem.trim(),
                            algorithm.trim(),
                            algorithmParameters,
                            indicatorSet,
                            budget);
                    requestDataList.add(requestData);
                }
            }

            if (requestDataList.isEmpty()) {
                throw new IllegalArgumentException("No valid experiments defined. Check inputs.");
            }
            CreateExperimentResponse response = client.createExperiment(request);

            resultView = new SimpleMessageView(
                    "Success",
                    "Experiment successfully created with ID: " + response.id());

            showResult(resultView, true);

        } catch (RestClientResponseException e) {
            resultView = errorHandler.httpErrorView(
                    e.getRawStatusCode(),
                    e.getStatusText(),
                    e.getResponseBodyAsString());
            showResult(resultView, false);
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            resultView = errorHandler.httpErrorView(e.getRawStatusCode(),
                    e.getStatusText(),
                    body);
            showResult(resultView, false);
        } catch (NumberFormatException e) {
            resultView = new SimpleMessageView(
                    "Invalid input",
                    "Budget must be a valid integer.");
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
        Runnable onStart = () -> {
            getTerminalUI().setFocus(resultView);
        };

        if (replace) {
            replace(resultView, onStart);
        } else {
            navigate(createContext(resultView, onStart));
        }
    }
}