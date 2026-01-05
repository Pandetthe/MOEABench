package pl.edu.agh.to.kotospring.client.views;

import java.util.*;
import java.util.function.BiFunction;

import org.springframework.lang.Nullable;
import org.springframework.shell.component.message.ShellMessageBuilder;
import org.springframework.shell.component.view.control.*;
import org.springframework.shell.component.view.control.cell.ListCell;
import org.springframework.shell.component.view.event.MouseEvent;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;
import org.springframework.util.Assert;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;

public class ResizingListView<T> extends BoxView {
    private final List<T> items;
    private final List<ListCell<T>> cells;
    private final ListView.ItemStyle itemStyle;
    private int start;
    private int pos;
    private final Set<Integer> selected;
    private BiFunction<ResizingListView<T>, T, ListCell<T>> factory;

    private int rowHeight = 1;

    private boolean autoRunOnOpen = false;
    private boolean centerVertically = false;

    public ResizingListView() {
        this(ListView.ItemStyle.NOCHECK);
    }

    public ResizingListView(ListView.ItemStyle itemStyle) {
        this.items = new ArrayList<>();
        this.cells = new ArrayList<>();
        this.start = 0;
        this.pos = 0;
        this.selected = new HashSet<>();
        this.factory = (listView, item) -> ListCell.of(item, listView.getItemStyle());
        Assert.notNull(itemStyle, "item style must be set");
        this.itemStyle = itemStyle;
        this.setItems(null);
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
    }

    public void setCenterVertically(boolean centerVertically) {
        this.centerVertically = centerVertically;
    }

    public ListView.ItemStyle getItemStyle() {
        return this.itemStyle;
    }

    public void setAutoRunOnOpen(boolean autoRunOnOpen) {
        this.autoRunOnOpen = autoRunOnOpen;
    }

    @Override
    protected void initInternal() {
        super.initInternal();
        this.registerViewCommand(ViewCommand.LINE_UP, this::up);
        this.registerViewCommand(ViewCommand.LINE_DOWN, this::down);
        this.registerKeyBinding(1048576, ViewCommand.LINE_UP);
        this.registerKeyBinding(1048577, ViewCommand.LINE_DOWN);
        this.registerKeyBinding(1048580, this::enter);
        this.registerKeyBinding(32, this::space);
        this.registerMouseBinding(516, ViewCommand.LINE_UP);
        this.registerMouseBinding(1028, ViewCommand.LINE_DOWN);
        this.registerMouseBinding(65, this::click);
    }

    private void updateCells() {
        this.cells.clear();
        for (T i : this.items) {
            ListCell<T> c = this.factory.apply(this, i);
            c.setItemStyle(this.getItemStyle());
            this.cells.add(c);
        }
    }

    public void setItems(@Nullable List<T> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        if (this.items.isEmpty()) {
            this.start = -1;
            this.pos = -1;
        } else {
            this.start = 0;
            this.pos = 0;
        }
        this.updateCells();
    }

    protected void drawInternal(Screen screen) {
        if (this.start > -1 && this.pos > -1) {
            Rectangle rect = this.getInnerRect();
            int y = getStartY(rect);

            int selectedStyle = this.resolveThemeStyle("style-highlight", 1);
            int selectedForegroundColor = this.resolveThemeForeground("style-highlight", -1, -1);
            int selectedBackgroundColor = this.resolveThemeBackground("style-highlight", -1, -1);
            int i = 0;

            for (ListCell<T> c : this.cells) {
                if (i < this.start) {
                    ++i;
                } else {
                    c.setRect(rect.x(), y, rect.width(), rowHeight);
                    y += rowHeight;

                    if (i == this.start + this.pos) {
                        c.setForegroundColor(selectedForegroundColor);
                        c.setBackgroundColor(selectedBackgroundColor);
                        c.setStyle(selectedStyle);
                    } else {
                        c.setBackgroundColor(-1);
                        c.setForegroundColor(-1);
                        c.setStyle(-1);
                    }

                    c.draw(screen);
                    ++i;

                    if ((i - this.start) * rowHeight >= rect.height()) {
                        break;
                    }
                }
            }
        }
        super.drawInternal(screen);
    }

    private int getStartY(Rectangle rect) {
        int y = rect.y();
        if (centerVertically) {
            int contentHeight = items.size() * rowHeight;
            if (contentHeight < rect.height()) {
                y += (rect.height() - contentHeight) / 2;
            }
        }
        return y;
    }

    public void setCellFactory(BiFunction<ResizingListView<T>, T, ListCell<T>> factory) {
        Assert.notNull(factory, "cell factory must be set");
        this.factory = factory;
    }

    private void scrollIndex(boolean up) {
        int size = this.items.size();
        int maxItems = Math.max(1, this.getInnerRect().height() / rowHeight);

        int active = this.start + this.pos;
        if (up) {
            if (this.start > 0 && this.pos == 0) {
                --this.start;
            } else if (this.start + this.pos <= 0) {
                this.start = size - Math.min(maxItems, size);
                this.pos = Math.min(maxItems, size) - 1;
            } else {
                --this.pos;
            }
        } else if (this.start + this.pos + 1 < Math.min(this.start + maxItems, size)) {
            ++this.pos;
        } else if (this.start + this.pos + 1 >= size) {
            this.start = 0;
            this.pos = 0;
        } else {
            if (this.start + maxItems < size) {
                this.start++;
            } else {
                if (active < size - 1) {
                    this.pos++;
                } else {
                    this.start = 0;
                    this.pos = 0;
                }
            }
        }
    }

    private void scrollIndex(int step) {
        if (this.start >= 0 || this.pos >= 0) {
            if (step < 0) {
                for (int i = step; i < 0; ++i) this.scrollIndex(true);
            } else if (step > 0) {
                for (int i = step; i > 0; --i) this.scrollIndex(false);
            }
        }
    }

    private void up() { this.scrollIndex(-1); }
    private void down() { this.scrollIndex(1); }

    private T selectedItem() {
        int active = this.start + this.pos;
        if (active >= 0 && active < this.items.size()) {
            return this.items.get(active);
        }
        return null;
    }

    private void enter() {
        if (this.itemStyle == ListView.ItemStyle.NOCHECK) {
            openSelected();
        }
    }

    private void space() {
        updateSelectionStates();
    }

    private void openSelected() {
        T item = selectedItem();
        if (item == null) return;

        if (autoRunOnOpen && item instanceof ExperimentOption opt && opt.action() != null) {
            opt.action().run();
            return;
        }
        this.dispatch(ShellMessageBuilder.ofView(this,
                ResizingListViewOpenSelectedItemEvent.of(this, item)));
    }

    private void updateSelectionStates() {
        int active = this.start + this.pos;
        if (this.itemStyle == ListView.ItemStyle.CHECKED) {
            boolean removed = this.selected.remove(active);
            if (!removed) this.selected.add(active);
        } else if (this.itemStyle == ListView.ItemStyle.RADIO) {
            this.selected.clear();
            this.selected.add(active);
        }
        ListIterator<ListCell<T>> iter = this.cells.listIterator();
        while (iter.hasNext()) {
            int index = iter.nextIndex();
            ListCell<T> c = iter.next();
            c.setSelected(this.selected.contains(index));
        }
    }

    private void click(MouseEvent event) {
        Rectangle rect = this.getInnerRect();
        int yStart = getStartY(rect);

        if (event.y() < yStart) {
            return;
        }

        int index = (event.y() - yStart) / rowHeight;
        int active = this.start + index;

        if (active >= 0 && active < this.items.size()) {
            this.pos = index;
            if (this.itemStyle == ListView.ItemStyle.NOCHECK) {
                openSelected();
                return;
            }
        }
        this.updateSelectionStates();
    }

    public record ResizingListViewItemEventArgs<T>(T item) implements ViewEventArgs {
        public static <T> ResizingListViewItemEventArgs<T> of(T item) {
            return new ResizingListViewItemEventArgs<>(item);
        }
    }

    public record ResizingListViewOpenSelectedItemEvent<T>(View view, ResizingListViewItemEventArgs<T> args) implements ViewEvent {
        public static <T> ResizingListViewOpenSelectedItemEvent<T> of(View view, T item) {
            return new ResizingListViewOpenSelectedItemEvent<>(view, ResizingListViewItemEventArgs.of(item));
        }
    }
}