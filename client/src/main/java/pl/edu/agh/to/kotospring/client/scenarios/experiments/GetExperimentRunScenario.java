package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.web.reactive.function.client.WebClientRequestException;
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
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentRunResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentRunResponseData;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get experiment run", type = ScenarioType.OTHER)
public class GetExperimentRunScenario extends Scenario {

    private ExperimentClient client;
    private long experimentId;
    private long runNo;

    private String currentAlgorithm = null;
    private String currentProblem = null;
    private String currentIndicator = null;
    private ExperimentPartStatus currentStatus = null;

    public GetExperimentRunScenario(ExperimentClient client, long experimentId, long runNo) {
        this.client = client;
        this.experimentId = experimentId;
        this.runNo = runNo;
    }

    public GetExperimentRunScenario() {

    }

    @Override
    public View build() {
        try {
            GetExperimentRunResponse response = client.getExperimentRun(
                    experimentId,
                    runNo,
                    currentAlgorithm,
                    currentProblem,
                    currentIndicator,
                    currentStatus);

            List<String> headers = List.of("Part ID", "Status", "Algorithm", "Problem", "Budget", "Started",
                    "Finished");
            List<Integer> widths = List.of(8, 15, 12, 12, 8, 20, 20);

            List<List<String>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());

            if (response.parts() != null) {
                for (GetExperimentRunResponseData run : response.parts()) {
                    List<String> row = new ArrayList<>();
                    row.add(String.valueOf(run.id()));
                    row.add(run.status().toString());
                    row.add(run.algorithm());
                    row.add(run.problem());
                    row.add(String.valueOf(run.budget()));
                    row.add(run.startedAt() != null ? formatter.format(run.startedAt()) : "-");
                    row.add(run.finishedAt() != null ? formatter.format(run.finishedAt()) : "-");
                    rows.add(row);
                }
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setTitle("Details for Experiment ID: " + experimentId + ", Run No: " + runNo);
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

    private void openFilterForm() {
        InputForm form = new InputForm(getTerminalUI(), "Filter Experiments");

        form.addInput("algorithm", "Algorithm");
        form.addInput("problem", "Problem");
        form.addInput("indicator", "Indicator");
        form.addInput("status", "Status (QUEUED, RUNNING, COMPLETED, FAILED)");

        form.setSubmitAction("Apply Filters", this::handleFilterSubmit);
        configure(form);
        form.focusFirstInput();
        navigate(ScenarioContext.of(form, this));
    }

    private void handleFilterSubmit(Map<String, String> data) {
        try {
            String algorithm = data.get("algorithm");
            String problem = data.get("problem");
            String indicator = data.get("indicator");
            String statusStr = data.get("status");

            this.currentAlgorithm = (algorithm != null && !algorithm.isBlank()) ? algorithm.trim() : null;
            this.currentProblem = (problem != null && !problem.isBlank()) ? problem.trim() : null;
            this.currentIndicator = (indicator != null && !indicator.isBlank()) ? indicator.trim() : null;
            if (statusStr != null && !statusStr.isBlank()) {
                this.currentStatus = ExperimentPartStatus.valueOf(statusStr.trim().toUpperCase());
            } else {
                this.currentStatus = null;
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
        this.currentAlgorithm = null;
        this.currentProblem = null;
        this.currentIndicator = null;
        this.currentStatus = null;
    }

    @Override
    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this, null, this::resetFilters);
    }

    private void handleRowSelection(MenuOption option) {
        String rowText = option.name();
        if (rowText.contains("Part ID") && rowText.contains("Status"))
            return;
        if (rowText.startsWith("--"))
            return;
        if (rowText.contains("<<") || rowText.contains(">>"))
            return;

        try {

            String[] columns = rowText.split("\\|");
            if (columns.length > 0) {
                String idStr = columns[0].trim();
                long partId = Long.parseLong(idStr);

                openDetailsScenario(experimentId, runNo, partId);
            }
        } catch (NumberFormatException e) {
        }
    }

    private void openDetailsScenario(long experimentId, long runId, long partId) {
        GetExperimentPartResultScenario experimentPartScenario = new GetExperimentPartResultScenario(client,
                experimentId, runId, partId);
        experimentPartScenario.configure(getTerminalUI());
        ScenarioContext context = experimentPartScenario.buildContext();
        experimentPartScenario.setNavigationConsumer(this::navigate);
        experimentPartScenario.setStatusBarConsumer(this::setStatusBar);

        navigate(context);
    }

}
