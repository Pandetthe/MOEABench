package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentPartResultResponse;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.GetExperimentResultResponse;

import java.util.Map;
import java.util.Optional;

@Component
public class GetExperimentResultAction implements ExperimentAction {
    @Override
    public String getMenuLabelAndTitle() {
        return "Check experiment (part) result";
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
            GetExperimentResultResponse response = client.getExperimentResult(id);
        }
        else {
            GetExperimentPartResultResponse response = client.getExperimentPartResult(id, partId.get());
        }

        return new SimpleMessageView("Success",
                "TODO: some way of showing results");
    }
}
