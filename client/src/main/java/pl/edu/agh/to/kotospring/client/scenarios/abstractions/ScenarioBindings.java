package pl.edu.agh.to.kotospring.client.scenarios.abstractions;

import org.springframework.shell.component.view.control.View;
import org.springframework.shell.component.view.event.EventLoop;
import pl.edu.agh.to.kotospring.client.views.ResizingListView;

import java.util.Objects;
import java.util.function.Consumer;

public final class ScenarioBindings {

    private final EventLoop eventLoop;

    public ScenarioBindings(EventLoop eventLoop) {
        this.eventLoop = Objects.requireNonNull(eventLoop, "eventLoop must not be null");
    }


    public void onCtrlKeyWhenFocused(View focusOwner, char key, Runnable action) {
        Objects.requireNonNull(focusOwner, "focusOwner must not be null");
        Objects.requireNonNull(action, "action must not be null");

        eventLoop.onDestroy(
                eventLoop.keyEvents()
                        .subscribe(event -> {
                            if (focusOwner.hasFocus()
                                    && event.hasCtrl()
                                    && event.getPlainKey() == key) {
                                action.run();
                            }
                        })
        );
    }


    public <T> void onOpenSelectedItem(ResizingListView<?> listView, Class<T> itemClass, Consumer<T> handler) {
        Objects.requireNonNull(listView, "listView must not be null");
        Objects.requireNonNull(itemClass, "itemClass must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        eventLoop.onDestroy(
                eventLoop.viewEvents(ResizingListView.ResizingListViewOpenSelectedItemEvent.class, listView)
                        .subscribe(event -> {
                            Object item = event.args().item();
                            if (itemClass.isInstance(item)) {
                                handler.accept(itemClass.cast(item));
                            }
                        })
        );
    }
}
