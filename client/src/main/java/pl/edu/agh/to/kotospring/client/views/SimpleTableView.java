package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.control.GridView;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;
import pl.edu.agh.to.kotospring.client.views.cells.SimpleTextCell;

import java.util.ArrayList;
import java.util.List;

public class SimpleTableView extends GridView {

    private final ResizingListView<ExperimentOption> contentList;
    private final List<Integer> colWidths;

    public SimpleTableView(List<String> headers, List<List<String>> data, List<Integer> colWidths) {
        this.colWidths = colWidths;

        setTitle("Results");
        setShowBorder(true);
        setRowSize(0, 1);
        setColumnSize(0);

        List<ExperimentOption> rows = new ArrayList<>();
        String headerText = formatRow(headers);
        rows.add(new ExperimentOption(headerText, () -> {}));
        rows.add(new ExperimentOption(createSeparator(), () -> {}));

        for (List<String> rowData : data) {
            String formattedRow = formatRow(rowData);
            rows.add(new ExperimentOption(formattedRow, () -> {}));
        }

        contentList = new ResizingListView<>();
        contentList.setShowBorder(false);
        contentList.setRowHeight(1);

        contentList.setCellFactory((list, item) -> new SimpleTextCell(item));
        contentList.setItems(rows);
        addItem(contentList, 0, 0, 1, 1, 0, 0);
    }

    public ResizingListView<ExperimentOption> getContentList() {
        return contentList;
    }

    private String formatRow(List<String> rowData) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rowData.size(); i++) {
            int width = i < colWidths.size() ? colWidths.get(i) : 20;
            String cell = rowData.get(i);
            if (cell == null) cell = "";
            if (cell.length() > width) {
                cell = cell.substring(0, Math.max(0, width - 3)) + "...";
            }
            if (i != rowData.size() - 1) {
            sb.append(String.format("%-" + width + "s", cell)).append(" | ");
            }
            else {
                sb.append(String.format("%-" + width + "s", cell));
            }
        }
        return sb.toString();
    }

    private String createSeparator() {
        List<String> separators = new ArrayList<>();
        for (Integer width : colWidths) {
            separators.add("-".repeat(width));
        }
        return formatRow(separators);
    }
}