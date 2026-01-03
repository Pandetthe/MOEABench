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
import pl.edu.agh.to.kotospring.client.views.ScenarioButtonCell;

public class MainMenu {

    private final static ParameterizedTypeReference<ResizingListViewOpenSelectedItemEvent<ScenarioData>> CUSTOM_LIST_TYPEREF
            = new ParameterizedTypeReference<>() {};

    private final List<ScenarioData> scenarioList = new ArrayList<>();
    private final TerminalUIBuilder terminalUIBuilder;

    private ScenarioContext currentScenarioContext = null;
    private TerminalUI ui;

    private ResizingListView<ScenarioData> scenariosView;

    private View rootView;
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

    public void run() {
        ui = terminalUIBuilder.build();
        eventLoop = ui.getEventLoop();
        rootView = buildScenarioBrowser(eventLoop, ui);
        eventLoop.onDestroy(eventLoop.keyEvents()
                .doOnNext(m -> {
                    if (m.getPlainKey() == Key.q && m.hasCtrl()) {
                        if (currentScenarioContext != null) {
                            currentScenarioContext.stop();
                            currentScenarioContext = null;
                            ui.setRoot(rootView, true);
                            ui.setFocus(scenariosView);
                        } else {
                            requestQuit();
                        }
                    }
                })
                .subscribe());

        ui.setRoot(rootView, true);
        ui.setFocus(scenariosView);
        ui.run();
    }

    private View buildScenarioBrowser(EventLoop eventLoop, TerminalUI component) {
        scenariosView = buildScenarioSelector();
        scenariosView.setItems(scenarioList);

        StatusBarView statusBar = buildStatusBar(eventLoop);

        GridView grid = new GridView();
        grid.setTitle("Main menu");
        grid.setTitleAlign(HorizontalAlign.CENTER);
        grid.setShowBorder(true);
        component.configure(grid);

        grid.setRowSize(0, 1);
        grid.setColumnSize(0);

        grid.addItem(scenariosView, 0, 0, 1, 1, 0, 0);
        grid.addItem(statusBar, 1, 0, 1, 1, 0, 0);

        eventLoop.onDestroy(eventLoop.viewEvents(CUSTOM_LIST_TYPEREF, scenariosView)
                .subscribe(event -> {
                    ScenarioContext context = event.args().item().scenario().configure(ui).buildContext();
                    ui.configure(context.view());
                    component.setRoot(context.view(), true);
                    context.start();
                    currentScenarioContext = context;
                }));

        return grid;
    }

    private ResizingListView<ScenarioData> buildScenarioSelector() {
        ResizingListView<ScenarioData> scenarios = new ResizingListView<>();
        ui.configure(scenarios);
        scenarios.setShowBorder(false);
        scenarios.setRowHeight(3);
        scenarios.setCellFactory((list, item) -> new ScenarioButtonCell(item));
        return scenarios;
    }

    private StatusBarView buildStatusBar(EventLoop eventLoop) {
        Runnable quitAction = this::requestQuit;
        StatusBarView statusBar = new StatusBarView(new StatusItem[]{
                StatusItem.of("CTRL-Q Exit", quitAction)
        });
        ui.configure(statusBar);
        return statusBar;
    }
}