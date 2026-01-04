package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentsResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GetExperimentsAction implements ExperimentAction {
    @Override
    public String getMenuLabelAndTitle() {
        return "Get list of experiments";
    }

    @Override
    public void configureInput(InputForm form) {
    }

    @Override
    public View execute(Map<String, String> data, ExperimentClient client) {
        GetExperimentsResponse response = client.getExperiments();
        List<String> headers = List.of("ID", "Status", "Queued", "Started", "Finished");
        List<Integer> widths = List.of(5, 10, 18, 18, 18);

        List<List<String>> rows = new ArrayList<>();

        for (GetExperimentsResponseData exp : response) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(exp.id()));
            row.add(exp.status().name());
            row.add(exp.queuedAt() != null ? String.valueOf(exp.queuedAt()) : "-");
            row.add(exp.startedAt() != null ? String.valueOf(exp.startedAt()) : "-");
            row.add(exp.finishedAt() != null ? String.valueOf(exp.finishedAt()) : "-");
            rows.add(row);
        }

        return new SimpleTableView(headers, rows, widths);
    }
}
