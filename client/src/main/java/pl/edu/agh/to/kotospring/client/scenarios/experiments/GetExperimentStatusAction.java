package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentPartStatusResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentStatusResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GetExperimentStatusAction implements ExperimentAction {


    @Override
    public String getMenuLabelAndTitle() {
        return "Check experiment (part) status";
    }

    @Override
    public void configureInput(InputForm form) {
        form.addInput("id", "ID");
        form.addInput("partId", "Part ID (optional, leave empty to check whole experiment)");
    }

    @Override
    public View execute(Map<String, String> data, ExperimentClient client) {
        long id = Long.parseLong(data.get("id"));
        Optional<Long> partId = data.get("partId").isBlank() ? Optional.empty() : Optional.of(Long.parseLong(data.get("partId")));

        if (partId.isEmpty()) {
            GetExperimentStatusResponse response = client.getExperimentStatus(id);
            System.out.println("Experiment status: " + response.status());

            List<String> headers = List.of("ID", "Status");
            List<Integer> widths = List.of(8, 20);

            List<List<String>> rows = new ArrayList<>();
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(id));
            row.add(String.valueOf(response.status()));
            rows.add(row);

            return new SimpleTableView(headers, rows, widths);


        }
        else {
            GetExperimentPartStatusResponse response = client.getExperimentPartStatus(id, partId.get());
            System.out.println("Experiment part status: " + response.status());

            List<String> headers = List.of("ID", "Part ID", "Status", "Error Message");
            List<Integer> widths = List.of(8, 10, 20, 30);

            List<List<String>> rows = new ArrayList<>();
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(id));
            row.add(String.valueOf(partId.get()));
            row.add(String.valueOf(response.status()));
            row.add(response.errorMessage() != null ? response.errorMessage() : "N/A");
            rows.add(row);

            return new SimpleTableView(headers, rows, widths);
        }
    }
}
