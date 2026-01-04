package pl.edu.agh.to.kotospring.client.scenarios;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.shell.component.view.control.GridView;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.component.view.event.EventLoop;
import org.springframework.shell.geom.HorizontalAlign;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.experiments.ExperimentAction;
import pl.edu.agh.to.kotospring.client.views.*;
import pl.edu.agh.to.kotospring.client.views.ResizingListView.ResizingListViewOpenSelectedItemEvent;
import pl.edu.agh.to.kotospring.client.views.cells.UniversalButtonCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScenarioComponent(name = "Experiments")
public class ExperimentScenario extends Scenario {

    private final static ParameterizedTypeReference<ResizingListViewOpenSelectedItemEvent<ExperimentOption>> LIST_EVENT_TYPE
            = new ParameterizedTypeReference<>() {
    };

    private final List<ExperimentAction> actions;
    private ResizingListView<ExperimentOption> optionsListView;
    private final ExperimentClient experimentClient;


    public ExperimentScenario(ExperimentClient experimentClient, List<ExperimentAction> actions) {
        this.experimentClient = experimentClient;
        this.actions = actions;
    }

    @Override
    public View build() {
        GridView grid = new GridView();
        configure(grid);
        grid.setTitle("Experiments");
        grid.setShowBorder(true);
        grid.setTitleAlign(HorizontalAlign.CENTER);
        grid.setRowSize(0, 1);
        grid.setColumnSize(0);

        List<ExperimentOption> menuOptions = new ArrayList<>();
        for (ExperimentAction action : actions) {
            menuOptions.add(new ExperimentOption(
                    action.getMenuLabelAndTitle(),
                    () -> launchAction(action)
            ));
        }
        optionsListView = new ResizingListView<>();
        configure(optionsListView);
        optionsListView.setShowBorder(false);
        optionsListView.setRowHeight(3);
        optionsListView.setCellFactory((list, item) -> new UniversalButtonCell<>(item, ExperimentOption::name));
        optionsListView.setItems(menuOptions);

        grid.addItem(optionsListView, 0, 0, 1, 1, 0, 0);
        return grid;
    }


    @Override
    public ScenarioContext buildContext() {
        return ScenarioContext.of(build(), this::onStart, null);
    }

    private void onStart() {
        EventLoop loop = getEventloop();
        getTerminalUI().setFocus(optionsListView);
        loop.onDestroy(loop.viewEvents(LIST_EVENT_TYPE, optionsListView)
                .subscribe(event -> {
                    if (event.args().item() != null) event.args().item().action().run();
                }));
    }

    private void launchAction(ExperimentAction action) {
        InputForm form = new InputForm(getTerminalUI(), action.getMenuLabelAndTitle());
        action.configureInput(form);
        boolean requiresInput = form.hasInputs();

        if (requiresInput) {
            form.setSubmitAction("Submit", (data) -> {
                runActionLogic(action, data);
            });
            getTerminalUI().setRoot(form, true);
            try {
                Thread.sleep(50);
                form.focusFirstInput();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            runActionLogic(action, Map.of());
        }
    }

    private void runActionLogic(ExperimentAction action, Map<String, String> data) {
        try {
            View resultView = action.execute(data, experimentClient);

            if (resultView != null) {
                getTerminalUI().configure(resultView);

                if (resultView instanceof SimpleTableView tableView) {
                    getTerminalUI().configure(tableView.getContentList());
                    getTerminalUI().setRoot(resultView, true);
                    getTerminalUI().setFocus(tableView.getContentList());

                } else if (resultView instanceof SimpleMessageView messageView) {
                    getTerminalUI().configure(messageView.getContentList());
                    getTerminalUI().setRoot(resultView, true);
                    getTerminalUI().setFocus(messageView.getContentList());
                }
                else {
                    getTerminalUI().setRoot(resultView, true);
                }

            } else {
                getTerminalUI().setRoot(build(), true);
                onStart();
            }
        }catch (Exception e) {
        e.printStackTrace();
        View err = new SimpleMessageView("Error", e.getMessage() == null ? e.toString() : e.getMessage());
        getTerminalUI().configure(err);
        if (err instanceof SimpleMessageView mv) {
            getTerminalUI().configure(mv.getContentList());
            getTerminalUI().setRoot(err, true);
            getTerminalUI().setFocus(mv.getContentList());
        } else {
            getTerminalUI().setRoot(err, true);
        }
    }

}
}