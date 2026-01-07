package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.control.BoxView;
import org.springframework.shell.component.view.event.MouseEvent;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;

public class CenteredButtonView extends BoxView {

    private static final int HORIZONTAL_PADDING = 3;
    private String text;
    private Runnable action;

    public CenteredButtonView() {
        setShowBorder(false);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setAction(Runnable action) {
        this.action = action;
    }

    @Override
    protected void initInternal() {
        super.initInternal();
        // 1048580 is Enter, 32 is Space (values from ResizingListView)
        this.registerKeyBinding(1048580, this::handleAction);
        this.registerKeyBinding(32, this::handleAction);
        // 65 is Mouse Click (value from ResizingListView)
        this.registerMouseBinding(65, this::handleMouseClick);
    }

    private void handleAction() {
        if (action != null) {
            action.run();
        }
    }

    private void handleMouseClick(MouseEvent event) {
        handleAction();
    }

    @Override
    protected void drawInternal(Screen screen) {
        super.drawInternal(screen);
        Rectangle rect = getInnerRect();
        if (text == null || text.isEmpty() || rect.width() <= 0) {
            return;
        }

        int boxWidth = text.length() + (2 * HORIZONTAL_PADDING) + 2;
        int startX = rect.x() + Math.max(0, (rect.width() - boxWidth) / 2);
        int startY = rect.y() + Math.max(0, (rect.height() - 3) / 2);

        if (hasFocus()) {
            int bgColor = resolveThemeBackground("style-highlight", -1, -1);
            if (bgColor > -1) {
                Rectangle btnRect = new Rectangle(startX, startY, boxWidth, 3);
                screen.writerBuilder().build().background(btnRect, bgColor);
            }
        }

        int style = hasFocus() ? resolveThemeStyle("style-highlight", -1) : -1;
        int fgColor = hasFocus() ? resolveThemeForeground("style-highlight", -1, -1) : -1;

        Screen.Writer writer = screen.writerBuilder()
                .style(style)
                .color(fgColor)
                .build();

        writer.text("┌" + "─".repeat(boxWidth - 2) + "┐", startX, startY);
        String middle = "│" + " ".repeat(HORIZONTAL_PADDING) + text + " ".repeat(HORIZONTAL_PADDING) + "│";
        writer.text(middle, startX, startY + 1);
        writer.text("└" + "─".repeat(boxWidth - 2) + "┘", startX, startY + 2);
    }
}
