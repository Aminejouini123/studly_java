package controllers.exams;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import models.Course;
import models.Exam;
import services.ExamService;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class ExamEditController extends BaseExamController {

    @FXML private TextField titleField, durationField, gradeField, fileField, linkField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> difficultyCombo, statusCombo;
    @FXML private Label courseNameLabel;
    @FXML private StackPane rootPane;
    @FXML private Label titleError, dateError, durationError, difficultyError, gradeError, statusError;

    private Course currentCourse;
    private Exam currentExam;
    private final ExamService examService = new ExamService();
    
    public void setExam(Exam exam) {
        this.currentExam = exam;
        this.currentCourse = null;
        
        titleField.setText(exam.getTitle());
        if (exam.getDate() != null) datePicker.setValue(exam.getDate().toLocalDate());
        durationField.setText(String.valueOf(exam.getDuration()));
        gradeField.setText(String.valueOf(exam.getGrade()));
        difficultyCombo.setValue(exam.getDifficulty());
        statusCombo.setValue(exam.getStatus());
        fileField.setText(exam.getFile());
        linkField.setText(exam.getLink());
    }

    @FXML
    public void initialize() {
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        statusCombo.getItems().addAll("Pending", "Passed", "Failed", "Aborted");
    }

    public void setExam(Exam exam, Course course) {
        this.currentExam = exam;
        this.currentCourse = course;
        courseNameLabel.setText(course.getName());

        titleField.setText(exam.getTitle());
        datePicker.setValue(exam.getDate().toLocalDate());
        durationField.setText(String.valueOf(exam.getDuration()));
        gradeField.setText(String.valueOf(exam.getGrade()));
        difficultyCombo.setValue(exam.getDifficulty());
        statusCombo.setValue(exam.getStatus());
        fileField.setText(exam.getFile());
        linkField.setText(exam.getLink());
    }

    @FXML
    private void handleUpdate() {
        if (!validateFields()) return;

        try {
            currentExam.setTitle(titleField.getText());
            currentExam.setDate(Date.valueOf(datePicker.getValue()));
            currentExam.setDuration(Integer.parseInt(durationField.getText()));
            currentExam.setGrade(Double.parseDouble(gradeField.getText()));
            currentExam.setDifficulty(difficultyCombo.getValue());
            currentExam.setStatus(statusCombo.getValue());
            currentExam.setFile(fileField.getText());
            currentExam.setLink(linkField.getText());

            examService.modifier(currentExam);
            showSuccessNotification(rootPane, "Updated!", "Changes saved successfully.", () -> returnToDashboard(rootPane));
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorNotification(rootPane, "Update Error", "Could not update exam: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Duration and Grade must be valid numbers.");
        }
    }

    @FXML
    public void handleCancel() {
        if (fromBackend) {
            returnToDashboard(rootPane);
        } else {
            navigateToExamList(titleField, currentCourse);
        }
    }

    private void hideErrors() {
        if (titleError != null) {
            titleError.setVisible(false); titleError.setManaged(false); titleField.setStyle("");
            dateError.setVisible(false); dateError.setManaged(false); datePicker.setStyle("");
            durationError.setVisible(false); durationError.setManaged(false); durationField.setStyle("");
            difficultyError.setVisible(false); difficultyError.setManaged(false); difficultyCombo.setStyle("");
            gradeError.setVisible(false); gradeError.setManaged(false); gradeField.setStyle("");
            statusError.setVisible(false); statusError.setManaged(false); statusCombo.setStyle("");
        }
    }

    private void showError(Label errorLabel, javafx.scene.control.Control field, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        field.setStyle("-fx-border-color: transparent transparent #ef4444 transparent; -fx-border-width: 0 0 2 0;");
    }

    private boolean validateFields() {
        hideErrors();
        boolean isValid = true;
        
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showError(titleError, titleField, "Please provide an exam title.");
            isValid = false;
        }
        if (datePicker.getValue() == null) {
            showError(dateError, datePicker, "Please select an exam date.");
            isValid = false;
        }
        if (difficultyCombo.getValue() == null) {
            showError(difficultyError, difficultyCombo, "Please select a difficulty level.");
            isValid = false;
        }
        if (statusCombo.getValue() == null) {
            showError(statusError, statusCombo, "Please select the exam status.");
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
        try {
            double grade = Double.parseDouble(gradeField.getText());
            if (grade < 0.0 || grade > 20.0) {
                showError(gradeError, gradeField, "Target Grade must be between 0 and 20.");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            showError(gradeError, gradeField, "Target Grade must be a valid number.");
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
