package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GetExperimentAction implements ExperimentAction {
    @Override
    public String getMenuLabelAndTitle() {
        return "Get experiment by ID";
    }

    @Override
    public void configureInput(InputForm form) {
        form.addInput("id", "Experiment ID");
    }

    @Override
    public View execute(Map<String, String> data, ExperimentClient client) {
        long id = Long.parseLong(data.get("id"));
        GetExperimentResponse response = client.getExperiment(id);

        List<String> headers = List.of("ID", "Status", "Algorithm", "Problem", "Budget", "Error");
        List<Integer> widths = List.of(5, 10, 14, 14, 10, 40);

        List<List<String>> rows = new ArrayList<>();

        for (GetExperimentResponseData exp : response.parts()) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(exp.id()));
            row.add(exp.status().name());
            row.add(exp.algorithm());
            row.add(exp.problem());
            row.add(String.valueOf(exp.budget()));
            row.add(exp.errorMessage() == null ? "None" : exp.errorMessage());
            rows.add(row);
        }
        return new SimpleTableView(headers, rows, widths);

    }
}
