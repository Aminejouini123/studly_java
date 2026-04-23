package controllers.group;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import models.Group;
import services.GroupService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class GroupsDashboardController {
    @FXML
    private Label groupsTotalLabel;

    @FXML
    private Label assignedLabel;

    @FXML
    private Label freeLabel;

    @FXML
    private Label totalCapacityLabel;

    @FXML
    private StackPane groupContentHost;

    private final GroupService groupService = new GroupService();

    @FXML
    public void initialize() {
        showGroupsTable();
        refreshStats();
    }

    @FXML
    public void showGroupsTable() {
        Parent view = loadView("/gestion_group/list_group.fxml");
        groupContentHost.getChildren().setAll(view);
        refreshStats();
    }

    @FXML
    public void showAddGroupForm() {
        try {
            URL resource = getClass().getResource("/gestion_group/add_group.fxml");
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: /gestion_group/add_group.fxml");
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            AddGroupController controller = loader.getController();
            controller.setOnSaved(this::refreshStats);
            controller.setOnDone(this::showGroupsTable);
            controller.setOnCancel(this::showGroupsTable);
            groupContentHost.getChildren().setAll(view);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load add group view.", e);
        }
    }

    private void refreshStats() {
        try {
            List<Group> groups = groupService.recuperer();
            int totalGroups = groups.size();
            int totalCapacity = groups.stream().mapToInt(Group::getCapacity).sum();

            // The Symfony UI shows: total / assigned / free / total capacity.
            // If you later add a real "status" or membership count, wire it here.
            int assigned = 0;
            int free = totalGroups;

            groupsTotalLabel.setText(String.valueOf(totalGroups));
            assignedLabel.setText(String.valueOf(assigned));
            freeLabel.setText(String.valueOf(free));
            totalCapacityLabel.setText(String.valueOf(totalCapacity));
        } catch (SQLException | RuntimeException e) {
            groupsTotalLabel.setText("0");
            assignedLabel.setText("0");
            freeLabel.setText("0");
            totalCapacityLabel.setText("0");
        }
    }

    private Parent loadView(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: " + resourcePath);
            }
            return FXMLLoader.load(resource);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load FXML resource: " + resourcePath, e);
        }
    }
}
