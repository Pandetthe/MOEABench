package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.control.cell.AbstractListCell;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;

public class SimpleTextCell extends AbstractListCell<ExperimentOption> {

    public SimpleTextCell(ExperimentOption item) {
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
        String text = getItem().name();

        if (isSelected()) {
            int bgColor = resolveThemeBackground(getBackgroundStyle(), getBackgroundColor(), -1);
            if (bgColor > -1) {
                screen.writerBuilder().build().background(rect, bgColor);
            }
        }
        screen.writerBuilder()
                .style(getStyle())
                .color(getForegroundColor())
                .build()
                .text(" " + text, rect.x(), rect.y());
    }
}