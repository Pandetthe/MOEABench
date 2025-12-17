package pl.edu.agh.to.kotospring.client.shell;

import org.jline.terminal.Terminal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ShellComponent
public class ExperimentCommands {

    private final ExperimentClient experimentApi;
    private final Terminal terminal;

    public ExperimentCommands(ExperimentClient experimentApi, Terminal terminal) {
        this.experimentApi = experimentApi;
        this.terminal = terminal;
    }

    @ShellMethod(value = "Run experiment", key = "experiment-run")
    public String experimentRun(
            @ShellOption String algorithms,
            @ShellOption String problems,
            @ShellOption String indicators,
            @ShellOption int budget
    ) {
        List<String> algorithmList = List.of(algorithms.split(","));
        List<String> problemList = List.of(problems.split(","));
        Set<String> indicatorSet = Set.of(indicators.split(","));

        CreateExperimentRequest request = new CreateExperimentRequest();

        for (String algorithm : algorithmList) {
            for (String problem : problemList) {

                Map<String, Object> algorithmParameters = Map.of();

                CreateExperimentRequestData data =
                        new CreateExperimentRequestData(
                                problem,
                                algorithm,
                                algorithmParameters,
                                indicatorSet,
                                budget
                        );

                request.add(data);
            }
        }

        CreateExperimentResponse response = experimentApi.createExperiment(request);
        return "Experiment created successfully with ID: " + response.id();
    }

    @ShellMethod(value = "List experiments", key = "experiment-list")
    public Table experimentList() {
        GetExperimentsResponse response = experimentApi.getExperiments();

        String[] headers = {"ID", "Status", "Queued At", "Started At", "Finished At"};
        List<String[]> rows = new ArrayList<>();

        for (GetExperimentsResponseData exp : response) {
            rows.add(new String[]{
                    String.valueOf(exp.id()),
                    String.valueOf(exp.status()),
                    String.valueOf(exp.queuedAt()),
                    String.valueOf(exp.startedAt()),
                    String.valueOf(exp.finishedAt())
            });
        }

        return createTable(headers, rows);
    }

    @ShellMethod(value = "Get experiment status", key = "experiment-status")
    public String experimentStatus(@ShellOption long id) {
        GetExperimentStatusResponse response = experimentApi.getExperimentStatus(id);
        return "Status: " + response.status();
    }

    @ShellMethod(value = "Show experiment part status", key = "experiment-part-status")
    public Table experimentPartStatus(
            @ShellOption long id,
            @ShellOption long part
    ) {
        GetExperimentPartStatusResponse response = experimentApi.getExperimentPartStatus(id, part);

        String[] headers = {"Status", "Error Message"};
        String[] row = new String[]{
                String.valueOf(response.status()),
                response.errorMessage() == null ? "" : response.errorMessage()
        };
        List<String[]> rows = new ArrayList<>();
        rows.add(row);

        return createTable(headers, rows);
    }

    @ShellMethod(value = "Show full experiment", key = "experiment-show")
    public Table experimentShow(@ShellOption long id) {
        GetExperimentResponse response = experimentApi.getExperiment(id);

        System.out.println("Experiment Details:");
        System.out.println("Status: " + response.status());
        System.out.println("Queued: " + response.queuedAt());
        System.out.println("Started: " + response.startedAt());
        System.out.println("Finished: " + response.finishedAt());
        System.out.println("Parts:");

        String[] headers = {"ID", "Status", "Algorithm", "Problem", "Budget", "Error"};
        List<String[]> rows = new ArrayList<>();

        for (GetExperimentResponseData part : response.parts()) {
            rows.add(new String[]{
                    String.valueOf(part.id()),
                    String.valueOf(part.status()),
                    part.algorithm(),
                    part.problem(),
                    String.valueOf(part.budget()),
                    part.errorMessage() == null ? "" : part.errorMessage()
            });
        }

        return createTable(headers, rows);
    }

    @ShellMethod(value = "Show experiment results", key = "experiment-results")
    public String experimentResults(@ShellOption long id) {
        GetExperimentResultResponse response = experimentApi.getExperimentResult(id);
        StringBuilder sb = new StringBuilder();

        for (GetExperimentResultResponseData partData : response) {
            sb.append(renderFullPartResult(partData.id(), partData.indicatorsValues(), partData.result()));
            sb.append("\n").append("-".repeat(getTerminalWidth())).append("\n"); // Separator between parts
        }
        return sb.toString();
    }

    @ShellMethod(value = "Show experiment part results", key = "experiment-part-results")
    public String experimentPartResults(
            @ShellOption long id,
            @ShellOption long part
    ) {
        GetExperimentPartResultResponse response = experimentApi.getExperimentPartResult(id, part);
        return renderFullPartResult(part, response.indicatorsValues(), response.result());
    }

    @ShellMethod(value = "Watch experiment status", key = "experiment-watch")
    public void experimentWatch(@ShellOption long id) throws InterruptedException {
        while (true) {
            var response = experimentApi.getExperimentStatus(id);
            System.out.print("\rCurrent Status: " + response.status() + "   ");

            switch (response.status()) {
                case SUCCESS, PARTIAL_SUCCESS, FAILED -> {
                    System.out.println();
                    return;
                }
            }
            //noinspection BusyWait
            Thread.sleep(1000);
        }
    }

    @ShellMethod(value = "Delete experiment", key = "experiment-delete")
    public String experimentDelete(@ShellOption long id) {
        experimentApi.deleteExperiment(id);
        return "Experiment " + id + " Deleted";
    }

    private String renderFullPartResult(long partId, Map<String, Double> indicators, List<AlgorithmResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Part ID: ").append(partId).append("\n\n");
        sb.append("Indicators:\n");
        if (indicators != null && !indicators.isEmpty()) {
            String[] headers = {"Indicator", "Value"};
            List<String[]> rows = indicators.entrySet().stream()
                    .map(e -> new String[]{e.getKey(), String.format("%.4f", e.getValue())})
                    .collect(Collectors.toList());
            sb.append(createTable(headers, rows).render(getTerminalWidth()));
        } else {
            sb.append("No indicators available.\n");
        }
        sb.append("\n");

        sb.append("Solutions:\n");
        if (results != null && !results.isEmpty()) {
            AlgorithmResult first = results.get(0);
            List<String> headersList = new ArrayList<>();

            if (first.variables() != null) headersList.addAll(first.variables().keySet());
            if (first.objectives() != null) headersList.addAll(first.objectives().keySet());
            if (first.constraints() != null) headersList.addAll(first.constraints().keySet());

            String[] headers = headersList.toArray(new String[0]);
            List<String[]> rows = new ArrayList<>();

            for (AlgorithmResult res : results) {
                List<String> rowData = new ArrayList<>();

                if (first.variables() != null) {
                    for (String key : first.variables().keySet()) {
                        rowData.add(String.valueOf(res.variables().get(key)));
                    }
                }

                if (first.objectives() != null) {
                    for (String key : first.objectives().keySet()) {
                        Double val = res.objectives().get(key);
                        rowData.add(val != null ? String.format("%.4f", val) : "-");
                    }
                }
                if (first.constraints() != null) {
                    for (String key : first.constraints().keySet()) {
                        Double val = res.constraints().get(key);
                        rowData.add(val != null ? String.format("%.4f", val) : "-");
                    }
                }
                rows.add(rowData.toArray(new String[0]));
            }

            sb.append(createTable(headers, rows).render(getTerminalWidth()));
        } else {
            sb.append("No solutions found.\n");
        }

        return sb.toString();
    }

    private int getTerminalWidth() {
        int width = terminal.getWidth();
        return (width <= 0) ? 80 : width;
    }

    private Table createTable(String[] headers, List<String[]> data) {
        String[][] tableData = new String[data.size() + 1][headers.length];
        tableData[0] = headers;

        for (int i = 0; i < data.size(); i++) {
            tableData[i + 1] = data.get(i);
        }

        return new TableBuilder(new ArrayTableModel(tableData))
                .addFullBorder(BorderStyle.fancy_light)
                .build();
    }
}
