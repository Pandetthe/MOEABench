package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.ListView;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;

import java.util.List;

@ScenarioComponent(name = "Problems")
public class ProblemScenario extends Scenario {
    private final RegistryClient registryClient;

    public ProblemScenario(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public View build() {
        ListView<String> box = new ListView<>();
        configure(box);
        List<String> problems = registryClient.getProblems();
        box.setItems(problems);
        box.setTitle("Problems");
        box.setShowBorder(true);
        box.setTitleAlign(HorizontalAlign.CENTER);
        return box;
    }
}