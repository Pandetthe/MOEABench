package pl.edu.agh.to.kotospring.client.scenarios.abstractions;

import org.springframework.lang.Nullable;
import org.springframework.shell.component.view.control.View;

public interface ScenarioContext {
    View view();

    Scenario scenario();

    void start();

    void stop();

    static ScenarioContext of(View view, Scenario scenario) {
        return of(view, scenario, null, null);
    }

    static ScenarioContext of(View view, Scenario scenario, @Nullable Runnable start, @Nullable Runnable stop) {
        return new ScenarioContext() {
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

        };
    }
}