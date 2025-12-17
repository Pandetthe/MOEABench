package pl.edu.agh.to.kotospring.client.shell;

import org.jline.terminal.Terminal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.table.*;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;

import java.util.List;

@ShellComponent
public class RegistryCommands {
    private final RegistryClient api;
    private final Terminal terminal;

    public RegistryCommands(RegistryClient api, Terminal terminal) {
        this.api = api;
        this.terminal = terminal;
    }

    @ShellMethod("List available algorithms")
    public String algorithms() {
        Table table = createDynamicTable(api.getAlgorithms());
        if (table == null) return "No algorithms available.";
        return "Algorithms:\n" + table.render(getTerminalWidth());
    }

    @ShellMethod("List available problems")
    public String problems() {
        Table table = createDynamicTable(api.getProblems());
        if (table == null) return "No problems available.";
        return "Problems:\n" + table.render(getTerminalWidth());
    }

    @ShellMethod("List available indicators")
    public String indicators() {
        Table table = createDynamicTable(api.getIndicators());
        if (table == null) return "No indicators available.";
        return "Indicators:\n" + table.render(getTerminalWidth());
    }

    private int getTerminalWidth() {
        int width = terminal.getWidth();
        return (width <= 0) ? 120 : width;
    }

    private Table createDynamicTable(List<String> data) {
        if (data.isEmpty()) {
            return null;
        }

        int maxItemLength = data.stream().mapToInt(String::length).max().orElse(0);
        int colWidth = maxItemLength + 3;

        int columns = Math.max(1, getTerminalWidth() / colWidth);

        int rows = (int) Math.ceil((double) data.size() / columns);

        String[][] tableData = new String[rows][columns];

        for (int i = 0; i < rows * columns; i++) {
            int row = i / columns;
            int col = i % columns;

            if (i < data.size()) {
                tableData[row][col] = data.get(i);
            } else {
                tableData[row][col] = "";
            }
        }

        TableModel model = new ArrayTableModel(tableData);
        TableBuilder tableBuilder = new TableBuilder(model);

        return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
    }
}