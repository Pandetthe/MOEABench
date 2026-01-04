package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.BoxView;
import org.springframework.shell.component.view.control.ListView;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;

import java.util.List;

@ScenarioComponent(name = "Indicators")
public class IndicatorScenario extends Scenario {

    private final RegistryClient registryClient;

    public IndicatorScenario(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public View build() {
        ListView<String> box = new ListView<>();
        configure(box);
        List<String> indicators = registryClient.getIndicators();
        box.setItems(indicators);
        box.setTitle("Indicators");
        box.setShowBorder(true);
        box.setTitleAlign(HorizontalAlign.CENTER);
        return box;
    }
}