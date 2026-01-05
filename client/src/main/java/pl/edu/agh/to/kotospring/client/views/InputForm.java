package pl.edu.agh.to.kotospring.client.views;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.shell.component.view.TerminalUI;
import org.springframework.shell.component.view.control.GridView;
import org.springframework.shell.component.view.control.InputView;
import org.springframework.shell.component.view.event.EventLoop;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;
import pl.edu.agh.to.kotospring.client.views.cells.UniversalButtonCell;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InputForm extends FixedGridView {

    private final static ParameterizedTypeReference<ResizingListView.ResizingListViewOpenSelectedItemEvent<ExperimentOption>> BUTTON_EVENT_TYPE
            = new ParameterizedTypeReference<>() {};
    private final TerminalUI ui;
    private final Map<String, InputView> inputs = new LinkedHashMap<>();
    private int currentRow = 1;

    public InputForm(TerminalUI ui, String title) {
        this.ui = ui;
        ui.configure(this);
        setTitle(title);
        setShowBorder(true);

        setColumnSize(0, 2);
        setColumnSize(1, 40);
        setRowSize(0, 1);
    }

    public void addInput(String key, String label) {
        InputView input = new InputView();
        ui.configure(input);
        input.setTitle(label);
        input.setShowBorder(true);

        setRowSize(currentRow, 4);
        addItem(input, currentRow, 1, 1, 1, 0, 0);

        inputs.put(key, input);
        currentRow++;
    }

    public void setSubmitAction(String buttonLabel, Consumer<Map<String, String>> onSubmit) {
        ResizingListView<ExperimentOption> submitBtn = new ResizingListView<>();
        ui.configure(submitBtn);
        submitBtn.setShowBorder(false);
        submitBtn.setRowHeight(3);
        submitBtn.setCellFactory((list, item) -> new UniversalButtonCell<>(item, ExperimentOption::name));

        ExperimentOption option = new ExperimentOption(buttonLabel, () -> {
            Map<String, String> results = inputs.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getInputText()
                    ));
            onSubmit.accept(results);
        });

        submitBtn.setItems(List.of(option));

        EventLoop loop = ui.getEventLoop();
        loop.onDestroy(loop.viewEvents(BUTTON_EVENT_TYPE, submitBtn)
                .subscribe(event -> event.args().item().action().run()));
        setRowSize(currentRow, 3);

        addItem(submitBtn, currentRow, 1, 1, 1, 0, 0);
    }

    public void focusFirstInput() {
        if (!inputs.isEmpty()) {
            ui.setFocus(inputs.values().iterator().next());
        }
    }

    public boolean hasInputs() {
        return !inputs.isEmpty();
    }
}