package controllers.group;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Group;
import services.GroupService;
import utils.SessionManager;

import java.sql.SQLException;

public class AddGroupController {
    @FXML
    private TextField capacityField;

    @FXML
    private TextField groupPhotoField;

    @FXML
    private TextField categoryField;

    @FXML
    private Button addButton;

    private final GroupService groupService = new GroupService();
    private Runnable onSaved;
    private Runnable onDone;
    private Runnable onCancel;

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @FXML
    public void initialize() {
        // No-op: this screen matches the Symfony form (category + capacity + photo URL).
    }

    @FXML
    private void addGroup() {
        String capacityText = safeTrim(capacityField.getText());
        String groupPhoto = safeTrim(groupPhotoField.getText());
        String category = safeTrim(categoryField.getText());

        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Capacity must be a number.");
            return;
        }

        if (capacity <= 0) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Capacity must be > 0.");
            return;
        }

        if (category.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Category is required.");
            return;
        }

        Group group = new Group();
        group.setCapacity(capacity);
        group.setGroupPhoto(groupPhoto.isEmpty() ? null : groupPhoto);
        group.setCategory(category);
        // Creator id comes from the logged-in user/session.
        // If session is missing, leave it unset (<= 0) so GroupService can resolve a valid user id (or NULL if allowed).
        group.setCreatorId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0);

        try {
            groupService.ajouter(group);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Group added successfully.");
            if (onSaved != null) {
                onSaved.run();
            }
            if (onDone != null) {
                onDone.run();
            } else {
                closeWindow();
            }
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add group: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        } else {
            closeWindow();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) addButton.getScene().getWindow();
        stage.close();
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
