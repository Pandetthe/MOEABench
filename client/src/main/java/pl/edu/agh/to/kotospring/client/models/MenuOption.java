package pl.edu.agh.to.kotospring.client.models;

public record MenuOption(String name, Runnable action, boolean selectable) implements SelectableItem {
    public MenuOption(String name, Runnable action) {
        this(name, action, true);
    }

    @Override
    public boolean isSelectable() {
        return selectable;
    }
}
