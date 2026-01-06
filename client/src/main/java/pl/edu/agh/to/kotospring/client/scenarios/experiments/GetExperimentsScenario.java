package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponseData;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get list of experiments", type = ScenarioType.EXPERIMENT_MENU, skipOnReturn = true)
public class GetExperimentsScenario extends Scenario {
    private final ExperimentClient experimentClient;

    private String currentAlgorithm = null;
    private String currentProblem = null;
    private String currentIndicator = null;
    private ExperimentStatus currentStatus = null;
    private OffsetDateTime currentStartTime = null;
    private OffsetDateTime currentEndTime = null;

    public GetExperimentsScenario(ExperimentClient experimentClient) {
        this.experimentClient = experimentClient;
    }

    @Override
    public View build() {
        try {
            GetExperimentsResponse response = experimentClient.getExperiments(
                    currentAlgorithm,
                    currentProblem,
                    currentIndicator,
                    currentStatus,
                    currentStartTime,
                    currentEndTime
            );
            List<String> headers = List.of("ID", "Status", "Queued", "Started", "Finished");
            List<Integer> widths = List.of(5, 16, 27, 27, 27);

            List<List<String>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());

            for (GetExperimentsResponseData exp : response) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(exp.id()));
                row.add(exp.status().name());
                row.add(exp.queuedAt() != null ? formatter.format(exp.queuedAt()) : "-");
                row.add(exp.startedAt() != null ? formatter.format(exp.startedAt()) : "-");
                row.add(exp.finishedAt() != null ? formatter.format(exp.finishedAt()) : "-");
                rows.add(row);
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setAutoRunOnOpen(false);

            configure(tableView);

            getEventloop().onDestroy(
                    getEventloop().keyEvents()
                            .subscribe(event -> {
                                if (tableView.hasFocus() && event.getPlainKey() == 'f' && event.hasCtrl()) {
                                    openFilterForm();
                                }
                            })
            );

            getEventloop().onDestroy(
                    getEventloop().viewEvents(ResizingListView.ResizingListViewOpenSelectedItemEvent.class, tableView)
                            .subscribe(event -> {
                                Object item = event.args().item();
                                if (item instanceof ExperimentOption option) {
                                    handleRowSelection(option);
                                }
                            })
            );

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
        form.addInput("status", "Status (QUEUED, IN_PROGRESS, SUCCESS, PARTIAL_SUCCESS, FAILED)");
        form.addInput("startTime", "Start Time");
        form.addInput("endTime", "End Time");

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
            String startStr = data.get("startTime");
            String endStr = data.get("endTime");

            this.currentAlgorithm = (algorithm != null && !algorithm.isBlank()) ? algorithm.trim() : null;
            this.currentProblem = (problem != null && !problem.isBlank()) ? problem.trim() : null;
            this.currentIndicator = (indicator != null && !indicator.isBlank()) ? indicator.trim() : null;
            if (statusStr != null && !statusStr.isBlank()) {
                this.currentStatus = ExperimentStatus.valueOf(statusStr.trim().toUpperCase());
            } else {
                this.currentStatus = null;
            }
            if (startStr != null && !startStr.isBlank()) {
                this.currentStartTime = OffsetDateTime.parse(startStr.trim());
            } else {
                this.currentStartTime = null;
            }
            if (endStr != null && !endStr.isBlank()) {
                this.currentEndTime = OffsetDateTime.parse(endStr.trim());
            } else {
                this.currentEndTime = null;
            }

            View filteredTableView = build();
            navigate(ScenarioContext.of(filteredTableView, this, () -> {
                if (filteredTableView instanceof SimpleTableView tv) {
                    getTerminalUI().setFocus(tv);
                }
            }, null));

        } catch (IllegalArgumentException | DateTimeParseException e) {
            SimpleMessageView errorView = new SimpleMessageView("Filter Error", "Invalid input format: " + e.getMessage());
            configure(errorView);
            navigate(ScenarioContext.of(errorView, this, () -> getTerminalUI().setFocus(errorView.getContentList()), null));
        }
    }

    private void resetFilters() {
        this.currentAlgorithm = null;
        this.currentProblem = null;
        this.currentIndicator = null;
        this.currentStatus = null;
        this.currentStartTime = null;
        this.currentEndTime = null;
    }

    @Override
    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this, null, this::resetFilters);
    }

    private void handleRowSelection(ExperimentOption option) {
        String rowText = option.name();

        if (rowText.contains("ID") && rowText.contains("Status")) return;
        if (rowText.startsWith("--")) return;
        if (rowText.contains("<<") || rowText.contains(">>")) return;

        try {
            String[] columns = rowText.split("\\|");
            if (columns.length > 0) {
                String idStr = columns[0].trim();
                long experimentId = Long.parseLong(idStr);

                openDetailsScenario(experimentId);
            }
        } catch (NumberFormatException e) {
        }
    }

    private void openDetailsScenario(long experimentId) {
        GetExperimentScenario experimentScenario = new GetExperimentScenario(experimentClient, experimentId);
        experimentScenario.configure(getTerminalUI());
        ScenarioContext context = experimentScenario.buildContext();
        experimentScenario.setNavigationConsumer(this::navigate);
        experimentScenario.setStatusBarConsumer(this::setStatusBar);

        navigate(context);
    }
}
