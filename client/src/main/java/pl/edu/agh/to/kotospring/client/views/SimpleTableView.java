package pl.edu.agh.to.kotospring.client.views;

import pl.edu.agh.to.kotospring.client.models.ExperimentOption;
import pl.edu.agh.to.kotospring.client.views.cells.SimpleTextCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleTableView extends ResizingListView<ExperimentOption> {
    private final List<Integer> colWidths;
    private final List<String> allHeaders;
    private final List<List<String>> allData;

    private boolean pagingEnabled = false;
    private int frozenColumns = 0;
    private int pageColumns = 0;
    private int colOffset = 0;

    private boolean showFullNumericCells = false;
    private boolean indicatorsFullOnLastPage = false;
    private int indicatorsLastPageWidth = 80;

    public SimpleTableView(List<String> headers, List<List<String>> data, List<Integer> colWidths) {
        super();
        this.colWidths = colWidths;
        this.allHeaders = headers == null ? List.of() : new ArrayList<>(headers);
        this.allData = data == null ? List.of() : new ArrayList<>(data);

        setTitle("Results");
        setShowBorder(true);
        setRowHeight(1);
        setCellFactory((list, item) -> new SimpleTextCell(item));
        setAutoRunOnOpen(true);

        refreshItems(buildRowsNoPaging());
    }

    public SimpleTableView(List<List<String>> data, List<Integer> colWidths) {
        this(List.of(), data, colWidths);
    }

    public void setShowFullNumericCells(boolean enabled) {
        this.showFullNumericCells = enabled;
        refreshItems(pagingEnabled ? buildRowsWithPaging() : buildRowsNoPaging());
    }

    public void enableColumnPaging(int frozenColumns, int pageColumns) {
        this.pagingEnabled = true;
        this.frozenColumns = Math.max(0, frozenColumns);
        this.pageColumns = Math.max(1, pageColumns);
        this.colOffset = 0;
        refreshItems(buildRowsWithPaging());
    }

    public void setIndicatorsFullOnLastPage(boolean enabled) {
        this.indicatorsFullOnLastPage = enabled;
        refreshItems(pagingEnabled ? buildRowsWithPaging() : buildRowsNoPaging());
    }

    public void setIndicatorsLastPageWidth(int width) {
        this.indicatorsLastPageWidth = Math.max(20, width);
        refreshItems(pagingEnabled ? buildRowsWithPaging() : buildRowsNoPaging());
    }

    private void refreshItems(List<ExperimentOption> rows) {
        setItems(rows);
    }

    private List<ExperimentOption> buildRowsNoPaging() {
        List<ExperimentOption> rows = new ArrayList<>();

        if (!allHeaders.isEmpty()) {
            rows.add(new ExperimentOption(formatRow(allHeaders, colWidths, allHeaders, false), () -> {}));
            rows.add(new ExperimentOption(createSeparator(allHeaders.size(), colWidths, allHeaders), () -> {}));
        }

        for (List<String> rowData : allData) {
            rows.add(new ExperimentOption(formatRow(rowData, colWidths, allHeaders, false), () -> {}));
        }
        return rows;
    }

    private List<ExperimentOption> buildRowsWithPaging() {
        int totalCols = allHeaders.size();
        if (totalCols == 0) return List.of(new ExperimentOption("No columns", () -> {}));

        int frozen = Math.min(frozenColumns, totalCols);
        int scrollable = Math.max(0, totalCols - frozen);

        if (scrollable <= 0 || pageColumns <= 0 || totalCols <= frozen + pageColumns) {
            return buildRowsNoPaging();
        }

        int maxOffset = Math.max(0, scrollable - pageColumns);
        colOffset = Math.min(colOffset, maxOffset);

        boolean isLastPage = (colOffset >= maxOffset);

        List<Integer> visibleIdx = new ArrayList<>();
        for (int i = 0; i < frozen; i++) visibleIdx.add(i);

        int start = frozen + colOffset;
        int endExclusive = Math.min(frozen + colOffset + pageColumns, totalCols);
        for (int i = start; i < endExclusive; i++) visibleIdx.add(i);

        List<String> headers = pick(allHeaders, visibleIdx);

        List<List<String>> data = new ArrayList<>();
        for (List<String> row : allData) data.add(pickRow(row, visibleIdx));

        List<Integer> widths = new ArrayList<>();
        for (int idx : visibleIdx) widths.add(idx < colWidths.size() ? colWidths.get(idx) : 20);

        if (indicatorsFullOnLastPage && isLastPage) {
            int indIdx = indexOfIgnoreCase(headers, "Indicators");
            if (indIdx >= 0) {
                widths.set(indIdx, Math.max(widths.get(indIdx), indicatorsLastPageWidth));
            }
        }

        List<ExperimentOption> rows = new ArrayList<>();
        rows.add(new ExperimentOption("<< Columns", this::pageLeft));
        rows.add(new ExperimentOption("Columns >>", this::pageRight));

        if (!allHeaders.isEmpty()) {
            rows.add(new ExperimentOption(formatRow(headers, widths, headers, isLastPage), () -> {}));
            rows.add(new ExperimentOption(createSeparator(headers.size(), widths, headers), () -> {}));
        }

        for (List<String> rowData : data) {
            rows.add(new ExperimentOption(formatRow(rowData, widths, headers, isLastPage), () -> {}));
        }

        return rows;
    }

    private void pageLeft() {
        if (!pagingEnabled) return;

        int totalCols = allHeaders.size();
        int frozen = Math.min(frozenColumns, totalCols);
        int scrollable = Math.max(0, totalCols - frozen);
        int maxOffset = Math.max(0, scrollable - pageColumns);

        colOffset = Math.max(0, colOffset - pageColumns);
        colOffset = Math.min(colOffset, maxOffset);

        refreshItems(buildRowsWithPaging());
    }

    private void pageRight() {
        if (!pagingEnabled) return;

        int totalCols = allHeaders.size();
        int frozen = Math.min(frozenColumns, totalCols);
        int scrollable = Math.max(0, totalCols - frozen);
        int maxOffset = Math.max(0, scrollable - pageColumns);

        colOffset = Math.min(maxOffset, colOffset + pageColumns);
        refreshItems(buildRowsWithPaging());
    }

    private String formatRow(List<String> rowData, List<Integer> widths, List<String> headersForThisRow, boolean isLastPage) {
        if (rowData == null) rowData = List.of();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rowData.size(); i++) {
            int width = i < widths.size() ? widths.get(i) : 20;
            String cell = rowData.get(i);
            if (cell == null) cell = "";

            String header = (headersForThisRow != null && i < headersForThisRow.size()) ? headersForThisRow.get(i) : "";

            boolean isIndicators = "Indicators".equalsIgnoreCase(header);
            boolean isPartId = "Part ID".equalsIgnoreCase(header);

            boolean truncate = true;

            if (isIndicators && indicatorsFullOnLastPage && isLastPage) {
                truncate = false;
            } else if (showFullNumericCells && !isIndicators && !isPartId) {
                truncate = false;
            }

            cell = fitToWidth(cell, width, truncate);

            if (i != rowData.size() - 1) {
                sb.append(String.format("%-" + width + "s", cell)).append(" | ");
            } else {
                sb.append(String.format("%-" + width + "s", cell));
            }
        }
        return sb.toString();
    }

    private static String fitToWidth(String cell, int width, boolean truncate) {
        if (width <= 0) return "";

        if (cell.length() <= width) return cell;

        if (truncate) {
            if (width <= 3) return cell.substring(0, width);
            return cell.substring(0, width - 3) + "...";
        } else {
            return cell.substring(0, width);
        }
    }

    private String createSeparator(int columns, List<Integer> widths, List<String> headersForThisRow) {
        List<String> separators = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            int width = i < widths.size() ? widths.get(i) : 20;
            separators.add("-".repeat(Math.max(0, width)));
        }
        return formatRow(separators, widths, headersForThisRow, false);
    }

    private static int indexOfIgnoreCase(List<String> list, String needle) {
        if (list == null || needle == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            String v = list.get(i);
            if (v != null && v.equalsIgnoreCase(needle)) return i;
        }
        return -1;
    }

    private static List<String> pick(List<String> src, List<Integer> idx) {
        if (src == null || src.isEmpty()) return List.of();
        List<String> out = new ArrayList<>(idx.size());
        for (int i : idx) out.add(i >= 0 && i < src.size() ? src.get(i) : "");
        return out;
    }

    private static List<String> pickRow(List<String> src, List<Integer> idx) {
        if (src == null) src = Collections.emptyList();
        List<String> out = new ArrayList<>(idx.size());
        for (int i : idx) out.add(i >= 0 && i < src.size() ? safe(src.get(i)) : "");
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}