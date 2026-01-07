package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.context.annotation.Scope;
import org.springframework.shell.component.view.control.View;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentAggregateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ScenarioComponent(name = "Get experiment aggregate", type = ScenarioType.OTHER)
@Scope("prototype")
public class GetExperimentAggregateScenario extends Scenario {

    private final ExperimentClient client;
    private long experimentId;

    public GetExperimentAggregateScenario(ExperimentClient client) {
        this.client = client;
    }

    public void setExperimentId(long experimentId) {
        this.experimentId = experimentId;
    }

    @Override
    public View build() {
        try {
            GetExperimentAggregateResponse resp = client.getExperimentAggregate(experimentId);
            return buildAggregateView(resp);
        } catch (Exception e) {
            return new SimpleMessageView("Error", "Could not fetch aggregate: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private View buildAggregateView(GetExperimentAggregateResponse resp) {
        if (resp == null || resp.isEmpty()) {
            return new SimpleMessageView("Aggregations", "No aggregated data available.");
        }

        List<String> headers = new ArrayList<>(
                List.of("Algorithm", "Problem", "Budget", "Indicator", "Min", "Max", "Mean", "Median", "SD"));
        List<List<String>> rows = new ArrayList<>();

        for (var data : resp) {
            for (var entry : data.indicators().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                List<String> row = new ArrayList<>();
                row.add(data.algorithm());
                row.add(data.problem());
                row.add(String.valueOf(data.budget()));
                row.add(entry.getKey());
                var ind = entry.getValue();
                row.add(formatDoubleShort(ind.min()));
                row.add(formatDoubleShort(ind.max()));
                row.add(formatDoubleShort(ind.mean()));
                row.add(formatDoubleShort(ind.median()));
                row.add(formatDoubleShort(ind.standardDeviation()));
                rows.add(row);
            }
        }

        List<Integer> widths = List.of(20, 20, 10, 20, 15, 15, 15, 15, 15);
        SimpleTableView table = new SimpleTableView(headers, rows, widths);
        table.setTitle("Aggregated Results for Experiment ID: " + experimentId);
        table.setShowBorder(true);
        table.setEnableWrapping(false);
        if (headers.size() > 5) {
            table.enableColumnPaging(4, 5);
        }
        configure(table);
        return table;
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
}
