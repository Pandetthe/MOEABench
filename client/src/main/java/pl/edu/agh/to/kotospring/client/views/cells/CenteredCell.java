package pl.edu.agh.to.kotospring.client.views.cells;

import org.springframework.shell.component.view.control.cell.AbstractListCell;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;

public class CenteredCell extends AbstractListCell<String> {

    public CenteredCell(String item) {
        super(item);
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

        String text = getItem();
        if (text == null) text = "";

        int freeSpace = rect.width() - text.length();
        int startX = rect.x() + Math.max(0, freeSpace / 2);
        int startY = rect.y();

        if (isSelected()) {
            int bgColor = resolveThemeBackground(getBackgroundStyle(), getBackgroundColor(), -1);
            if (bgColor > -1) {
                screen.writerBuilder().build().background(rect, bgColor);
            }
        }
        writer.text(text, startX, startY);
    }
}