package controllers.courses;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.geometry.Pos;
import models.Course;
import services.CourseService;
import java.sql.SQLException;
import java.sql.Timestamp;
import javafx.stage.FileChooser;
import java.io.File;

public class CourseAddController extends BaseCourseController {

    @FXML private TextField courseNameField, teacherEmailField, coefficientField, durationField, courseFileField, courseLinkField;
    @FXML private ComboBox<String> semesterComboBox, difficultyComboBox, typeComboBox, priorityComboBox, statusComboBox;
    @FXML private TextArea commentArea;
    @FXML private Label formTitle;
    @FXML private Button submitBtn;
    @FXML private StackPane rootPane;

    // --- Inline Error Labels ---
    @FXML private Label errName, errEmail, errSemester, errDifficulty, errType, errPriority, errStatus, errCoeff, errDuration;

    @FXML
    public void initialize() {
        if (formTitle != null) formTitle.setText("Add a New Course");
        if (submitBtn != null) submitBtn.setText("💾 Add Course");

        // Real-time validation listeners
        addClearOnType(courseNameField, errName);
        addClearOnType(teacherEmailField, errEmail);
        addClearOnType(coefficientField, errCoeff);
        addClearOnType(durationField, errDuration);
        addClearOnSelect(semesterComboBox, errSemester);
        addClearOnSelect(difficultyComboBox, errDifficulty);
        addClearOnSelect(typeComboBox, errType);
        addClearOnSelect(priorityComboBox, errPriority);
        addClearOnSelect(statusComboBox, errStatus);
    }

    private void addClearOnType(TextField field, Label errLabel) {
        if (field != null && errLabel != null) {
            field.textProperty().addListener((obs, old, val) -> {
                if (!val.isEmpty()) {
                    clearError(field, errLabel);
                }
            });
        }
    }

    private void addClearOnSelect(ComboBox<String> combo, Label errLabel) {
        if (combo != null && errLabel != null) {
            combo.valueProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    clearError(combo, errLabel);
                }
            });
        }
    }

    private void showError(javafx.scene.Node field, Label errLabel, String msg) {
        if (errLabel != null) {
            errLabel.setText("⚠ " + msg);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        }
        if (field != null) {
            field.setStyle(field.getStyle() + " -fx-border-color: #EF4444; -fx-border-width: 1.5; -fx-border-radius: 10;");
        }
    }

    private void clearError(javafx.scene.Node field, Label errLabel) {
        if (errLabel != null) {
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
        if (field != null) {
            // Remove error border by reapplying base style class
            field.setStyle("");
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        // Name
        if (courseNameField.getText().trim().isEmpty()) {
            showError(courseNameField, errName, "Course name is required.");
            valid = false;
        }

        // Email
        String email = teacherEmailField.getText().trim();
        if (email.isEmpty()) {
            showError(teacherEmailField, errEmail, "Teacher email is required.");
            valid = false;
        } else if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(teacherEmailField, errEmail, "Please enter a valid email (e.g. prof@univ.edu).");
            valid = false;
        }

        // Semester
        if (semesterComboBox.getValue() == null) {
            showError(semesterComboBox, errSemester, "Please select a semester.");
            valid = false;
        }

        // Difficulty
        if (difficultyComboBox.getValue() == null) {
            showError(difficultyComboBox, errDifficulty, "Please select a difficulty level.");
            valid = false;
        }

        // Type
        if (typeComboBox.getValue() == null) {
            showError(typeComboBox, errType, "Please select a course type.");
            valid = false;
        }

        // Priority
        if (priorityComboBox.getValue() == null) {
            showError(priorityComboBox, errPriority, "Please select a priority.");
            valid = false;
        }

        // Status
        if (statusComboBox.getValue() == null) {
            showError(statusComboBox, errStatus, "Please select a status.");
            valid = false;
        }

        // Coefficient
        String coeff = coefficientField.getText().trim();
        if (coeff.isEmpty()) {
            showError(coefficientField, errCoeff, "Coefficient is required.");
            valid = false;
        } else {
            try {
                double c = Double.parseDouble(coeff);
                if (c < 0 || c > 10) {
                    showError(coefficientField, errCoeff, "Coefficient must be between 0 and 10.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showError(coefficientField, errCoeff, "Coefficient must be a valid number.");
                valid = false;
            }
        }

        // Duration
        String dur = durationField.getText().trim();
        if (!dur.isEmpty()) {
            try {
                int d = Integer.parseInt(dur);
                if (d <= 0) {
                    showError(durationField, errDuration, "Duration must be a positive number.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showError(durationField, errDuration, "Duration must be a whole number (e.g. 30).");
                valid = false;
            }
        }

        return valid;
    }

    @FXML
    public void addCourse(ActionEvent event) {
        if (!validateForm()) return;

        try {
            Course course = buildCourse();
            new CourseService().ajouter(course);
            showSuccessNotification();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorNotification("Error saving course: " + e.getMessage());
        }
    }

    private Course buildCourse() {
        Course course = new Course();
        course.setName(courseNameField.getText().trim());
        course.setTeacher_email(teacherEmailField.getText().trim());
        course.setSemester(semesterComboBox.getValue());
        course.setDifficulty_level(difficultyComboBox.getValue());
        course.setType(typeComboBox.getValue());
        course.setPriority(priorityComboBox.getValue());
        course.setStatus(statusComboBox.getValue());
        course.setCourse_file(courseFileField.getText().isEmpty() ? "" : courseFileField.getText());
        course.setCourse_link(courseLinkField.getText().isEmpty() ? "" : courseLinkField.getText());
        course.setComment(commentArea.getText() == null ? "" : commentArea.getText());
        course.setCoefficient(parseCoefficient());
        course.setDuration(parseDuration());
        course.setCreated_at(new Timestamp(System.currentTimeMillis()));
        course.setUser_id(1);
        return course;
    }

    private void showSuccessNotification() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("success-overlay");
        overlay.setOpacity(0);
        VBox card = new VBox(20);
        card.getStyleClass().add("success-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(340, javafx.scene.layout.Region.USE_PREF_SIZE);
        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("success-icon-box");
        SVGPath checkIcon = createIcon("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z", "#16A34A", 32);
        iconBox.getChildren().add(checkIcon);
        Label title = new Label("Success!");
        title.getStyleClass().add("success-title");
        Label message = new Label("Course added successfully. Redirecting to your dashboard...");
        message.getStyleClass().add("success-message");
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);
        card.getChildren().addAll(iconBox, title, message);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(400), card);
        scaleUp.setFromX(0.85);
        scaleUp.setFromY(0.85);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        fadeIn.play();
        scaleUp.play();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> {
            loadScene("/gestion_cours/frontend_courses.fxml", null, rootPane);
        }));
        timeline.play();
    }

    private void showErrorNotification(String msg) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("error-overlay");
        overlay.setOpacity(0);
        VBox card = new VBox(15);
        card.getStyleClass().add("error-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(380, javafx.scene.layout.Region.USE_PREF_SIZE);
        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("error-icon-box");
        SVGPath errorIcon = createIcon("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z", "#E11D48", 28);
        iconBox.getChildren().add(errorIcon);
        Label title = new Label("Please fix the errors");
        title.getStyleClass().add("error-title");
        Label message = new Label(msg);
        message.getStyleClass().add("error-message");
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);
        Button closeBtn = new Button("I'll fix it");
        closeBtn.getStyleClass().add("btn-error-close");
        closeBtn.setOnAction(e -> rootPane.getChildren().remove(overlay));
        card.getChildren().addAll(iconBox, title, message, closeBtn);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);
        FadeTransition ft = new FadeTransition(Duration.millis(300), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private double parseCoefficient() {
        try { return Double.parseDouble(coefficientField.getText()); } catch (Exception e) { return 1.0; }
    }

    private int parseDuration() {
        try { return Integer.parseInt(durationField.getText()); } catch (Exception e) { return 0; }
    }

    @FXML
    public void handleBrowseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            courseFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    public void handleBack(javafx.scene.input.MouseEvent event) {
        goToCourses(event);
    }
}
