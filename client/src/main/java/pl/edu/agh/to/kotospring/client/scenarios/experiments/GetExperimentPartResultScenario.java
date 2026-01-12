package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.context.annotation.Scope;
import org.springframework.shell.component.view.control.View;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.FixedGridView;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentPartResultResponse;

import java.util.*;

@ScenarioComponent(name = "Get experiment part result", type = ScenarioType.OTHER)
@Scope("prototype")
public class GetExperimentPartResultScenario extends Scenario {

    private final ExperimentClient client;
    private long experimentId;
    private long runNo;
    private long partId;
    private static final int FIXED_COLUMNS_NUMBER = 4;
    private static final int MAX_COLUMNS_NUMBER = 5;

    public GetExperimentPartResultScenario(ExperimentClient client) {
        this.client = client;
    }

    public void setExperimentId(long experimentId) {
        this.experimentId = experimentId;
    }

    public void setRunNo(long runNo) {
        this.runNo = runNo;
    }

    public void setPartId(long partId) {
        this.partId = partId;
    }

    @Override
    public void onStart() {
        setStatusBar(List.of("TAB Change selection"));
    }

    @Override
    public View build() {
        try {
            GetExperimentPartResultResponse resp = client.getExperimentPartResult(experimentId, runNo, partId);
            return buildGridView(resp);
        } catch (Exception e) {
            return new SimpleMessageView("Error", "Could not fetch result: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private View buildGridView(GetExperimentPartResultResponse resp) {
        FixedGridView grid = new FixedGridView();
        configure(grid);
        grid.setRowSize(10, 0);
        grid.setColumnSize(0);
        grid.setShowBorders(true);
        grid.setTitle("Results for Experiment ID: " + experimentId + ", Run No: " + runNo + ", Part ID: " + partId);
        View indicatorsView = buildIndicatorsView(resp.indicatorsValues());
        grid.addItem(indicatorsView, 0, 0, 1, 1, 0, 0);
        View solutionsTable = buildSolutionsTable(resp.result());
        grid.addItem(solutionsTable, 1, 0, 1, 1, 0, 0);

        return grid;
    }

    private View buildIndicatorsView(Map<String, Double> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return new SimpleMessageView("Indicators", "No indicator values available.");
        }

        List<String> headers = List.of("Indicator", "Value");
        List<List<String>> rows = new ArrayList<>();
        for (var entry : indicators.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            rows.add(List.of(entry.getKey(), formatDoubleShort(entry.getValue())));
        }

        SimpleTableView table = new SimpleTableView(headers, rows, List.of(20, 20));
        table.setTitle("Statistical Indicators");
        table.setShowBorder(true);
        table.setEnableWrapping(false);
        configure(table);
        return table;
    }

    private View buildSolutionsTable(List<AlgorithmResult> results) {
        AlgorithmResult first = (results == null || results.isEmpty()) ? null : results.get(0);

        List<String> varKeys = first == null ? List.of() : keys(first.variables());
        List<String> objKeys = first == null ? List.of() : keys(first.objectives());
        List<String> conKeys = first == null ? List.of() : keys(first.constraints());

        List<String> headers = new ArrayList<>();
        headers.addAll(varKeys);
        headers.addAll(objKeys);
        headers.addAll(conKeys);

        if (headers.isEmpty()) {
            headers.add("Solutions");
        }

        List<List<String>> rows = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            List<String> row = new ArrayList<>();
            row.add("No solutions found");
            rows.add(row);
        } else {
            boolean fallbackToString = headers.contains("Solutions");
            for (AlgorithmResult r : results) {
                List<String> row = new ArrayList<>();
                if (fallbackToString) {
                    row.add(String.valueOf(r));
                } else {
                    for (String k : varKeys)
                        row.add(formatAny(r.variables() == null ? null : r.variables().get(k)));
                    for (String k : objKeys)
                        row.add(formatDouble(r.objectives() == null ? null : r.objectives().get(k)));
                    for (String k : conKeys)
                        row.add(formatDouble(r.constraints() == null ? null : r.constraints().get(k)));
                }
                rows.add(row);
            }
        }

        List<Integer> colWidths = widths(headers, rows);
        SimpleTableView table = new SimpleTableView(headers, rows, colWidths);
        table.setTitle("Solutions (Use arrows to page columns)");

        if (headers.size() > MAX_COLUMNS_NUMBER) {
            table.enableColumnPaging(FIXED_COLUMNS_NUMBER, MAX_COLUMNS_NUMBER);
        }

        table.setShowFullNumericCells(true);
        table.setShowBorder(true);
        table.setEnableWrapping(false);
        configure(table);
        return table;
    }

    private static List<String> keys(Map<String, ?> map) {
        return (map == null || map.isEmpty()) ? List.of() : new ArrayList<>(map.keySet());
    }

    private static String formatAny(Object v) {
        if (v == null)
            return "-";
        if (v instanceof Double d)
            return formatDoubleShort(d);
        if (v instanceof Float f)
            return formatDoubleShort(f.doubleValue());
        if (v instanceof Number n)
            return formatDoubleShort(n.doubleValue());
        return String.valueOf(v);
    }

    private static String formatDouble(Double v) {
        return formatDoubleShort(v);
    }

    private static String formatDoubleShort(Double v) {
        if (v == null)
            return "-";
        double x = v;
        double ax = Math.abs(x);
        if (ax != 0.0 && (ax < 1e-3 || ax >= 1e4)) {
            return String.format(Locale.ROOT, "%.3e", x).replace("e+0", "e+").replace("e-0", "e-");
        }
        String s = String.format(Locale.ROOT, "%.4f", x);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.isEmpty() ? "0" : s;
    }

    private static List<Integer> widths(List<String> headers, List<List<String>> rows) {
        int cols = headers.size();
        int[] w = new int[cols];
        for (int i = 0; i < cols; i++)
            w[i] = headers.get(i) == null ? 0 : headers.get(i).length();
        for (List<String> r : rows) {
            for (int i = 0; i < cols; i++) {
                String cell = (r != null && i < r.size() && r.get(i) != null) ? r.get(i) : "";
                w[i] = Math.max(w[i], cell.length());
            }
        }
        List<Integer> out = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            out.add(Math.min(30, Math.max(15, w[i])));
        }
        return out;
    }
}