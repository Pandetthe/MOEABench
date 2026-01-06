package pl.edu.agh.to.kotospring.client.scenarios.experiments;

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
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentFullResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponseData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get experiment by ID", type = ScenarioType.EXPERIMENT_MENU, skipOnReturn = true)
public class GetExperimentScenario extends Scenario {

    private final ExperimentClient client;
    private InputForm inputForm;
    private final ExperimentErrorHandler errorHandler;

    public GetExperimentScenario(ExperimentClient client, ExperimentErrorHandler errorHandler) {
        this.client = client;
        this.errorHandler = errorHandler;
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

            GetExperimentFullResponse response = client.getExperiment(id);

            List<String> headers = List.of("Part ID", "Status", "Algorithm", "Problem", "Budget", "Error");
            List<Integer> widths = List.of(10, 10, 14, 14, 10, 40);


            List<List<String>> rows = new ArrayList<>();

            if (response.runs() != null) {
                for (GetExperimentResponse run : response.runs()) {
                    if (run.parts() != null) {
                        for (GetExperimentResponseData part : run.parts()) {
                            List<String> row = new ArrayList<>();
                            row.add(String.valueOf(part.id()));
                            row.add(part.status().name());
                            row.add(part.algorithm());
                            row.add(part.problem());
                            row.add(String.valueOf(part.budget()));
                            row.add(part.errorMessage() == null ? "-" : part.errorMessage());
                            rows.add(row);
                        }
                    }
                }
            }
            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setTitle("Experiment " + id + " Details");
            resultView = tableView;


        } catch (RestClientResponseException e) {
            resultView = errorHandler.httpErrorView(e.getRawStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            resultView = errorHandler.httpErrorView(e.getRawStatusCode(), e.getStatusText(), body);
        } catch (NumberFormatException e) {
            resultView = new SimpleMessageView("Invalid input", "Experiment ID must be a valid integer.");
        } catch (Exception e) {
            resultView = new SimpleMessageView("Error", "Unexpected error: " + e.getMessage());
        }

        configure(resultView);
        View finalResultView = resultView;

        navigate(ScenarioContext.of(resultView, null, () -> {
            if (finalResultView instanceof SimpleMessageView mv) {
                getTerminalUI().setFocus(mv.getContentList());
            } else if (finalResultView instanceof SimpleTableView tv) {
                getTerminalUI().setFocus(tv);
            } else {
                getTerminalUI().setFocus(finalResultView);
            }
        }, null));
    }
}