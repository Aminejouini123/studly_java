package controllers.group;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import models.Group;
import models.Invitation;
import services.GroupService;
import services.InvitationService;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final InvitationService invitationService = new InvitationService();

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
            if (groupContentHost == null) {
                showAlert(Alert.AlertType.ERROR, "Navigation", "Zone d'affichage introuvable (groupContentHost).");
                return;
            }
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
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void refreshStats() {
        try {
            models.User current = SessionManager.getCurrentUser();
            List<Group> all = groupService.recuperer();
            if (current == null) {
                int totalGroups = all.size();
                int totalCapacity = all.stream().mapToInt(Group::getCapacity).sum();
                groupsTotalLabel.setText(String.valueOf(totalGroups));
                assignedLabel.setText("0");
                freeLabel.setText(String.valueOf(totalGroups));
                totalCapacityLabel.setText(String.valueOf(totalCapacity));
                return;
            }

            Set<Integer> acceptedGroupIds = invitationService.recuperer().stream()
                    .filter(i -> i.getReceiver_id() == current.getId())
                    .filter(i -> isAccepted(i))
                    .map(Invitation::getGroup_id)
                    .collect(Collectors.toSet());

            List<Group> visible = all.stream()
                    .filter(g -> g != null)
                    .filter(g -> groupService.isGroupCreator(g, current) || acceptedGroupIds.contains(g.getId()))
                    .collect(Collectors.toList());

            int totalGroups = visible.size();
            int totalCapacity = visible.stream().mapToInt(Group::getCapacity).sum();

            int createdByMe = (int) visible.stream().filter(g -> groupService.isGroupCreator(g, current)).count();
            int joined = totalGroups - createdByMe;

            groupsTotalLabel.setText(String.valueOf(totalGroups));
            assignedLabel.setText(String.valueOf(createdByMe));
            freeLabel.setText(String.valueOf(joined));
            totalCapacityLabel.setText(String.valueOf(totalCapacity));
        } catch (SQLException | RuntimeException e) {
            groupsTotalLabel.setText("0");
            assignedLabel.setText("0");
            freeLabel.setText("0");
            totalCapacityLabel.setText("0");
        }
    }

    private static boolean isAccepted(Invitation inv) {
        if (inv == null || inv.getStatus() == null) {
            return false;
        }
        String s = inv.getStatus().trim().toUpperCase(Locale.ROOT);
        return s.contains("ACCEPTED");
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

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
