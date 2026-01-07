package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.TerminalUI;
import org.springframework.shell.component.view.control.InputView;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InputForm extends FixedGridView {

    private final TerminalUI ui;
    private final Map<String, InputView> inputs = new LinkedHashMap<>();
    private final List<Integer> rowSizes = new ArrayList<>(Arrays.asList(0, 0));

    public InputForm(TerminalUI ui, String title) {
        this.ui = ui;
        ui.configure(this);
        setTitle(title);
        setShowBorder(true);

        setColumnSize(0, 50, 0);
        updateRowSizes();
    }

    private void updateRowSizes() {
        int[] rows = new int[rowSizes.size()];
        for (int i = 0; i < rowSizes.size(); i++) {
            rows[i] = rowSizes.get(i);
        }
        setRowSize(rows);
    }

    public void addInput(String key, String label) {
        InputView input = new InputView();
        ui.configure(input);
        input.setTitle(label);
        input.setShowBorder(true);

        int rowIndex = rowSizes.size() - 1;
        rowSizes.add(rowIndex, 4);
        updateRowSizes();

        addItem(input, rowIndex, 1, 1, 1, 0, 0);

        inputs.put(key, input);
    }

    public void setSubmitAction(String buttonLabel, Consumer<Map<String, String>> onSubmit) {
        CenteredButtonView submitBtn = new CenteredButtonView();
        ui.configure(submitBtn);
        submitBtn.setText(buttonLabel);

        submitBtn.setAction(() -> {
            Map<String, String> results = inputs.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getInputText()));
            onSubmit.accept(results);
        });

        int rowIndex = rowSizes.size() - 1;
        rowSizes.add(rowIndex, 3);
        updateRowSizes();

        addItem(submitBtn, rowIndex, 1, 1, 1, 0, 0);
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