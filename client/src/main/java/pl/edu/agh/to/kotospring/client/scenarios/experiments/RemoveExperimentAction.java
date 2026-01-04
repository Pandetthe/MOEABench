package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;

import java.util.Map;

@Component
public class RemoveExperimentAction implements ExperimentAction {

    @Override
    public String getMenuLabelAndTitle() {
        return "Delete experiment by ID";
    }

    @Override
    public void configureInput(InputForm form) {
        form.addInput("id", "Experiment ID");
    }

    @Override
    public View execute(Map<String, String> data, ExperimentClient client) {
        long id = Long.parseLong(data.get("id"));
        client.deleteExperiment(id);

        return new SimpleMessageView(
                "Success",
                "Experiment with ID " + id + " has been deleted successfully."
        );
    }
}
