package pl.edu.agh.to.kotospring.client.scenarios.abstractions;

import org.springframework.lang.Nullable;
import org.springframework.shell.component.view.control.View;

import java.util.ArrayList;
import java.util.List;

public interface ScenarioContext {
    View view();

    Scenario scenario();

    void start();

    void stop();

    List<String> statusBarItems();

    void setStatusBarItems(List<String> items);

    static ScenarioContext of(View view, Scenario scenario) {
        return of(view, scenario, null, null);
    }

    static ScenarioContext of(View view, Scenario scenario, @Nullable Runnable start, @Nullable Runnable stop) {
        return new ScenarioContext() {
            private final List<String> statusBarItems = new ArrayList<>();

            @Override
            public View view() {
                return view;
            }

            @Override
            public Scenario scenario() {
                return scenario;
            }

            @Override
            public void start() {
                if (start != null) {
                    start.run();
                }
            }

            @Override
            public void stop() {
                if (stop != null) {
                    stop.run();
                }
            }

            @Override
            public List<String> statusBarItems() {
                return statusBarItems;
            }

            @Override
            public void setStatusBarItems(List<String> items) {
                this.statusBarItems.clear();
                if (items != null) {
                    this.statusBarItems.addAll(items);
                }
            }
        };
    }
}