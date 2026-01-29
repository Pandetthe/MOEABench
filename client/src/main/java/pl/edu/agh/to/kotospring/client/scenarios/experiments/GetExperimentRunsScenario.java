package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentRunsResponse;
import org.springframework.shell.component.view.control.View;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.*;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentRunsResponseData;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pl.edu.agh.to.kotospring.client.services.ExperimentErrorHandler;
import java.nio.charset.StandardCharsets;

@ScenarioComponent(name = "Get all runs", type = ScenarioType.EXPERIMENT_MENU)
public class GetExperimentRunsScenario extends Scenario {
    private final ExperimentClient experimentClient;
    private final ExperimentErrorHandler errorHandler;

    private String currentAlgorithm = null;
    private String currentProblem = null;
    private String currentIndicator = null;
    private ExperimentRunStatus currentStatus = null;
    private OffsetDateTime currentStartTime = null;
    private OffsetDateTime currentEndTime = null;

    private int currentPage = 0;
    private final int pageSize = 20;
    private int totalPages = 1;

    public GetExperimentRunsScenario(ExperimentClient experimentClient, ExperimentErrorHandler errorHandler) {
        this.experimentClient = experimentClient;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void onStart() {
        setStatusBar(List.of("CTRL-F Search", "[ Prev Page", "] Next Page"));
    }

    @Override
    public View build() {
        try {
            GetExperimentRunsResponse page = experimentClient.getExperimentRuns(
                    currentAlgorithm,
                    currentProblem,
                    currentIndicator,
                    currentStatus,
                    currentStartTime,
                    currentEndTime,
                    currentPage,
                    pageSize);

            var metadata = page.metadata();
            this.totalPages = (int) metadata.totalPages();

            List<String> headers = List.of("Experiment ID", "Run ID", "Status", "Started", "Finished");
            List<Integer> widths = List.of(15, 10, 16, 27, 27);

            List<List<String>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());

            for (GetExperimentRunsResponseData exp : page.content()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(exp.experimentId()));
                row.add(String.valueOf(exp.runNo()));
                row.add(exp.status().name());
                row.add(exp.startedAt() != null ? formatter.format(exp.startedAt()) : "-");
                row.add(exp.finishedAt() != null ? formatter.format(exp.finishedAt()) : "-");
                rows.add(row);
            }

            SimpleTableView tableView = new SimpleTableView(headers, rows, widths);
            tableView.setTitle(String.format("Runs (Page %d/%d) - Press Enter to Add to Group", currentPage + 1, Math.max(1, totalPages)));
            tableView.setAutoRunOnOpen(false);

            configure(tableView);

            ScenarioBindings bindings = new ScenarioBindings(getEventloop());
            bindings.onCtrlKeyWhenFocused(tableView, 'f', this::openFilterForm);
            bindings.onOpenSelectedItem(tableView, pl.edu.agh.to.kotospring.client.models.MenuOption.class, this::handleRowSelection);

            // Row paging bindings
            getEventloop().onDestroy(
                    getEventloop().keyEvents()
                            .subscribe(event -> {
                                if (tableView.hasFocus()) {
                                    if (event.getPlainKey() == '[') {
                                        prevPage();
                                    } else if (event.getPlainKey() == ']') {
                                        nextPage();
                                    }
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
        form.addInput("status", "Status (QUEUED, IN_PROGRESS, SUCCESS, PARTIAL_SUCCESS, FAILED)");
        form.addInput("startTime", "Start Time");
        form.addInput("endTime", "End Time");

        form.setSubmitAction("Apply Filters", this::handleFilterSubmit);
        configure(form);
        form.focusFirstInput();
        navigate(createContext(form));
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
                this.currentStatus = ExperimentRunStatus.valueOf(statusStr.trim().toUpperCase());
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

            this.currentPage = 0;
            View filteredTableView = build();
            navigate(createContext(filteredTableView, () -> {
                if (filteredTableView instanceof SimpleTableView tv) {
                    getTerminalUI().setFocus(tv);
                }
            }));

        } catch (IllegalArgumentException | DateTimeParseException e) {
            SimpleMessageView errorView = new SimpleMessageView("Filter Error",
                    "Invalid input format: " + e.getMessage());
            configure(errorView);
            navigate(createContext(errorView, () -> getTerminalUI().setFocus(errorView)));
        }
    }

    private void resetFilters() {
        this.currentAlgorithm = null;
        this.currentProblem = null;
        this.currentIndicator = null;
        this.currentStatus = null;
        this.currentStartTime = null;
        this.currentEndTime = null;
        this.currentPage = 0;
    }

    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            refresh();
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            refresh();
        }
    }

    private void refresh() {
        View view = build();
        replace(createContext(view, () -> {
            if (view instanceof SimpleTableView tv) {
                getTerminalUI().setFocus(tv);
            }
        }));
    }

    private void handleRowSelection(pl.edu.agh.to.kotospring.client.models.MenuOption option) {
        String rowText = option.name();

        if (rowText.contains("Experiment ID") && rowText.contains("Run ID")) return;
        if (rowText.startsWith("----")) return;

        try {
            String[] columns = rowText.split("\\|");
            String expIdStr = null;
            String runIdStr = null;

            int found = 0;
            for (String col : columns) {
                if (!col.trim().isEmpty()) {
                    if (found == 0) expIdStr = col.trim();
                    else if (found == 1) runIdStr = col.trim();
                    found++;
                    if (found >= 2) break;
                }
            }

            if (expIdStr != null && runIdStr != null) {
                long experimentId = Long.parseLong(expIdStr);
                long runNo = Long.parseLong(runIdStr);
                openAddToGroupForm(experimentId, runNo);
            }
        } catch (NumberFormatException e) {
            SimpleMessageView errorView = new SimpleMessageView("Error", "Invalid number format.");
            configure(errorView);
            navigate(createContext(errorView));
        }
    }

    private void openAddToGroupForm(long experimentId, long runNo) {
        InputForm form = new InputForm(getTerminalUI(), "Add Run to Group");
        form.addInput("groupId", "Group ID");
        form.setSubmitAction("Add", (data) -> handleAddRunToGroup(data, experimentId, runNo));
        configure(form);
        form.focusFirstInput();
        navigate(createContext(form));
    }

    private void handleAddRunToGroup(Map<String, String> data, long experimentId, long runNo) {
        try {
            long groupId = Long.parseLong(data.get("groupId").trim());
            experimentClient.addRunToExperimentGroup(groupId, experimentId, runNo);
            
            SimpleMessageView successView = new SimpleMessageView("Success", 
                String.format("Run %d (Exp %d) added to Group %d", runNo, experimentId, groupId));
            configure(successView);
            navigate(createContext(successView, () -> {
                View runsView = build();
                replace(createContext(runsView, () -> {
                     if (runsView instanceof SimpleTableView tv) getTerminalUI().setFocus(tv);
                }));
            }));

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            View resultView = errorHandler.httpErrorView(e.getStatusCode().value(),
                    e.getStatusText(),
                    body);
            configure(resultView);
            navigate(createContext(resultView, () -> getTerminalUI().setFocus(resultView)));
        } catch (NumberFormatException e) {
            SimpleMessageView errorView = new SimpleMessageView("Error", "Invalid Group ID format.");
            configure(errorView);
            navigate(createContext(errorView));
        } catch (Exception e) {
             SimpleMessageView errorView = new SimpleMessageView("Error", "Failed to add run: " + e.getMessage());
             configure(errorView);
             navigate(createContext(errorView));
        }
    }

    @Override
    public ScenarioContext buildContext() {
        return createContext(build(), null, this::resetFilters);
    }
}
