package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.control.BoxView;
import org.springframework.shell.component.view.screen.Screen;
import org.springframework.shell.geom.Rectangle;

public class SimpleMessageView extends BoxView {

    private final String message;

    public SimpleMessageView(String title, String message) {
        setTitle(title);
        setShowBorder(true);
        this.message = message;
    }

    @Override
    protected void drawInternal(Screen screen) {
        super.drawInternal(screen);
        Rectangle rect = getInnerRect();
        if (message != null && !message.isEmpty() && rect.width() > 0 && rect.height() > 0) {
            String[] lines = message.split("\n");
            int totalHeight = lines.length;
            int startY = rect.y() + (rect.height() - totalHeight) / 2;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int startX = rect.x() + (rect.width() - line.length()) / 2;
                if (startY + i >= rect.y() && startY + i < rect.y() + rect.height()) {
                    screen.writerBuilder()
                            .build()
                            .text(line, Math.max(rect.x(), startX), startY + i);
                }
            }
        }
    }
}