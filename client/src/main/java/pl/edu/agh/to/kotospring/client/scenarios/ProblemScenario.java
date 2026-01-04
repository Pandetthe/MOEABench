package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;

import java.util.ArrayList;
import java.util.List;

@ScenarioComponent(name = "Problems")
public class ProblemScenario extends Scenario {
    private final RegistryClient registryClient;
    private static final int COLUMNS_COUNT = 6;
    private SimpleTableView tableView;

    public ProblemScenario(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public View build() {
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

        List<String> headers = new ArrayList<>();
        List<Integer> colWidths = new ArrayList<>();

        for (int i = 0; i < COLUMNS_COUNT; i++) {
            headers.add(" ");
            colWidths.add(16);
        }

        tableView = new SimpleTableView(headers, rows, colWidths);

        configure(tableView);
        tableView.setTitle("Problems");
        tableView.setTitleAlign(HorizontalAlign.CENTER);
//        tableView.setShowBorder(false);


        return tableView;
    }

    @Override
    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this::onStart, null);
    }

    private void onStart() {
        if (tableView != null) {
            getTerminalUI().configure(tableView.getContentList());
            getTerminalUI().setFocus(tableView.getContentList());
        }
    }
}