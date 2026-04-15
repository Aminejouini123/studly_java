package controllers.group;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import models.Group;
import services.GroupService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GroupsListController {
    @FXML
    private ListView<Group> groupsListView;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        groupsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                    return;
                }
                setText(group.getCategory() + " (capacity: " + group.getCapacity() + ")");
            }
        });

        groupsListView.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
                return;
            }

            Group selected = groupsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openGroupDetails(selected);
            }
        });

        loadGroups();
    }

    private void loadGroups() {
        errorLabel.setText("");

        List<Group> groups = new ArrayList<>();
        try {
            groups = new GroupService().recuperer();
        } catch (SQLException | RuntimeException e) {
            errorLabel.setText("Unable to load groups from database. Showing sample data.");
            groups = sampleGroups();
        }

        ObservableList<Group> items = FXCollections.observableArrayList(groups);
        groupsListView.setItems(items);
    }

    @FXML
    private void openSelectedGroup() {
        Group selected = groupsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openGroupDetails(selected);
        }
    }

    private void openGroupDetails(Group group) {
        try {
            URL resource = getClass().getResource("/gestion_group/group_details.fxml");
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: /gestion_group/group_details.fxml");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            GroupDetailsController controller = loader.getController();
            controller.setGroup(group);
            controller.setOnBack(this::reloadList);

            StackPane host = findContentHost(groupsListView);
            host.getChildren().setAll(view);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load group details view.", e);
        }
    }

    private void reloadList() {
        try {
            URL resource = getClass().getResource("/gestion_group/groups_list.fxml");
            if (resource == null) {
                return;
            }
            Parent view = FXMLLoader.load(resource);
            StackPane host = findContentHost(groupsListView);
            host.getChildren().setAll(view);
        } catch (IOException ignored) {
        }
    }

    private static StackPane findContentHost(Node node) {
        Parent current = node.getParent();
        while (current != null) {
            if (current instanceof StackPane) {
                StackPane stackPane = (StackPane) current;
                if ("contentHost".equals(stackPane.getId())) {
                    return stackPane;
                }
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to find #contentHost parent.");
    }

    private static List<Group> sampleGroups() {
        List<Group> list = new ArrayList<>();
        list.add(new Group(1, 20, null, "Math", 1));
        list.add(new Group(2, 15, null, "Programming", 1));
        list.add(new Group(3, 30, null, "Physics", 2));
        return list;
    }
}
