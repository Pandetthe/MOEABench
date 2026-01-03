package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.shell.component.view.control.BoxView;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;

@ScenarioComponent(name = "Indicators")
public class IndicatorScenario extends Scenario {
    @Override
    public View build() {
        BoxView box = new BoxView();
        configure(box);
        box.setTitle("Indicators");
        box.setShowBorder(true);
        box.setTitleAlign(HorizontalAlign.CENTER);
        return box;
    }
}