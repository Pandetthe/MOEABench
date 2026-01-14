package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;
import pl.edu.agh.to.kotospring.client.models.MenuOption;
import pl.edu.agh.to.kotospring.client.views.cells.SimpleTextCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleTableView extends ResizingListView<MenuOption> {
    private final List<Integer> originalColWidths;
    private List<Integer> currentColWidths;
    private final List<String> allHeaders;
    private final List<List<String>> allData;

    private boolean pagingEnabled = false;
    private int frozenColumns = 0;
    private int pageColumns = 0;
    private int colOffset = 0;

    private boolean showFullNumericCells = false;
    private boolean indicatorsFullOnLastPage = false;
    private int indicatorsLastPageWidth = 80;
    private int lastWidth = -1;
    private final static int ROW_HEIGHT = 1;
    private final static int MIN_TABLE_WIDTH = 10;

    public SimpleTableView(List<String> headers, List<List<String>> data, List<Integer> colWidths) {
        super();
        this.originalColWidths = new ArrayList<>(colWidths);
        this.currentColWidths = new ArrayList<>(colWidths);
        this.allHeaders = headers == null ? Collections.emptyList() : new ArrayList<>(headers);
        this.allData = data == null ? Collections.emptyList() : new ArrayList<>(data);

        setTitle("Results");
        setShowBorder(true);
        setRowHeight(ROW_HEIGHT);
        setCellFactory((list, item) -> new SimpleTextCell(item));
        setAutoRunOnOpen(true);
        refreshItems(buildRowsNoPaging());
    }

    public SimpleTableView(List<List<String>> data, List<Integer> colWidths) {
        this(Collections.emptyList(), data, colWidths);
    }

    @Override
    protected void drawInternal(Screen screen) {
        Rectangle rect = getInnerRect();
        if (rect.width() > 0 && rect.width() != lastWidth) {
            lastWidth = rect.width();
            recalculateAndRefresh(lastWidth);
        }
        super.drawInternal(screen);
    }

    @Override
    protected void initInternal() {
        super.initInternal();
        this.registerKeyBinding(1048578, this::pageLeft);
        this.registerKeyBinding(1048579, this::pageRight);
        this.registerKeyBinding(1048588, this::pageLeft);
        this.registerKeyBinding(1048589, this::pageRight);
    }

    private void recalculateAndRefresh(int totalWidth) {
        int numCols = getTotalColumnCount();
        if (numCols == 0) {
            return;
        }
        int maxTableWidth = Math.max(MIN_TABLE_WIDTH, totalWidth - 2);
        if (pagingEnabled) {
            validateScrollOffset(numCols);
        }
        List<Integer> visibleIdx = determineVisibleColumnIndices(numCols);
        if (pagingEnabled) {
            visibleIdx = adjustPageSizeToFitScreen(visibleIdx, maxTableWidth, numCols);
        }
        this.currentColWidths = calculateScaledColumnWidths(visibleIdx, maxTableWidth, numCols);
        refreshItems(pagingEnabled ? buildRowsWithPaging() : buildRowsNoPaging());
    }

    private int getTotalColumnCount() {
        if (!allHeaders.isEmpty()) {
            return allHeaders.size();
        }
        if (!allData.isEmpty()) {
            return allData.getFirst().size();
        }
        return 0;
    }

    private void validateScrollOffset(int numCols) {
        int scrollable = Math.max(0, numCols - frozenColumns);
        int maxOffset = Math.max(0, scrollable - pageColumns);
        colOffset = Math.min(colOffset, maxOffset);
    }

    private List<Integer> determineVisibleColumnIndices(int numCols) {
        List<Integer> visibleIdx = new ArrayList<>();
        int frozenLimit = pagingEnabled ? frozenColumns : numCols;
        for (int i = 0; i < frozenLimit; i++) {
            if (i < numCols) visibleIdx.add(i);
        }

        if (pagingEnabled) {
            int start = frozenColumns + colOffset;
            int end = Math.min(start + pageColumns, numCols);
            for (int i = start; i < end; i++) {
                visibleIdx.add(i);
            }
        }
        return visibleIdx;
    }

    private List<Integer> adjustPageSizeToFitScreen(List<Integer> currentVisibleIdx, int maxTableWidth, int numCols) {
        int currentVisibleCount = currentVisibleIdx.size();
        int separatorSpace = (currentVisibleCount - 1) * 3;
        int availableSpace = maxTableWidth - separatorSpace;

        if (currentVisibleCount * 5 > availableSpace) {
            while (currentVisibleCount > frozenColumns + 1
                    && (currentVisibleCount * 5 + (currentVisibleCount - 1) * 3) > maxTableWidth) {
                currentVisibleCount--;
            }
            this.pageColumns = Math.max(1, currentVisibleCount - frozenColumns);

            return determineVisibleColumnIndices(numCols);
        }

        return currentVisibleIdx;
    }

    private List<Integer> calculateScaledColumnWidths(List<Integer> visibleIdx, int maxTableWidth, int numCols) {
        int currentVisibleCount = visibleIdx.size();
        if (currentVisibleCount == 0) return new ArrayList<>(Collections.nCopies(numCols, 20));

        int separatorSpace = (currentVisibleCount - 1) * 3;
        int availableSpace = maxTableWidth - separatorSpace;
        availableSpace = Math.max(currentVisibleCount * 2, availableSpace);
        int totalOriginalVisibleWidth = 0;

        for (int idx : visibleIdx) {
            totalOriginalVisibleWidth += originalColWidths.get(idx);
        }

        double scaleFactor = (totalOriginalVisibleWidth > 0)
                ? (double) availableSpace / totalOriginalVisibleWidth
                : 1.0;
        List<Integer> newWidths = new ArrayList<>(Collections.nCopies(numCols, 20));
        int allocated = 0;

        for (int idx : visibleIdx) {
            int width = (int) Math.round(originalColWidths.get(idx) * scaleFactor);
            width = Math.max(5, width);
            allocated += width;
            newWidths.set(idx, width);
        }
        if (allocated != availableSpace) {
            int lastIdx = visibleIdx.getLast();
            int adjusted = newWidths.get(lastIdx) + (availableSpace - allocated);
            newWidths.set(lastIdx, Math.max(5, adjusted));
        }

        return newWidths;
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

    private void refreshItems(List<MenuOption> rows) {
        setItems(rows);
    }

    private List<MenuOption> buildRowsNoPaging() {
        List<MenuOption> rows = new ArrayList<>();

        if (!allHeaders.isEmpty()) {
            rows.add(new MenuOption(formatRow(allHeaders, currentColWidths, allHeaders, false), () -> {
            }, false));
            rows.add(new MenuOption(createSeparator(allHeaders.size(), currentColWidths, allHeaders), () -> {
            }, false));
        }

        for (List<String> rowData : allData) {
            rows.add(new MenuOption(formatRow(rowData, currentColWidths, allHeaders, false), () -> {
            }));
        }
        return rows;
    }

    private List<MenuOption> buildRowsWithPaging() {
        int totalCols = allHeaders.size();
        if (totalCols == 0)
            return Collections.singletonList(new MenuOption("No columns", () -> {
            }));

        int frozen = Math.min(frozenColumns, totalCols);
        int scrollable = Math.max(0, totalCols - frozen);

        if (scrollable <= 0 || pageColumns <= 0 || totalCols <= frozen + pageColumns) {
            return buildRowsNoPaging();
        }

        int maxOffset = Math.max(0, scrollable - pageColumns);
        colOffset = Math.min(colOffset, maxOffset);

        boolean isLastPage = (colOffset >= maxOffset);

        List<Integer> visibleIdx = new ArrayList<>();
        for (int i = 0; i < frozen; i++)
            visibleIdx.add(i);

        int start = frozen + colOffset;
        int endExclusive = Math.min(frozen + colOffset + pageColumns, totalCols);
        for (int i = start; i < endExclusive; i++)
            visibleIdx.add(i);

        List<String> headers = pick(allHeaders, visibleIdx);

        List<List<String>> data = new ArrayList<>();
        for (List<String> row : allData)
            data.add(pickRow(row, visibleIdx));

        List<Integer> widths = new ArrayList<>();
        for (int idx : visibleIdx) {
            widths.add(idx < currentColWidths.size() ? currentColWidths.get(idx) : 20);
        }

        if (indicatorsFullOnLastPage && isLastPage) {
            int indIdx = indexOfIgnoreCase(headers, "Indicators");
            if (indIdx >= 0) {
                widths.set(indIdx, Math.max(widths.get(indIdx), indicatorsLastPageWidth));
            }
        }

        List<MenuOption> rows = new ArrayList<>();

        if (!allHeaders.isEmpty()) {
            rows.add(new MenuOption(formatRow(headers, widths, headers, isLastPage), () -> {
            }, false));
            rows.add(new MenuOption(createSeparator(headers.size(), widths, headers), () -> {
            }, false));
        }

        for (List<String> rowData : data) {
            rows.add(new MenuOption(formatRow(rowData, widths, headers, isLastPage), () -> {
            }));
        }

        return rows;
    }

    private void pageLeft() {
        if (!pagingEnabled)
            return;

        colOffset = Math.max(0, colOffset - 1);
        if (lastWidth > 0) {
            recalculateAndRefresh(lastWidth);
        } else {
            refreshItems(buildRowsWithPaging());
        }
    }

    private void pageRight() {
        if (!pagingEnabled)
            return;

        int totalCols = allHeaders.size();
        int frozen = Math.min(frozenColumns, totalCols);
        int scrollable = Math.max(0, totalCols - frozen);
        int maxOffset = Math.max(0, scrollable - pageColumns);

        colOffset = Math.min(maxOffset, colOffset + 1);
        if (lastWidth > 0) {
            recalculateAndRefresh(lastWidth);
        } else {
            refreshItems(buildRowsWithPaging());
        }
    }

    private String formatRow(List<String> rowData, List<Integer> widths, List<String> headersForThisRow,
            boolean isLastPage) {
        if (rowData == null)
            rowData = Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rowData.size(); i++) {
            int width = i < widths.size() ? widths.get(i) : 20;
            String cell = i < rowData.size() ? rowData.get(i) : "";
            if (cell == null)
                cell = "";

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
        if (width <= 0)
            return "";

        if (cell.length() <= width)
            return cell;

        if (truncate) {
            if (width <= 3)
                return cell.substring(0, width);
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
        if (list == null || needle == null)
            return -1;
        for (int i = 0; i < list.size(); i++) {
            String v = list.get(i);
            if (v != null && v.equalsIgnoreCase(needle))
                return i;
        }
        return -1;
    }

    private static List<String> pick(List<String> src, List<Integer> idx) {
        if (src == null || src.isEmpty())
            return Collections.emptyList();
        List<String> out = new ArrayList<>(idx.size());
        for (int i : idx)
            out.add(i >= 0 && i < src.size() ? src.get(i) : "");
        return out;
    }

    private static List<String> pickRow(List<String> src, List<Integer> idx) {
        if (src == null)
            src = Collections.emptyList();
        List<String> out = new ArrayList<>(idx.size());
        for (int i : idx)
            out.add(i >= 0 && i < src.size() ? safe(src.get(i)) : "");
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}