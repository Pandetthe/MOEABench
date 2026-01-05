package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get list of experiments", type = ScenarioType.EXPERIMENT_MENU)
public class GetExperimentsScenario extends Scenario {
    private final ExperimentClient experimentClient;

    public GetExperimentsScenario(ExperimentClient experimentClient) {
        this.experimentClient = experimentClient;
    }

    @Override
    public View build() {
        try {
            GetExperimentsResponse response = experimentClient.getExperiments();
            List<String> headers = List.of("ID", "Status", "Queued", "Started", "Finished");
            List<Integer> widths = List.of(5, 16, 27, 27, 27);

            List<List<String>> rows = new ArrayList<>();

            for (GetExperimentsResponseData exp : response) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(exp.id()));
                row.add(exp.status().name());
                row.add(exp.queuedAt() != null ? String.valueOf(exp.queuedAt()) : "-");
                row.add(exp.startedAt() != null ? String.valueOf(exp.startedAt()) : "-");
                row.add(exp.finishedAt() != null ? String.valueOf(exp.finishedAt()) : "-");
                rows.add(row);
            }

            return new SimpleTableView(headers, rows, widths);
        } catch (WebClientRequestException e) {
            return new SimpleMessageView("Connection Error", "Service unavailable. Could not connect to the server.");
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                return new SimpleMessageView("Server Error", "Internal Server Error. Please try again later.");
            }
            return new SimpleMessageView("Error", "Server returned error: " + e.getStatusCode());
        } catch (Exception e) {
            return new SimpleMessageView("Unexpected Error", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
