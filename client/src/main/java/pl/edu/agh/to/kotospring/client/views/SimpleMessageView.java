package pl.edu.agh.to.kotospring.client.views;

import org.springframework.shell.component.view.control.GridView;
import pl.edu.agh.to.kotospring.client.models.ExperimentOption;

import java.util.ArrayList;
import java.util.List;

public class SimpleMessageView extends GridView {

    private final ResizingListView<ExperimentOption> contentList;

    public SimpleMessageView(String title, String message) {
        setTitle(title);
        setShowBorder(true);
        setRowSize(0, 10);
        setColumnSize(0);

        contentList = new ResizingListView<>();
        contentList.setShowBorder(false);
        contentList.setRowHeight(1);
        contentList.setCellFactory((list, item) -> new SimpleTextCell(item));

        List<ExperimentOption> items = new ArrayList<>();
        items.add(new ExperimentOption(" ", () -> {}));
        items.add(new ExperimentOption(" " + message, () -> {}));
        items.add(new ExperimentOption(" ", () -> {}));


        contentList.setItems(items);
        addItem(contentList, 0, 0, 1, 1, 0, 0);
    }

    public ResizingListView<ExperimentOption> getContentList() {
        return contentList;
    }

}