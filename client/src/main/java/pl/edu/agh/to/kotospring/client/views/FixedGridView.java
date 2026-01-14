package pl.edu.agh.to.kotospring.client.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.component.view.control.BoxView;
import org.springframework.shell.component.view.control.View;
import org.springframework.shell.component.view.control.ViewCommand;
import org.springframework.shell.component.view.event.KeyEvent;
import org.springframework.shell.component.view.event.KeyHandler;
import org.springframework.shell.component.view.event.MouseHandler;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;
import org.springframework.util.Assert;

public class FixedGridView extends BoxView {
    private static final Logger log = LoggerFactory.getLogger(FixedGridView.class);
    private final List<GridItem> gridItems = new ArrayList<>();
    private int[] columnSize;
    private int[] rowSize;
    private int minWidth;
    private int minHeight;
    private int gapRows;
    private int gapColumns;
    private int rowOffset;
    private int columnOffset;
    private boolean showBorders;

    public FixedGridView() {
    }

    public FixedGridView setColumnSize(int... columns) {
        this.columnSize = columns;
        return this;
    }

    public FixedGridView setRowSize(int... rows) {
        this.rowSize = rows;
        return this;
    }

    public FixedGridView setMinSize(int minWidth, int minHeight) {
        Assert.state(minWidth > -1 || minHeight > -1, "Minimum sizes for rows or colums cannot be negative");
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        return this;
    }

    public FixedGridView addItem(View view, int row, int column, int rowSpan, int colSpan, int minGridHeight,
            int minGridWidth) {
        GridItem gridItem = new GridItem(view, row, column, colSpan, rowSpan, minGridHeight, minGridWidth, false);
        this.gridItems.add(gridItem);

        if (view instanceof ResizingListView<?> resizable) {
            resizable.setOnBoundaryNext(this::nextView);
            resizable.setOnBoundaryPrev(this::prevView);
        }

        return this;
    }

    public void clearItems() {
        this.gridItems.clear();
    }

    public void setShowBorders(boolean showBorders) {
        this.showBorders = showBorders;
    }

    public boolean isShowBorders() {
        return this.showBorders;
    }

    @Override
    public void focus(View view, boolean focused) {
        super.focus(view, focused);
        if (focused && !gridItems.isEmpty()) {
            GridItem focusItem = gridItems.get(0);
            this.getViewService().setFocus(focusItem.view);
        }
    }

    public MouseHandler getMouseHandler() {
        log.trace("getMouseHandler()");
        return (args) -> {
            View focus = null;

            for (GridItem i : this.gridItems) {
                MouseHandler.MouseHandlerResult r = i.view.getMouseHandler().handle(args);
                if (r.focus() != null) {
                    focus = r.focus();
                    break;
                }
            }

            return MouseHandler.resultOf(args.event(), true, focus, (View) null);
        };
    }

    private void nextView() {
        View toFocus = null;
        boolean found = false;

        for (GridItem i : this.gridItems) {
            if (i.visible) {
                if (toFocus == null) {
                    toFocus = i.view;
                }

                if (found) {
                    toFocus = i.view;
                    break;
                }

                if (i.view.hasFocus()) {
                    found = true;
                }
            }
        }

        if (toFocus != null) {
            this.getViewService().setFocus(toFocus);
        }

    }

    private void prevView() {
        View toFocus = null;
        View lastVisible = null;

        for (GridItem i : this.gridItems) {
            if (i.visible) {
                if (i.view.hasFocus()) {
                    toFocus = lastVisible;
                    break;
                }
                lastVisible = i.view;
            }
        }

        if (toFocus == null) {
            for (int j = gridItems.size() - 1; j >= 0; --j) {
                GridItem i = gridItems.get(j);
                if (i.visible) {
                    toFocus = i.view;
                    break;
                }
            }
        }

        if (toFocus != null) {
            this.getViewService().setFocus(toFocus);
        }
    }

    protected void initInternal() {
        this.registerViewCommand(ViewCommand.NEXT_VIEW, this::nextView);
        this.registerKeyBinding(KeyEvent.Key.CursorDown, ViewCommand.NEXT_VIEW);
        this.registerKeyBinding(KeyEvent.Key.Tab, ViewCommand.NEXT_VIEW);
        this.registerViewCommand("PREV_VIEW", this::prevView);
        this.registerKeyBinding(KeyEvent.Key.CursorUp, "PREV_VIEW");
        this.registerKeyBinding(KeyEvent.Key.Backtab, "PREV_VIEW");
    }

    public KeyHandler getKeyHandler() {
        log.trace("getKeyHandler()");
        KeyHandler handler = null;

        for (GridItem i : this.gridItems) {
            if (i.view.hasFocus()) {
                handler = i.view.getKeyHandler();
                break;
            }
        }

        return handler != null ? handler.thenIfNotConsumed(super.getKeyHandler()) : super.getKeyHandler();
    }

    public KeyHandler getHotKeyHandler() {
        log.trace("getHotKeyHandler()");
        KeyHandler handler = null;

        for (GridItem i : this.gridItems) {
            if (handler == null) {
                handler = i.view.getHotKeyHandler();
            } else {
                handler = handler.thenIfNotConsumed(i.view.getHotKeyHandler());
            }
        }

        if (handler != null) {
            return handler.thenIfNotConsumed(super.getHotKeyHandler());
        } else {
            return super.getHotKeyHandler();
        }
    }

    public boolean hasFocus() {
        for (GridItem i : this.gridItems) {
            if (i.view.hasFocus()) {
                return true;
            }
        }

        return super.hasFocus();
    }

    protected void drawInternal(Screen screen) {
        super.drawInternal(screen);
        Rectangle rect = this.getInnerRect();
        int x = rect.x();
        int y = rect.y();
        int width = rect.width();
        int height = rect.height();
        Map<View, GridItem> items = new HashMap<>();

        for (GridItem item : this.gridItems) {
            item.visible = false;
            if (item.width > 0 && item.height > 0 && width >= item.minGridWidth && height >= item.minGridHeight) {
                GridItem previousItem = (GridItem) items.get(item.view);
                if (previousItem == null || item.minGridWidth >= previousItem.minGridWidth
                        || item.minGridHeight >= previousItem.minGridHeight) {
                    items.put(item.view, item);
                }
            }
        }

        int rows = this.rowSize.length;
        int columns = this.columnSize.length;

        for (GridItem item : items.values()) {
            int rowEnd = item.row + item.height;
            if (rowEnd > rows) {
                rows = rowEnd;
            }

            int columnEnd = item.column + item.width;
            if (columnEnd > columns) {
                columns = columnEnd;
            }
        }

        if (rows != 0 && columns != 0) {
            int[] rowPos = new int[rows];
            int[] rowHeight = new int[rows];
            int[] columnPos = new int[columns];
            int[] columnWidth = new int[columns];
            int remainingWidth = width;
            int remainingHeight = height;
            int proportionalWidth = 0;
            int proportionalHeight = 0;

            for (int index = 0; index < this.rowSize.length; ++index) {
                int row = this.rowSize[index];
                if (row > 0) {
                    if (row < this.minHeight) {
                        row = this.minHeight;
                    }

                    remainingHeight -= row;
                    rowHeight[index] = row;
                } else if (row == 0) {
                    ++proportionalHeight;
                } else {
                    proportionalHeight += -row;
                }
            }

            for (int index = 0; index < this.columnSize.length; ++index) {
                int column = this.columnSize[index];
                if (column > 0) {
                    if (column < this.minWidth) {
                        column = this.minWidth;
                    }

                    remainingWidth -= column;
                    columnWidth[index] = column;
                } else if (column == 0) {
                    ++proportionalWidth;
                } else {
                    proportionalWidth += -column;
                }
            }

            if (this.isShowBorders()) {
                remainingHeight -= rows + 1;
                remainingWidth -= columns + 1;
            } else {
                remainingHeight -= (rows - 1) * this.gapRows;
                remainingWidth -= (columns - 1) * this.gapColumns;
            }

            if (rows > this.rowSize.length) {
                proportionalHeight += rows - this.rowSize.length;
            }

            if (columns > this.columnSize.length) {
                proportionalWidth += columns - this.columnSize.length;
            }

            for (int index = 0; index < rows; ++index) {
                int row = 0;
                if (index < this.rowSize.length) {
                    row = this.rowSize[index];
                }

                if (row <= 0) {
                    if (row == 0) {
                        row = 1;
                    } else {
                        row = -row;
                    }

                    int rowAbs = row * remainingHeight / proportionalHeight;
                    remainingHeight -= rowAbs;
                    proportionalHeight -= row;
                    if (rowAbs < this.minHeight) {
                        rowAbs = this.minHeight;
                    }

                    rowHeight[index] = rowAbs;
                }
            }

            for (int index = 0; index < columns; ++index) {
                int column = 0;
                if (index < this.columnSize.length) {
                    column = this.columnSize[index];
                }

                if (column <= 0) {
                    if (column == 0) {
                        column = 1;
                    } else {
                        column = -column;
                    }

                    int columnAbs = column * remainingWidth / proportionalWidth;
                    remainingWidth -= columnAbs;
                    proportionalWidth -= column;
                    if (columnAbs < this.minWidth) {
                        columnAbs = this.minWidth;
                    }

                    columnWidth[index] = columnAbs;
                }
            }

            int columnX = 0;
            int rowY = 0;
            if (this.isShowBorders()) {
                ++columnX;
                ++rowY;
            }

            for (int index = 0; index < rowHeight.length; ++index) {
                int row = rowHeight[index];
                rowPos[index] = rowY;
                int gap = this.gapRows;
                if (this.isShowBorders()) {
                    gap = 1;
                }

                rowY += row + gap;
            }

            for (int index = 0; index < columnWidth.length; ++index) {
                int column = columnWidth[index];
                columnPos[index] = columnX;
                int gap = this.gapColumns;
                if (this.isShowBorders()) {
                    gap = 1;
                }

                columnX += column + gap;
            }

            GridItem focus = null;

            for (Map.Entry<View, GridItem> entry : items.entrySet()) {
                View primitive = (View) entry.getKey();
                GridItem item = (GridItem) entry.getValue();
                int px = columnPos[item.column];
                int py = rowPos[item.row];
                int pw = 0;
                int ph = 0;

                for (int index = 0; index < item.height; ++index) {
                    ph += rowHeight[item.row + index];
                }

                for (int index = 0; index < item.width; ++index) {
                    pw += columnWidth[item.column + index];
                }

                if (this.isShowBorders()) {
                    pw += item.width - 1;
                    ph += item.height - 1;
                } else {
                    pw += (item.width - 1) * this.gapColumns;
                    ph += (item.height - 1) * this.gapRows;
                }

                item.x = px;
                item.y = py;
                item.w = pw;
                item.h = ph;
                item.visible = true;
                if (primitive.hasFocus()) {
                    focus = item;
                }
            }

            int offsetX = 0;
            int offsetY = 0;
            int add = 1;
            if (!this.isShowBorders()) {
                add = this.gapRows;
            }

            for (int index = 0; index < rowHeight.length; ++index) {
                int height2 = rowHeight[index];
                if (index >= this.rowOffset) {
                    break;
                }

                offsetY += height2 + add;
            }

            if (!this.isShowBorders()) {
                add = this.gapColumns;
            }

            for (int index = 0; index < columnWidth.length; ++index) {
                int width2 = columnWidth[index];
                if (index >= this.columnOffset) {
                    break;
                }

                offsetX += width2 + add;
            }

            if (focus != null) {
                if (focus.y + focus.h - offsetY >= height) {
                    offsetY = focus.y - height + focus.h;
                }

                if (focus.y - offsetY < 0) {
                    offsetY = focus.y;
                }

                if (focus.x + focus.w - offsetX >= width) {
                    offsetX = focus.x - width + focus.w;
                }

                if (focus.x - offsetX < 0) {
                    offsetX = focus.x;
                }
            }

            int from = 0;
            int to = 0;

            for (int index = 0; index < rowPos.length; ++index) {
                int pos = rowPos[index];
                if (pos - offsetY < 0) {
                    from = index + 1;
                }

                if (pos - offsetY < height) {
                    to = index;
                }
            }

            if (this.rowOffset < from) {
                this.rowOffset = from;
            }

            if (this.rowOffset > to) {
                this.rowOffset = to;
            }

            from = 0;
            to = 0;

            for (int index = 0; index < columnPos.length; ++index) {
                int pos = columnPos[index];
                if (pos - offsetX < 0) {
                    from = index + 1;
                }

                if (pos - offsetX < width) {
                    to = index;
                }
            }

            if (this.columnOffset < from) {
                this.columnOffset = from;
            }

            if (this.columnOffset > to) {
                this.columnOffset = to;
            }

            for (Map.Entry<View, GridItem> entry : items.entrySet()) {
                View view = (View) entry.getKey();
                GridItem item = (GridItem) entry.getValue();
                if (item.visible) {
                    item.x -= offsetX;
                    item.y -= offsetY;
                    if (item.x < width && item.x + item.w > 0 && item.y < height && item.y + item.h > 0) {
                        if (item.x + item.w > width) {
                            item.w = width - item.x;
                        }

                        if (item.y + item.h > height) {
                            item.h = height - item.y;
                        }

                        if (item.x < 0) {
                            item.w += item.x;
                            item.x = 0;
                        }

                        if (item.y < 0) {
                            item.h += item.y;
                            item.y = 0;
                        }

                        if (item.w > 0 && item.h > 0) {
                            item.x += x;
                            item.y += y;
                            view.setRect(item.x, item.y, item.w, item.h);
                            view.draw(screen);
                            if (this.isShowBorders()) {
                                screen.writerBuilder().build().border(item.x - 1, item.y - 1, item.w + 2, item.h + 2);
                            }
                        } else {
                            item.visible = false;
                        }
                    } else {
                        item.visible = false;
                    }
                }
            }

        }
    }

    private static class GridItem {
        View view;
        int row;
        int column;
        int width;
        int height;
        int minGridHeight;
        int minGridWidth;
        boolean visible;
        int x;
        int y;
        int w;
        int h;

        GridItem(View view, int row, int column, int width, int height, int minGridHeight, int minGridWidth,
                boolean visible) {
            this.view = view;
            this.row = row;
            this.column = column;
            this.width = width;
            this.height = height;
            this.minGridHeight = minGridHeight;
            this.minGridWidth = minGridWidth;
            this.visible = visible;
        }
    }
}
