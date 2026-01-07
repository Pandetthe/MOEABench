package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.shell.component.view.control.View;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.CenteredButtonView;
import pl.edu.agh.to.kotospring.client.views.FixedGridView;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentPartResponse;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ScenarioComponent(name = "Get experiment part", type = ScenarioType.OTHER)
@Scope("prototype")
public class GetExperimentPartScenario extends Scenario {

    private final ExperimentClient client;
    private long experimentId;
    private long runId;
    private long partId;

    private final ObjectProvider<GetExperimentPartResultScenario> getExperimentPartResultScenarioProvider;

    public GetExperimentPartScenario(ExperimentClient client,
            ObjectProvider<GetExperimentPartResultScenario> getExperimentPartResultScenarioProvider) {
        this.client = client;
        this.getExperimentPartResultScenarioProvider = getExperimentPartResultScenarioProvider;
    }

    public void setExperimentId(long experimentId) {
        this.experimentId = experimentId;
    }

    public void setRunId(long runId) {
        this.runId = runId;
    }

    public void setPartId(long partId) {
        this.partId = partId;
    }

    @Override
    public View build() {
        try {
            GetExperimentPartResponse response = client.getExperimentPart(experimentId, runId, partId);

            FixedGridView grid = new FixedGridView();
            configure(grid);
            grid.setTitle("Details for Experiment ID: " + experimentId + ", Run No: " + runId + ", Part ID: " + partId);
            grid.setShowBorders(true);

            grid.setRowSize(0, 0);
            grid.setRowSize(1, 1);

            List<String> headers = List.of("Property", "Value");
            List<Integer> widths = List.of(20, 60);
            List<List<String>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());

            if (response != null) {
                rows.add(List.of("Status", String.valueOf(response.status())));
                rows.add(List.of("Algorithm", response.algorithm() != null ? response.algorithm() : "-"));
                rows.add(List.of("Problem", response.problem() != null ? response.problem() : "-"));
                Set<String> indicators = response.indicators();
                rows.add(List.of("Indicators", indicators != null ? String.join(", ", indicators) : "-"));
                rows.add(List.of("Started At",
                        response.startedAt() != null ? formatter.format(response.startedAt()) : "-"));
                rows.add(List.of("Finished At",
                        response.finishedAt() != null ? formatter.format(response.finishedAt()) : "-"));
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setEnableWrapping(false);
            configure(tableView);
            grid.addItem(tableView, 0, 0, 1, 1, 0, 0);

            CenteredButtonView resultsButton = new CenteredButtonView();
            resultsButton.setText("View Results");
            resultsButton.setAction(this::openResults);
            configure(resultsButton);
            grid.addItem(resultsButton, 1, 0, 1, 1, 0, 0);

            return grid;

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

    private void openResults() {
        GetExperimentPartResultScenario partResultScenario = getExperimentPartResultScenarioProvider.getObject();
        partResultScenario.setExperimentId(experimentId);
        partResultScenario.setRunNo(runId);
        partResultScenario.setPartId(partId);
        wireChild(partResultScenario);
        navigate(partResultScenario.buildContext());
    }

}
