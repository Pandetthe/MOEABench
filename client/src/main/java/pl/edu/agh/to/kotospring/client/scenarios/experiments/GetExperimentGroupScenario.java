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

import org.springframework.beans.factory.ObjectProvider;
import pl.edu.agh.to.kotospring.client.models.MenuOption;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioBindings;

import java.util.ArrayList;
import java.util.List;

@ScenarioComponent(name = "Get experiment group", type = ScenarioType.OTHER)
public class GetExperimentGroupScenario extends Scenario {

    private long groupId;
    private final ExperimentClient experimentClient;
    private final ObjectProvider<GetExperimentRunScenario> experimentRunScenarioProvider;

    public GetExperimentGroupScenario(ExperimentClient experimentClient,
                                      ObjectProvider<GetExperimentRunScenario> experimentRunScenarioProvider) {
        this.experimentClient = experimentClient;
        this.experimentRunScenarioProvider = experimentRunScenarioProvider;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    @Override
    public View build() {
        try {
            var group = experimentClient.getExperimentGroup(groupId);
            var runs = group.runs();

            List<String> headers = List.of("Experiment ID", "Run No");
            List<Integer> widths = List.of(20, 10);
            List<List<String>> rows = new ArrayList<>();

            for (var run : runs) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(run.experimentId()));
                row.add(String.valueOf(run.runNo()));
                rows.add(row);
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setTitle("Group: " + group.name());
            tableView.setAutoRunOnOpen(false);
            configure(tableView);

            ScenarioBindings bindings = new ScenarioBindings(getEventloop());
            bindings.onOpenSelectedItem(tableView, MenuOption.class, this::handleRowSelection);

            return tableView;

        } catch (WebClientRequestException e) {
            return new SimpleMessageView("Connection Error", "Service unavailable. Could not connect to the server.");
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                return new SimpleMessageView("Server Error", "Internal Server Error. Please try again later.");
            }
            return new SimpleMessageView("Error", "Server returned error: " + e.getStatusCode());
        } catch (Exception e) {
            return new SimpleMessageView("Error", "Could not fetch experiment group: " + e.getMessage());
        }
    }

    private void handleRowSelection(MenuOption option) {
        String rowText = option.name();

        try {
            String[] parts = rowText.split("\\|");
            if (parts.length >= 2) {
                long experimentId = Long.parseLong(parts[0].trim());
                long runNo = Long.parseLong(parts[1].trim());

                GetExperimentRunScenario scenario = experimentRunScenarioProvider.getObject();
                scenario.setExperimentId(experimentId);
                scenario.setRunNo(runNo);
                wireChild(scenario);
                navigate(scenario.buildContext());
            }
        } catch (NumberFormatException e) {
        }
    }
}
