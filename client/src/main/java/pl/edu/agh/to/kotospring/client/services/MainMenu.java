package pl.edu.agh.to.kotospring.client.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.shell.component.message.ShellMessageBuilder;
import org.springframework.shell.component.view.TerminalUI;
import org.springframework.shell.component.view.TerminalUIBuilder;
import org.springframework.shell.component.view.control.*;
import org.springframework.shell.component.view.control.StatusBarView.StatusItem;
import org.springframework.shell.component.view.event.EventLoop;
import org.springframework.shell.component.view.event.KeyEvent.Key;
import org.springframework.shell.geom.HorizontalAlign;
import org.springframework.util.StringUtils;
import pl.edu.agh.to.kotospring.client.models.ScenarioData;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;
import pl.edu.agh.to.kotospring.client.views.ResizingListView.ResizingListViewOpenSelectedItemEvent;
import pl.edu.agh.to.kotospring.client.views.cells.UniversalButtonCell;

public class MainMenu {

    private final static ParameterizedTypeReference<ResizingListViewOpenSelectedItemEvent<ScenarioData>> CUSTOM_LIST_TYPEREF
            = new ParameterizedTypeReference<>() {};

    private final List<ScenarioData> scenarioList = new ArrayList<>();
    private final TerminalUIBuilder terminalUIBuilder;

    private ScenarioContext currentScenarioContext = null;
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
                if (StringUtils.hasText(name)) {
                    scenarioList.add(new ScenarioData(sce, name));
                }
            }
        });
    }

    private void requestQuit() {
        eventLoop.dispatch(ShellMessageBuilder.ofInterrupt());
    }

    private void returnToMenu() {
        if (currentScenarioContext != null) {
            currentScenarioContext.stop();
            currentScenarioContext = null;
            updateGridContent(scenariosView);
            ui.setFocus(scenariosView);
        }
    }

    private void handleQuitOrReturn() {
        if (currentScenarioContext != null) {
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

        eventLoop.onDestroy(eventLoop.viewEvents(CUSTOM_LIST_TYPEREF, scenariosView)
                .subscribe(event -> {
                    ScenarioContext context = event.args().item().scenario().configure(ui).buildContext();
                    ui.configure(context.view());

                    updateGridContent(context.view());

                    context.start();
                    currentScenarioContext = context;
                }));
    }

    private void updateGridContent(View centerView) {
        mainGrid.clearItems();
        mainGrid.addItem(centerView, 0, 0, 1, 1, 0, 0);
        mainGrid.addItem(statusBar, 1, 0, 1, 1, 0, 0);
    }

    private ResizingListView<ScenarioData> buildScenarioSelector() {
        ResizingListView<ScenarioData> scenarios = new ResizingListView<>();
        ui.configure(scenarios);
        scenarios.setTitle("Main menu");
        scenarios.setTitleAlign(HorizontalAlign.CENTER);
        scenarios.setShowBorder(true);
        scenarios.setRowHeight(3);
        scenarios.setCellFactory((list, item) -> new UniversalButtonCell<>(item, ScenarioData::name));
        return scenarios;
    }

    private StatusBarView buildStatusBar() {
        StatusBarView statusBar = new StatusBarView(new StatusItem[]{
                StatusItem.of("CTRL-Q Exit/Return", this::handleQuitOrReturn)
        });
        ui.configure(statusBar);
        return statusBar;
    }
}