package pl.edu.agh.to.kotospring.client.services;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.shell.component.message.ShellMessageBuilder;
import org.springframework.shell.component.view.TerminalUI;
import org.springframework.shell.component.view.TerminalUIBuilder;
import org.springframework.shell.component.view.control.*;
import org.springframework.shell.component.view.control.StatusBarView.StatusItem;
import org.springframework.shell.component.view.event.EventLoop;
import org.springframework.shell.component.view.event.KeyEvent;
import org.springframework.shell.component.view.event.KeyEvent.Key;
import org.springframework.shell.geom.HorizontalAlign;
import org.springframework.util.StringUtils;
import pl.edu.agh.to.kotospring.client.models.ScenarioData;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.ResizingListView.ResizingListViewOpenSelectedItemEvent;
import pl.edu.agh.to.kotospring.client.views.cells.UniversalButtonCell;

public class MainMenu {
    private final List<ScenarioData> scenarioList = new ArrayList<>();
    private final TerminalUIBuilder terminalUIBuilder;

    private final Deque<ScenarioContext> contextStack = new ArrayDeque<>();

    private TerminalUI ui;

    private ResizingListView<ScenarioData> scenariosView;
    private StatusBarView statusBar;
    private GridView mainGrid;

    private EventLoop eventLoop;

    public MainMenu(TerminalUIBuilder terminalUIBuilder, List<Scenario> scenarios) {
        this.terminalUIBuilder = terminalUIBuilder;
        mapScenarios(scenarios);
    }

    private void mapScenarios(List<Scenario> scenarios) {
        scenarios.forEach(sce -> {
            ScenarioComponent ann = AnnotationUtils.findAnnotation(sce.getClass(), ScenarioComponent.class);
            if (ann != null) {
                String name = ann.name();
                if (StringUtils.hasText(name) && ann.type() == ScenarioType.MAIN_MENU) {
                    scenarioList.add(new ScenarioData(sce, name));
                }
            }
        });
    }

    private void requestQuit() {
        eventLoop.dispatch(ShellMessageBuilder.ofInterrupt());
    }

    private void returnToMenu() {
        while (!contextStack.isEmpty()) {
            ScenarioContext current = contextStack.pop();
            current.stop();
            if (contextStack.isEmpty()) {
                updateGridContent(scenariosView);
                updateStatusBarForMenu();
                ui.setFocus(scenariosView);
            } else {
                ScenarioContext previous = contextStack.peek();
                ScenarioComponent ann = AnnotationUtils.findAnnotation(previous.scenario().getClass(), ScenarioComponent.class);
                if (ann != null && ann.skipOnReturn())
                    continue;
                updateGridContent(previous.view());
                updateStatusBarForScenario();
                ui.setFocus(previous.view());
            }
            break;
        }
    }

    private void handleQuitOrReturn() {
        if (!contextStack.isEmpty()) {
            returnToMenu();
        } else {
            requestQuit();
        }
    }

    public void run() {
        ui = terminalUIBuilder.build();
        eventLoop = ui.getEventLoop();

        initializeLayout();

        eventLoop.onDestroy(eventLoop.keyEvents()
                .doOnNext(m -> {
                    if (m.getPlainKey() == Key.q && m.hasCtrl()) {
                        handleQuitOrReturn();
                    } else if (m.getPlainKey() == (1073741928 & 1048575)) {
                        // Fix for weird bug, that backspace is not the same key
                        eventLoop.dispatch(ShellMessageBuilder.ofKeyEvent(new KeyEvent(Key.Backspace, null)));
                    }
                })
                .subscribe());

        ui.setRoot(mainGrid, true);
        ui.setFocus(scenariosView);
        ui.run();
    }

    private void initializeLayout() {
        scenariosView = buildScenarioSelector();
        scenariosView.setItems(scenarioList);

        statusBar = buildStatusBar();

        mainGrid = new GridView();
        ui.configure(mainGrid);

        mainGrid.setRowSize(0, 1);
        mainGrid.setColumnSize(0);

        updateGridContent(scenariosView);
        updateStatusBarForMenu();

        eventLoop.onDestroy(eventLoop.viewEvents(ResizingListViewOpenSelectedItemEvent.class, scenariosView)
                .subscribe(event -> {
                    Object item = event.args().item();
                    if (item instanceof ScenarioData scenarioData) {
                        Scenario scenario = scenarioData.scenario();
                        openScenario(scenario);
                    }
                }));
    }

    private void openScenario(Scenario scenario) {
        scenario.configure(ui);
        scenario.setNavigationConsumer(this::navigateTo);
        ScenarioContext context = scenario.buildContext();
        navigateTo(context);
    }

    private void navigateTo(ScenarioContext context) {
        ui.configure(context.view());
        contextStack.push(context);
        updateGridContent(context.view());
        updateStatusBarForScenario();
        ui.setFocus(context.view());
        context.start();
    }

    private void updateGridContent(View centerView) {
        mainGrid.clearItems();
        mainGrid.addItem(centerView, 0, 0, 1, 1, 0, 0);
        mainGrid.addItem(statusBar, 1, 0, 1, 1, 0, 0);
    }

    private void updateStatusBarForMenu() {
        statusBar.setItems(List.of(
                StatusItem.of("CTRL-Q Exit", this::requestQuit)
        ));
    }

    private void updateStatusBarForScenario() {
        statusBar.setItems(List.of(
                StatusItem.of("CTRL-Q Return", this::returnToMenu)
        ));
    }

    private ResizingListView<ScenarioData> buildScenarioSelector() {
        ResizingListView<ScenarioData> scenarios = new ResizingListView<>();
        ui.configure(scenarios);
        scenarios.setRowHeight(3);
        scenarios.setTitle("Main menu");
        scenarios.setTitleAlign(HorizontalAlign.CENTER);
        scenarios.setShowBorder(true);
        scenarios.setCenterVertically(true);
        scenarios.setCellFactory((list, item) -> new UniversalButtonCell<>(item, ScenarioData::name));
        return scenarios;
    }

    private StatusBarView buildStatusBar() {
        StatusBarView statusBar = new StatusBarView();
        ui.configure(statusBar);
        return statusBar;
    }
}