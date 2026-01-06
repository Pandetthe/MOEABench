package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentPartResponse;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ScenarioComponent(name = "", type = ScenarioType.EXPERIMENT_MENU, skipOnReturn = true)
public class GetExperimentPartScenario extends Scenario {

    private ExperimentClient client;
    private long experimentId;
    private long runId;
    private long partId;

    protected GetExperimentPartScenario() {
    }

    public GetExperimentPartScenario(ExperimentClient client, long experimentId, long runId, long partId) {
        this.client = client;
        this.experimentId = experimentId;
        this.runId = runId;
        this.partId = partId;
    }

    @Override
    public View build() {
        try {
            GetExperimentPartResponse response = client.getExperimentPart(experimentId, runId, partId);

            List<String> headers = List.of("Status", "Algorithm", "Problem", "Indicator", "Started", "Finished");
            List<Integer> widths = List.of(13, 10, 10, 23, 20, 20);
            List<List<String>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());

            if (response != null) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(response.status()));
                row.add(response.algorithm());
                row.add(response.problem());
                Set<String> indicators = response.indicators();
                row.add(indicators != null ? String.join(",", indicators) : "-");
                row.add(response.startedAt() != null ? formatter.format(response.startedAt()) : "-");
                row.add(response.finishedAt() != null ? formatter.format(response.finishedAt()) : "-");
                rows.add(row);

            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setTitle("Details for Experiment ID: " + experimentId + ", Run No: " + runId + ", Part ID: " + partId);
            tableView.setAutoRunOnOpen(false);
            configure(tableView);

            return tableView;

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