package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.context.annotation.Scope;
import org.springframework.shell.component.view.control.View;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.AggregateTableColumn;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentAggregateResponse;
import pl.edu.agh.to.kotospring.client.utils.NumberFormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Get experiment group aggregate", type = ScenarioType.OTHER)
@Scope("prototype")
public class GetExperimentGroupAggregateScenario extends Scenario {

    private final ExperimentClient client;
    private long groupId;
    private static final int FIXED_COLUMNS_NUMBER = 4;
    private static final int MAX_COLUMNS_NUMBER = 5;
    private static final List<AggregateTableColumn> AGGREGATE_TABLE_COLUMNS = List.of(
            new AggregateTableColumn("Algorithm", 20),
            new AggregateTableColumn("Problem", 20),
            new AggregateTableColumn("Budget", 10),
            new AggregateTableColumn("Indicator", 20),
            new AggregateTableColumn("Min", 15),
            new AggregateTableColumn("Max", 15),
            new AggregateTableColumn("Mean", 15),
            new AggregateTableColumn("Median", 15),
            new AggregateTableColumn("SD", 15));

    public GetExperimentGroupAggregateScenario(ExperimentClient client) {
        this.client = client;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    @Override
    public View build() {
        try {
            GetExperimentAggregateResponse resp = client.getExperimentGroupAggregate(groupId);
            return buildAggregateView(resp);
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            return new SimpleMessageView("Connection Error", "Service unavailable. Could not connect to the server.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                return new SimpleMessageView("Server Error", "Internal Server Error (" + e.getStatusCode() + "). Please try again later.");
            }
            return new SimpleMessageView("Error", "Server returned error: " + e.getStatusCode() + " for Group ID: " + groupId);
        } catch (Exception e) {
            return new SimpleMessageView("Error", "Could not fetch aggregate: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private View buildAggregateView(GetExperimentAggregateResponse resp) {
        if (resp == null || resp.isEmpty()) {
            return new SimpleMessageView("Aggregations", "No aggregated data available.");
        }

        List<String> headers = AGGREGATE_TABLE_COLUMNS.stream()
                .map(AggregateTableColumn::header)
                .toList();

        List<List<String>> rows = new ArrayList<>();

        for (var data : resp) {
            for (var entry : data.indicators().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                List<String> row = new ArrayList<>();
                row.add(data.algorithm());
                row.add(data.problem());
                row.add(String.valueOf(data.budget()));
                row.add(entry.getKey());
                var ind = entry.getValue();
                row.add(NumberFormatUtils.formatDoubleShort(ind.min()));
                row.add(NumberFormatUtils.formatDoubleShort(ind.max()));
                row.add(NumberFormatUtils.formatDoubleShort(ind.mean()));
                row.add(NumberFormatUtils.formatDoubleShort(ind.median()));
                row.add(NumberFormatUtils.formatDoubleShort(ind.standardDeviation()));
                rows.add(row);
            }
        }

        List<Integer> widths = AGGREGATE_TABLE_COLUMNS.stream()
                .map(AggregateTableColumn::width)
                .toList();

        SimpleTableView table = new SimpleTableView(headers, rows, widths);
        table.setTitle("Aggregated Results for Experiment Group ID: " + groupId);
        table.setShowBorder(true);
        table.setEnableWrapping(false);
        if (AGGREGATE_TABLE_COLUMNS.size() > MAX_COLUMNS_NUMBER) {
            table.enableColumnPaging(FIXED_COLUMNS_NUMBER, MAX_COLUMNS_NUMBER);
        }
        configure(table);
        return table;
    }

}
