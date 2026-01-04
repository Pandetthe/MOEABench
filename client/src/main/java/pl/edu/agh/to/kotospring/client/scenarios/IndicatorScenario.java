package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.ListView;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.views.cells.CenteredCell;

import java.util.List;

@ScenarioComponent(name = "Indicators")
public class IndicatorScenario extends Scenario {

    private final RegistryClient registryClient;
    private ListView<String> box;

    public IndicatorScenario(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public View build() {
        box = new ListView<>();
        configure(box);
        List<String> indicators = registryClient.getIndicators();

        box.setTitle("Indicators");
        box.setShowBorder(true);
        box.setTitleAlign(HorizontalAlign.CENTER);
        box.setCellFactory((list, item) -> new CenteredCell(item));
        box.setItems(indicators);
        return box;
    }
}