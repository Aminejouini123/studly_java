package controllers.activities;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.Activity;
import models.Course;
import services.ActivityService;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ActivityAddController extends BaseActivityController {

    @FXML private TextField titleField, durationField, linkField, fileField;
    @FXML private ComboBox<String> typeComboBox, statusComboBox, difficultyComboBox, levelComboBox;
    @FXML private TextArea descriptionArea, instructionsArea, outputArea, hintsArea;
    @FXML private Button addBtn;
    @FXML private Label titleError, typeError, statusError, durationError, difficultyError, levelError, descriptionError, instructionsError, outputError, hintsError;

    private Course currentCourse;
    private final ActivityService activityService = new ActivityService();

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll("Workshop", "Assignment", "Practical Work", "Course Material");
        statusComboBox.getItems().setAll("To Do", "In Progress", "Completed");
        difficultyComboBox.getItems().setAll("Easy", "Medium", "Hard");
        levelComboBox.getItems().setAll("Beginner", "Intermediate", "Advanced");
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
    }

    @FXML
    private void handleAddActivity() {
        if (!validateInput()) return;

        if (currentCourse == null) {
            showAlert("System Error", "No course context found. Please restart the activity flow.");
            return;
        }

        try {
            Activity activity = new Activity(
                titleField.getText(),
                descriptionArea.getText(),
                fileField.getText(),
                linkField.getText(),
                Integer.parseInt(durationField.getText()),
                statusComboBox.getValue(),
                difficultyComboBox.getValue(),
                levelComboBox.getValue(),
                typeComboBox.getValue(),
                instructionsArea.getText(),
                outputArea.getText(),
                hintsArea.getText(),
                new Timestamp(System.currentTimeMillis()), 
                currentCourse.getId(),
                1 
            );

            activityService.ajouter(activity);
            if (fromBackend) {
                returnToDashboard(addBtn);
            } else {
                navigateToActivityList(addBtn, currentCourse);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not save activity: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        if (fromBackend) {
            returnToDashboard(addBtn);
            return;
        }
        navigateToActivityList(addBtn, currentCourse);
    }


    private void hideErrors() {
        if (titleError != null) {
            titleError.setVisible(false); titleError.setManaged(false); titleField.setStyle("");
            typeError.setVisible(false); typeError.setManaged(false); typeComboBox.setStyle("");
            statusError.setVisible(false); statusError.setManaged(false); statusComboBox.setStyle("");
            difficultyError.setVisible(false); difficultyError.setManaged(false); difficultyComboBox.setStyle("");
            levelError.setVisible(false); levelError.setManaged(false); levelComboBox.setStyle("");
            descriptionError.setVisible(false); descriptionError.setManaged(false); descriptionArea.setStyle("");
            instructionsError.setVisible(false); instructionsError.setManaged(false); instructionsArea.setStyle("");
            outputError.setVisible(false); outputError.setManaged(false); outputArea.setStyle("");
            hintsError.setVisible(false); hintsError.setManaged(false); hintsArea.setStyle("");
        }
    }

    private void showError(Label errorLabel, javafx.scene.control.Control field, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        field.setStyle("-fx-border-color: transparent transparent #ef4444 transparent; -fx-border-width: 0 0 2 0;");
    }

    private boolean validateInput() {
        hideErrors();
        boolean isValid = true;
        
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showError(titleError, titleField, "Please provide an activity title.");
            isValid = false;
        }
        if (typeComboBox.getValue() == null) {
            showError(typeError, typeComboBox, "Please select an activity type.");
            isValid = false;
        }
        if (statusComboBox.getValue() == null) {
            showError(statusError, statusComboBox, "Please select a status for the activity.");
            isValid = false;
        }
        if (difficultyComboBox.getValue() == null) {
            showError(difficultyError, difficultyComboBox, "Please select a difficulty level.");
            isValid = false;
        }
        if (levelComboBox.getValue() == null) {
            showError(levelError, levelComboBox, "Please select a target level.");
            isValid = false;
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            showError(descriptionError, descriptionArea, "Please provide a description.");
            isValid = false;
        }
        if (instructionsArea.getText() == null || instructionsArea.getText().trim().isEmpty()) {
            showError(instructionsError, instructionsArea, "Please provide specific instructions.");
            isValid = false;
        }
        if (outputArea.getText() == null || outputArea.getText().trim().isEmpty()) {
            showError(outputError, outputArea, "Please provide expected output.");
            isValid = false;
        }
        if (hintsArea.getText() == null || hintsArea.getText().trim().isEmpty()) {
            showError(hintsError, hintsArea, "Please provide hints.");
            isValid = false;
        }
        try {
            int duration = Integer.parseInt(durationField.getText());
            if (duration <= 0) {
                showError(durationError, durationField, "Duration must be greater than 0.");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            showError(durationError, durationField, "Duration must be a valid number.");
            isValid = false;
        }
        
        return isValid;
    }

    @FXML
    public void handleChooseFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Document");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"),
            new javafx.stage.FileChooser.ExtensionFilter("PDF Documents", "*.pdf"),
            new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(fileField.getScene().getWindow());
        if (selectedFile != null) {
            fileField.setText(selectedFile.getAbsolutePath());
        }
    }

}
