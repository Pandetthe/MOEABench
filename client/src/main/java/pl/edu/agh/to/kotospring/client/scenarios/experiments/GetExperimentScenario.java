package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.models.MenuOption;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponse;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get experiment", type = ScenarioType.OTHER)
public class GetExperimentScenario extends Scenario {

    private ExperimentClient client;
    private long experimentId;

    private ExperimentRunStatus runStatus = null;

    public GetExperimentScenario(ExperimentClient client, long experimentId) {
        this.client = client;
        this.experimentId = experimentId;
    }

    protected GetExperimentScenario() {
    }

    @Override
    public View build() {
        try {

            GetExperimentResponse response = client.getExperiment(experimentId, runStatus);

            List<String> headers = List.of("RunNo", "Status", "Started", "Finished");
            List<Integer> widths = List.of(10, 15, 30, 30);

            List<List<String>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());

            if (response.runs() != null) {
                for (var run : response.runs()) {
                    List<String> row = new ArrayList<>();
                    row.add(String.valueOf(run.runNo()));
                    row.add(run.status().name());
                    row.add(run.startedAt() != null ? formatter.format(run.startedAt()) : "-");
                    row.add(run.finishedAt() != null ? formatter.format(run.finishedAt()) : "-");
                    rows.add(row);
                }
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setTitle("Details for Experiment ID: " + experimentId);
            tableView.setAutoRunOnOpen(false);
            configure(tableView);

            getEventloop().onDestroy(
                    getEventloop().keyEvents()
                            .subscribe(event -> {
                                if (tableView.hasFocus() && event.getPlainKey() == 'f' && event.hasCtrl()) {
                                    openFilterForm();
                                }
                            }));

            getEventloop().onDestroy(
                    getEventloop().viewEvents(ResizingListView.ResizingListViewOpenSelectedItemEvent.class, tableView)
                            .subscribe(event -> {
                                Object item = event.args().item();
                                if (item instanceof MenuOption option) {
                                    handleRowSelection(option);
                                }
                            }));

            return tableView;

        } catch (RestClientResponseException e) {
            return new SimpleMessageView("Error", "HTTP Error: " + e.getStatusText());
        } catch (WebClientResponseException e) {
            return new SimpleMessageView("Error", "Server Error: " + e.getStatusCode());
        } catch (Exception e) {
            return new SimpleMessageView("Error", "Unexpected error: " + e.getMessage());
        }
    }

    private void openFilterForm() {
        InputForm form = new InputForm(getTerminalUI(), "Filter Experiments");

        form.addInput("status", "Status (QUEUED, IN_PROGRESS, SUCCESS, PARTIAL_SUCCESS, FAILED)");

        form.setSubmitAction("Apply Filters", this::handleFilterSubmit);
        configure(form);
        form.focusFirstInput();
        navigate(ScenarioContext.of(form, this));
    }

    private void handleFilterSubmit(Map<String, String> data) {
        try {
            String statusStr = data.get("status");

            if (statusStr != null && !statusStr.isBlank()) {
                this.runStatus = ExperimentRunStatus.valueOf(statusStr.trim().toUpperCase());
            } else {
                this.runStatus = null;
            }

            View filteredTableView = build();
            navigate(ScenarioContext.of(filteredTableView, this, () -> {
                if (filteredTableView instanceof SimpleTableView tv) {
                    getTerminalUI().setFocus(tv);
                }
            }, null));

        } catch (IllegalArgumentException | DateTimeParseException e) {
            SimpleMessageView errorView = new SimpleMessageView("Filter Error",
                    "Invalid input format: " + e.getMessage());
            configure(errorView);
            navigate(ScenarioContext.of(errorView, this, () -> getTerminalUI().setFocus(errorView),
                    null));
        }
    }

    private void resetFilters() {
        this.runStatus = null;
    }

    @Override
    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this, null, this::resetFilters);
    }

    private void handleRowSelection(MenuOption option) {
        String rowText = option.name();
        if (rowText.contains("RunNo") && rowText.contains("Status"))
            return;
        if (rowText.startsWith("--"))
            return;
        if (rowText.contains("<<") || rowText.contains(">>"))
            return;

        try {

            String[] columns = rowText.split("\\|");
            if (columns.length > 0) {
                String idStr = columns[0].trim();
                long runId = Long.parseLong(idStr);

                openDetailsScenario(experimentId, runId);
            }
        } catch (NumberFormatException e) {
        }
    }

    private void openDetailsScenario(long experimentId, long runId) {
        GetExperimentRunScenario experimentScenario = new GetExperimentRunScenario(client, experimentId, runId);
        experimentScenario.configure(getTerminalUI());
        ScenarioContext context = experimentScenario.buildContext();
        experimentScenario.setNavigationConsumer(this::navigate);
        experimentScenario.setStatusBarConsumer(this::setStatusBar);

        navigate(context);
    }
}