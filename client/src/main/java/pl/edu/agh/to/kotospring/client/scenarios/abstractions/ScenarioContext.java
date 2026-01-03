package pl.edu.agh.to.kotospring.client.scenarios.abstractions;

import org.springframework.lang.Nullable;
import org.springframework.shell.component.view.control.View;

public interface ScenarioContext {
    View view();

    void start();

    void stop();

    static ScenarioContext of(View view) {
        return of(view, null, null);
    }

    static ScenarioContext of(View view, @Nullable Runnable start, @Nullable Runnable stop) {
        return new ScenarioContext() {

            @Override
            public org.springframework.shell.component.view.control.View view() {
                return view;
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