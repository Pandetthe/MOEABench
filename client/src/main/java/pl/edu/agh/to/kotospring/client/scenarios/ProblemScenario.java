package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;

import java.util.ArrayList;
import java.util.List;

@ScenarioComponent(name = "Problems")
public class ProblemScenario extends Scenario {
    private final RegistryClient registryClient;
    private static final int COLUMNS_COUNT = 6;

    public ProblemScenario(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public View build() {
        try {
            List<String> allProblems = registryClient.getProblems();
            List<List<String>> rows = new ArrayList<>();
            List<String> currentRow = new ArrayList<>();

            for (String problem : allProblems) {
                currentRow.add(problem);
                if (currentRow.size() == COLUMNS_COUNT) {
                    rows.add(new ArrayList<>(currentRow));
                    currentRow.clear();
                }
            }
            if (!currentRow.isEmpty()) {
                while (currentRow.size() < COLUMNS_COUNT) {
                    currentRow.add("");
                }
                rows.add(currentRow);
            }

            List<Integer> colWidths = new ArrayList<>();

            for (int i = 0; i < COLUMNS_COUNT; i++) {
                colWidths.add(16);
            }

            SimpleTableView tableView = new SimpleTableView(rows, colWidths);
            tableView.setTitle("Problems");
            tableView.setTitleAlign(HorizontalAlign.CENTER);
            configure(tableView);
            return tableView;
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