package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;

import java.util.Map;

public interface ExperimentAction {

    String getMenuLabelAndTitle();

    void configureInput(InputForm form);

    View execute(Map<String, String> data, ExperimentClient client);
}
