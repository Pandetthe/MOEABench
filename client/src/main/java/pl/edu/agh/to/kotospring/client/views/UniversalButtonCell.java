package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.control.cell.AbstractListCell;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;

import java.util.function.Function;

public class UniversalButtonCell<T> extends AbstractListCell<T> {
    private static final int HORIZONTAL_PADDING = 3;

    private final Function<T, String> nameProvider;

    public UniversalButtonCell(T item, Function<T, String> nameProvider) {
        super(item);
        this.nameProvider = nameProvider;
    }

    @Override
    public void draw(Screen screen) {
        drawContent(screen);
    }

    @Override
    protected void drawBackground(Screen screen) {
    }

    @Override
    protected void drawContent(Screen screen) {
        Rectangle rect = getRect();
        Screen.Writer writer = screen.writerBuilder()
                .style(getStyle())
                .color(getForegroundColor())
                .build();

        String text = nameProvider.apply(getItem());

        int boxWidth = text.length() + (2 * HORIZONTAL_PADDING) + 2;

        int startX = rect.x() + Math.max(0, (rect.width() - boxWidth) / 2);
        int startY = rect.y();

        if (isSelected()) {
            int bgColor = resolveThemeBackground(getBackgroundStyle(), getBackgroundColor(), -1);
            if (bgColor > -1) {
                Rectangle btnRect = new Rectangle(startX, startY, boxWidth, 3);
                screen.writerBuilder().build().background(btnRect, bgColor);
            }
        }

        writer.text("┌" + "─".repeat(boxWidth - 2) + "┐", startX, startY);

        String middle = "│" + " ".repeat(HORIZONTAL_PADDING) + text + " ".repeat(HORIZONTAL_PADDING) + "│";
        writer.text(middle, startX, startY + 1);

        writer.text("└" + "─".repeat(boxWidth - 2) + "┘", startX, startY + 2);
    }
}