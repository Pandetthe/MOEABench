package pl.edu.agh.to.kotospring.client.scenarios.abstractions;

import org.springframework.shell.component.view.TerminalUI;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.component.view.control.ViewService;
import org.springframework.shell.component.view.event.EventLoop;
import java.util.function.Consumer;

public abstract class Scenario {
    private TerminalUI ui;
    private ViewService viewService;
    private EventLoop eventloop;
    private Consumer<ScenarioContext> navigationConsumer;

    protected ViewService getViewService() {
        return viewService;
    }

    protected EventLoop getEventloop() {
        return eventloop;
    }

    protected TerminalUI getTerminalUI() {
        return ui;
    }

    public void setNavigationConsumer(Consumer<ScenarioContext> navigationConsumer) {
        this.navigationConsumer = navigationConsumer;
    }

    protected void navigate(ScenarioContext context) {
        if (navigationConsumer != null) {
            navigationConsumer.accept(context);
        }
    }

    public abstract View build();

    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this);
    }

    public Scenario configure(TerminalUI ui) {
        this.ui = ui;
        this.eventloop = ui.getEventLoop();
        this.viewService = ui.getViewService();
        return this;
    }

    protected void configure(View view) {
        if (ui != null) {
            ui.configure(view);
        }
    }
}