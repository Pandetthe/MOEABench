package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.cells.CenteredCell;

import java.util.List;

@ScenarioComponent(name = "Indicators")
public class IndicatorScenario extends Scenario {
    private final RegistryClient registryClient;

    public IndicatorScenario(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public View build() {
        ResizingListView<String> listView = new ResizingListView<>();
        configure(listView);
        List<String> indicators = registryClient.getIndicators();
        listView.setTitle("Indicators");
        listView.setShowBorder(true);
        listView.setTitleAlign(HorizontalAlign.CENTER);
        listView.setCenterVertically(true);
        listView.setCellFactory((list, item) -> new CenteredCell(item));
        listView.setItems(indicators);
        return listView;
    }
}