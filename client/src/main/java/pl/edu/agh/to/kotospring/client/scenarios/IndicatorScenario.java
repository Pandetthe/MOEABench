package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
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
        try {
            ResizingListView<String> listView = new ResizingListView<>();
            configure(listView);
            listView.setTitle("Indicators");
            listView.setShowBorder(true);
            listView.setTitleAlign(HorizontalAlign.CENTER);
            listView.setCenterVertically(true);
            listView.setCellFactory((list, item) -> new CenteredCell(item));
            List<String> indicators = registryClient.getIndicators();
            listView.setItems(indicators);
            return listView;
        } catch (WebClientRequestException e) {
            return new SimpleMessageView("Connection Error", "Service unavailable. Could not connect to the server.");
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                return new SimpleMessageView("Server Error", "Internal Server Error. Please try again later.");
            }
            return new SimpleMessageView("Error", "Server returned error: " + e.getStatusCode());
        } catch (Exception e) {
            return new SimpleMessageView("Unexpected Error", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}