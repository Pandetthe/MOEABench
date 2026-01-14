package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.models.MenuOption;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioBindings;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.cells.UniversalButtonCell;

import java.util.List;

@ScenarioComponent(name = "Experiment Options", type = ScenarioType.OTHER)
@Scope("prototype")
public class ExperimentOptionsScenario extends Scenario {

    private final ObjectProvider<GetExperimentScenario> getExperimentScenarioProvider;
    private final ObjectProvider<GetExperimentAggregateScenario> getExperimentAggregateScenarioProvider;
    private long experimentId;
    private final static int ROW_HEIGHT = 3;


    public ExperimentOptionsScenario(
            ObjectProvider<GetExperimentScenario> getExperimentScenarioProvider,
            ObjectProvider<GetExperimentAggregateScenario> getExperimentAggregateScenarioProvider) {
        this.getExperimentScenarioProvider = getExperimentScenarioProvider;
        this.getExperimentAggregateScenarioProvider = getExperimentAggregateScenarioProvider;
    }

    public void setExperimentId(long experimentId) {
        this.experimentId = experimentId;
    }

    @Override
    public View build() {
        ResizingListView<MenuOption> options = new ResizingListView<>();
        configure(options);
        options.setRowHeight(ROW_HEIGHT);
        options.setTitle("Experiment ID: " + experimentId);
        options.setTitleAlign(HorizontalAlign.CENTER);
        options.setShowBorder(true);
        options.setCenterVertically(true);
        options.setCellFactory((list, item) -> new UniversalButtonCell<>(item, MenuOption::name));

        options.setItems(List.of(
                new MenuOption("Show Runs", this::openRuns),
                new MenuOption("Show Aggregations", this::openAggregations)));

        ScenarioBindings bindings = new ScenarioBindings(getEventloop());
        bindings.onOpenSelectedItem(options, MenuOption.class, option -> option.action().run());


        return options;
    }

    private void openRuns() {
        GetExperimentScenario scenario = getExperimentScenarioProvider.getObject();
        scenario.setExperimentId(experimentId);
        wireChild(scenario);
        navigate(scenario.buildContext());
    }

    private void openAggregations() {
        GetExperimentAggregateScenario scenario = getExperimentAggregateScenarioProvider.getObject();
        scenario.setExperimentId(experimentId);
        wireChild(scenario);
        navigate(scenario.buildContext());
    }
}
