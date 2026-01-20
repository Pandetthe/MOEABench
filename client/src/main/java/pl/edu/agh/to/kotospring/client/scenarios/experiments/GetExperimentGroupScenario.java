package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.shell.component.view.control.View;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;

@ScenarioComponent(name = "Get experiment group", type = ScenarioType.OTHER)
public class GetExperimentGroupScenario extends Scenario {

    private long groupId;
    private final ExperimentClient experimentClient;

    public GetExperimentGroupScenario(ExperimentClient experimentClient) {
        this.experimentClient = experimentClient;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    @Override
    public View build() {
        return null;
    }
}
