package controllers.group;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Group;
import services.GroupService;

import java.sql.SQLException;

public class EditGroupController {
    @FXML
    private TextField capacityField;

    @FXML
    private TextField groupPhotoField;

    @FXML
    private TextField categoryField;

    @FXML
    private Button saveButton;

    private final GroupService groupService = new GroupService();
    private Runnable onSaved;
    private Runnable onDone;
    private Runnable onCancel;
    private Group group;

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setGroup(Group group) {
        this.group = group;
        render();
    }

    @FXML
    public void initialize() {
        render();
    }

    private void render() {
        if (group == null || capacityField == null) {
            return;
        }
        capacityField.setText(String.valueOf(group.getCapacity()));
        groupPhotoField.setText(group.getGroupPhoto() == null ? "" : group.getGroupPhoto());
        categoryField.setText(group.getCategory() == null ? "" : group.getCategory());
    }

    @FXML
    private void saveChanges() {
        if (group == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun groupe selectionne.");
            return;
        }

        String capacityText = safeTrim(capacityField.getText());
        String groupPhoto = safeTrim(groupPhotoField.getText());
        String category = safeTrim(categoryField.getText());

        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation", "La capacite doit etre un nombre.");
            return;
        }

        if (capacity <= 0) {
            showAlert(Alert.AlertType.ERROR, "Validation", "La capacite doit etre > 0.");
            return;
        }

        if (category.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "La categorie est requise.");
            return;
        }

        group.setCapacity(capacity);
        group.setGroupPhoto(groupPhoto.isEmpty() ? null : groupPhoto);
        group.setCategory(category);
        // Keep creator id as-is; if it is unset (<= 0) let GroupService resolve it appropriately.
        if (group.getCreatorId() <= 0) {
            group.setCreatorId(0);
        }

        try {
            groupService.modifier(group);
            if (onSaved != null) {
                onSaved.run();
            }
            if (onDone != null) {
                onDone.run();
            } else {
                closeWindow();
            }
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible: " + e.getMessage());
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
        if (saveButton == null || saveButton.getScene() == null) {
            return;
        }
        Stage stage = (Stage) saveButton.getScene().getWindow();
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
