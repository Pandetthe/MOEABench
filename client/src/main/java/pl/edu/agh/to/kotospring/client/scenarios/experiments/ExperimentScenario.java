package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import org.springframework.util.StringUtils;
import pl.edu.agh.to.kotospring.client.models.ScenarioData;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.*;
import pl.edu.agh.to.kotospring.client.views.cells.UniversalButtonCell;

import java.util.ArrayList;
import java.util.List;

@ScenarioComponent(name = "Experiments")
public class ExperimentScenario extends Scenario {
    private final List<ScenarioData> scenarioList = new ArrayList<>();
    private final static int ROW_HEIGHT = 3;

    public ExperimentScenario(List<Scenario> scenarios) {
        mapScenarios(scenarios);
    }

    private void mapScenarios(List<Scenario> scenarios) {
        scenarios.forEach(sce -> {
            ScenarioComponent ann = AnnotationUtils.findAnnotation(sce.getClass(), ScenarioComponent.class);
            if (ann != null) {
                String name = ann.name();
                if (StringUtils.hasText(name) && ann.type() == ScenarioType.EXPERIMENT_MENU) {
                    scenarioList.add(new ScenarioData(sce, name));
                }
            }
        });
    }

    @Override
    public View build() {
        ResizingListView<ScenarioData> scenarios = new ResizingListView<>();
        configure(scenarios);
        scenarios.setRowHeight(ROW_HEIGHT);
        scenarios.setTitle("Experiments menu");
        scenarios.setTitleAlign(HorizontalAlign.CENTER);
        scenarios.setShowBorder(true);
        scenarios.setCenterVertically(true);
        scenarios.setCellFactory((list, item) -> new UniversalButtonCell<>(item, ScenarioData::name));
        scenarios.setItems(scenarioList);
        getEventloop().onDestroy(
                getEventloop().viewEvents(ResizingListView.ResizingListViewOpenSelectedItemEvent.class, scenarios)
                        .subscribe(event -> {
                            Object item = event.args().item();
                            if (item instanceof ScenarioData scenarioData) {
                                Scenario scenario = scenarioData.scenario();
                                wireChild(scenario);
                                navigate(scenario.buildContext());
                            }
                        }));
        return scenarios;
    }
}