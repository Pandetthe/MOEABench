package pl.edu.agh.to.kotospring.client.scenarios.abstractions;

import org.springframework.shell.component.view.TerminalUI;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.component.view.control.ViewService;
import org.springframework.shell.component.view.event.EventLoop;

import java.util.List;
import java.util.function.Consumer;

public abstract class Scenario {
    private TerminalUI ui;
    private ViewService viewService;
    private EventLoop eventloop;
    private Consumer<ScenarioContext> navigationConsumer;
    private Consumer<ScenarioContext> replacementConsumer;
    private Consumer<List<String>> statusBarConsumer;

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

    public void setReplacementConsumer(Consumer<ScenarioContext> replacementConsumer) {
        this.replacementConsumer = replacementConsumer;
    }

    public void setStatusBarConsumer(Consumer<List<String>> statusBarConsumer) {
        this.statusBarConsumer = statusBarConsumer;
    }

    public void setStatusBar(List<String> statusBar) {
        if (statusBarConsumer != null) {
            statusBarConsumer.accept(statusBar);
        }
    }

    protected void navigate(ScenarioContext context) {
        if (navigationConsumer != null) {
            navigationConsumer.accept(context);
        }
    }

    protected void navigate(View view) {
        navigate(createContext(view));
    }

    protected void replace(ScenarioContext context) {
        if (replacementConsumer != null) {
            replacementConsumer.accept(context);
        } else {
            navigate(context);
        }
    }

    protected void replace(View view) {
        replace(createContext(view));
    }

    protected void replace(View view, Runnable onStart) {
        replace(createContext(view, onStart));
    }

    protected ScenarioContext createContext(View view) {
        return ScenarioContext.of(view, this);
    }

    protected ScenarioContext createContext(View view, Runnable onStart) {
        return ScenarioContext.of(view, this, onStart, null);
    }

    protected void wireChild(Scenario child) {
        child.configure(getTerminalUI());
        child.setNavigationConsumer(this::navigate);
        child.setReplacementConsumer(this::replace);
        child.setStatusBarConsumer(this::setStatusBar);
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