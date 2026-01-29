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

@ScenarioComponent(name = "Experiment Group Options", type = ScenarioType.OTHER)
@Scope("prototype")
public class ExperimentGroupOptionsScenario extends Scenario {

    private final ObjectProvider<GetExperimentGroupScenario> getExperimentGroupScenarioProvider;
    private final ObjectProvider<GetExperimentGroupAggregateScenario> getExperimentGroupAggregateScenarioProvider;
    private long groupId;
    private final static int ROW_HEIGHT = 3;

    public ExperimentGroupOptionsScenario(
            ObjectProvider<GetExperimentGroupScenario> getExperimentGroupScenarioProvider,
            ObjectProvider<GetExperimentGroupAggregateScenario> getExperimentGroupAggregateScenarioProvider) {
        this.getExperimentGroupScenarioProvider = getExperimentGroupScenarioProvider;
        this.getExperimentGroupAggregateScenarioProvider = getExperimentGroupAggregateScenarioProvider;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    @Override
    public View build() {
        ResizingListView<MenuOption> options = new ResizingListView<>();
        configure(options);
        options.setRowHeight(ROW_HEIGHT);
        options.setTitle("Experiment Group ID: " + groupId);
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
        GetExperimentGroupScenario scenario = getExperimentGroupScenarioProvider.getObject();
        scenario.setGroupId(groupId);
        wireChild(scenario);
        navigate(scenario.buildContext());
    }

    private void openAggregations() {
        GetExperimentGroupAggregateScenario scenario = getExperimentGroupAggregateScenarioProvider.getObject();
        scenario.setGroupId(groupId);
        wireChild(scenario);
        navigate(scenario.buildContext());
    }
}
