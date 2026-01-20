package pl.edu.agh.to.kotospring.client.scenarios.experiments;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.component.view.control.View;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.models.MenuOption;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioBindings;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentGroupsResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentGroupsResponseData;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponseData;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ScenarioComponent(name = "Get experiment groups", type = ScenarioType.EXPERIMENT_MENU)
public class GetExperimentGroupsScenario extends Scenario {

    private final ExperimentClient experimentClient;
    private final ObjectProvider<GetExperimentGroupScenario> getExperimentGroupScenarioProvider;


    public GetExperimentGroupsScenario(ExperimentClient experimentClient, ObjectProvider<GetExperimentGroupScenario> getExperimentGroupScenarioProvider) {
        this.experimentClient = experimentClient;
        this.getExperimentGroupScenarioProvider = getExperimentGroupScenarioProvider;
    }


    @Override
    public View build() {
        try {
            GetExperimentGroupsResponse response = experimentClient.getExperimentGroups();

            List<String> headers = List.of("ID", "Group Name");
            List<Integer> widths = List.of(5, 30);

            List<List<String>> rows = new ArrayList<>();


            for (GetExperimentGroupsResponseData exp : response) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(exp.id()));
                row.add(exp.name());
                rows.add(row);
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
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
            return new SimpleMessageView("Unexpected Error", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private void handleRowSelection(MenuOption option) {
        String rowText = option.name();

        if (rowText.contains("ID") && rowText.contains("Group Name"))
            return;
        if (rowText.startsWith("--"))
            return;
        if (rowText.contains("<<") || rowText.contains(">>"))
            return;

        try {
            String[] columns = rowText.split("\\|");
            if (columns.length > 0) {
                String groupIdStr = columns[0].trim();
                long groupId = Long.parseLong(groupIdStr);

                openOptionsScenario(groupId);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void openOptionsScenario(long groupId) {
        GetExperimentGroupScenario groupScenario = getExperimentGroupScenarioProvider.getObject();
        groupScenario.setGroupId(groupId);
        wireChild(groupScenario);
        navigate(groupScenario.buildContext());
    }
}
