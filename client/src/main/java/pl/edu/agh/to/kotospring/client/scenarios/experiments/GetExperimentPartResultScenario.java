package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentPartResultResponse;

import java.util.*;
import java.util.stream.Collectors;

@ScenarioComponent(name = "Get experiment part result", type = ScenarioType.OTHER)
public class GetExperimentPartResultScenario extends Scenario {

    private ExperimentClient client;
    private long experimentId;
    private long runNo;
    private long partId;

    public GetExperimentPartResultScenario() {
    }

    public GetExperimentPartResultScenario(ExperimentClient client, long experimentId, long runNo, long partId) {
        this.client = client;
        this.experimentId = experimentId;
        this.runNo = runNo;
        this.partId = partId;
    }

    @Override
    public View build() {
        try {
            GetExperimentPartResultResponse resp = client.getExperimentPartResult(experimentId, runNo, partId);

            return partResultAsOneTableIndicatorsLast(partId, resp);

        } catch (Exception e) {
            return new SimpleMessageView("Error", "Could not fetch result: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private View partResultAsOneTableIndicatorsLast(long partId, GetExperimentPartResultResponse resp) {
        List<AlgorithmResult> results = resp.result();
        AlgorithmResult first = (results == null || results.isEmpty()) ? null : results.get(0);

        List<String> varKeys = first == null ? List.of() : keys(first.variables());
        List<String> objKeys = first == null ? List.of() : keys(first.objectives());
        List<String> conKeys = first == null ? List.of() : keys(first.constraints());

        List<String> headers = new ArrayList<>();
        headers.add("Part ID");
        headers.addAll(varKeys);
        headers.addAll(objKeys);
        headers.addAll(conKeys);
        headers.add("Indicators");

        // fallback gdy tylko jeden nagłówek (Part ID, Indicators -> dodaj Solutions)
        if (headers.size() == 2 && "Indicators".equals(headers.get(1))) {
            headers.add(1, "Solutions");
        }

        String indicatorsCompact = compactIndicators(resp.indicatorsValues());

        List<List<String>> rows = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(partId));
            row.add("No solutions");
            int numericCols = varKeys.size() + objKeys.size() + conKeys.size();
            for (int i = 1; i < numericCols; i++)
                row.add("-");
            row.add(indicatorsCompact);
            rows.add(row);
        } else {
            boolean fallbackToString = headers.contains("Solutions");
            for (AlgorithmResult r : results) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(partId));

                if (fallbackToString) {
                    row.add(String.valueOf(r));
                    int numericCols = varKeys.size() + objKeys.size() + conKeys.size();
                    for (int i = 1; i < numericCols; i++)
                        row.add("-");
                } else {
                    for (String k : varKeys)
                        row.add(formatAny(r.variables() == null ? null : r.variables().get(k)));
                    for (String k : objKeys)
                        row.add(formatDouble(r.objectives() == null ? null : r.objectives().get(k)));
                    for (String k : conKeys)
                        row.add(formatDouble(r.constraints() == null ? null : r.constraints().get(k)));
                }

                row.add(indicatorsCompact);
                rows.add(row);
            }
        }

        List<Integer> colWidths = widths(headers, rows);
        SimpleTableView table = new SimpleTableView(headers, rows, colWidths);

        table.enableColumnPaging(1, 7); // Możesz ustawić jakie kolumny mają podlegać paginacji
        table.setShowFullNumericCells(true);
        table.setIndicatorsFullOnLastPage(true);
        table.setIndicatorsLastPageWidth(30);

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
        if (v == null)
            return "-";
        return formatDoubleShort(v);
    }

    private static String formatDoubleShort(Double v) {
        if (v == null)
            return "-";
        double x = v;
        double ax = Math.abs(x);

        if (ax != 0.0 && (ax < 1e-3 || ax >= 1e4)) {
            return String.format(Locale.ROOT, "%.3e", x)
                    .replace("e+0", "e+")
                    .replace("e-0", "e-");
        }

        String s = String.format(Locale.ROOT, "%.4f", x);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.isEmpty() ? "0" : s;
    }

    private static String compactIndicators(Map<String, Double> indicators) {
        if (indicators == null || indicators.isEmpty())
            return "-";
        return indicators.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + formatDoubleShort(e.getValue()))
                .collect(Collectors.joining("; "));
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
            String h = headers.get(i);

            int minW = Math.max(5, h == null ? 5 : h.length());
            int maxW;

            if ("Part ID".equals(h))
                maxW = 6;
            else if ("Indicators".equals(h))
                maxW = 18;
            else if ("Solutions".equals(h))
                maxW = 30;
            else
                maxW = 12;

            out.add(Math.min(maxW, Math.max(minW, w[i])));
        }
        return out;
    }

    @Override
    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this, null, null);
    }
}