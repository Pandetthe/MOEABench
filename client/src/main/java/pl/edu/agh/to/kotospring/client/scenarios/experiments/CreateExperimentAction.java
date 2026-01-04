package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import org.springframework.stereotype.Component;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CreateExperimentAction implements ExperimentAction {

    @Override
    public String getMenuLabelAndTitle() {
        return "Create new Experiment";
    }


    @Override
    public void configureInput(InputForm form) {
        form.addInput("problems", "Problems");
        form.addInput("algorithms", "Algorithms");
        form.addInput("indicators", "Indicators");
        form.addInput("budget", "Budget");
    }

    @Override
    public View execute(Map<String, String> data, ExperimentClient client) {
        List<String> algorithmList = List.of(data.get("algorithms").split(","));
        List<String> problemList = List.of(data.get("problems").split(","));
        Set<String> indicatorSet = Set.of(data.get("indicators").split(","));
        int budget =Integer.parseInt(data.get("budget"));

        CreateExperimentRequest request = new CreateExperimentRequest();
        for (String algorithm : algorithmList) {
            for (String problem : problemList) {

                Map<String, Object> algorithmParameters = Map.of();

                CreateExperimentRequestData requestData =
                        new CreateExperimentRequestData(
                                problem,
                                algorithm,
                                algorithmParameters,
                                indicatorSet,
                                budget
                        );

                request.add(requestData);
            }
        }
        CreateExperimentResponse response = client.createExperiment(request);
        return new SimpleMessageView(
                "Success",
                "Experiment successfully created with ID: " + response.id()
        );
    }
}
