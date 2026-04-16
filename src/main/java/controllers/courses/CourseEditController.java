package controllers.courses;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import models.Course;
import services.CourseService;
import java.sql.SQLException;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.collections.FXCollections;

public class CourseEditController extends BaseCourseController {

    @FXML
    private TextField courseNameField, teacherEmailField, coefficientField, durationField, courseFileField,
            courseLinkField;
    @FXML
    private ComboBox<String> semesterComboBox, difficultyComboBox, typeComboBox, priorityComboBox, statusComboBox;
    @FXML
    private TextArea commentArea;
    @FXML
    private Label formTitle;
    @FXML
    private Button submitBtn;

    private Course course;

    public void setCourse(Course course) {
        this.course = course;
        if (course != null) {
            populateForm(course);
        }
    }

    private static Course courseToEdit;

    public static void startEdit(Course course, Stage stage) {
        courseToEdit = course; // FIX: Ensure the course data is passed to the static field for the controller to use
        try {
            FXMLLoader loader = new FXMLLoader(CourseEditController.class.getResource("/gestion_cours/frontend_edit_course.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        populateChoiceLists();
        if (courseToEdit != null) {
            populateForm(courseToEdit);
        }
    }

    private void populateChoiceLists() {
        semesterComboBox.setItems(FXCollections.observableArrayList("Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5"));
        difficultyComboBox.setItems(FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced", "Expert"));
        typeComboBox.setItems(FXCollections.observableArrayList("In-Person", "Online", "Hybrid", "Recorded"));
        priorityComboBox.setItems(FXCollections.observableArrayList("Low", "Medium", "High", "Critical"));
        statusComboBox.setItems(FXCollections.observableArrayList("Active", "Pending", "Archived", "Draft"));
    }

    private void populateForm(Course course) {
        if (formTitle != null) formTitle.setText("Edit Course");
        if (submitBtn != null) submitBtn.setText("💾 Save Changes");

        if (courseNameField != null) courseNameField.setText(course.getName());
        if (teacherEmailField != null) teacherEmailField.setText(course.getTeacher_email());
        if (semesterComboBox != null) semesterComboBox.setValue(course.getSemester());
        if (difficultyComboBox != null) difficultyComboBox.setValue(course.getDifficulty_level());
        if (typeComboBox != null) typeComboBox.setValue(course.getType());
        if (priorityComboBox != null) priorityComboBox.setValue(course.getPriority());
        if (statusComboBox != null) statusComboBox.setValue(course.getStatus());
        if (coefficientField != null) coefficientField.setText(String.valueOf(course.getCoefficient()));
        if (durationField != null) durationField.setText(String.valueOf(course.getDuration()));
        if (courseFileField != null) courseFileField.setText(course.getCourse_file());
        if (courseLinkField != null) courseLinkField.setText(course.getCourse_link());
        if (commentArea != null) commentArea.setText(course.getComment());
    }

    @FXML
    public void saveChanges(ActionEvent event) {
        Course target = (courseToEdit != null) ? courseToEdit : this.course;
        if (target == null) return;

        try {
            updateCourseFromForm(target);
            new CourseService().modifier(target);
            courseToEdit = null; 
            
            returnToCourses(null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCourseFromForm(Course course) {
        if (courseNameField != null) course.setName(courseNameField.getText());
        if (teacherEmailField != null) course.setTeacher_email(teacherEmailField.getText());
        if (semesterComboBox != null) course.setSemester(semesterComboBox.getValue());
        if (difficultyComboBox != null) course.setDifficulty_level(difficultyComboBox.getValue());
        if (typeComboBox != null) course.setType(typeComboBox.getValue());
        if (priorityComboBox != null) course.setPriority(priorityComboBox.getValue());
        if (statusComboBox != null) course.setStatus(statusComboBox.getValue());
        if (courseFileField != null) course.setCourse_file(courseFileField.getText());
        if (courseLinkField != null) course.setCourse_link(courseLinkField.getText());
        if (commentArea != null) course.setComment(commentArea.getText());

        try {
            course.setCoefficient(Double.parseDouble(coefficientField.getText()));
        } catch (Exception e) {
            course.setCoefficient(1.0);
        }

        try {
            course.setDuration(Integer.parseInt(durationField.getText()));
        } catch (Exception e) {
            course.setDuration(0);
        }
    }

    @FXML
    public void handleBrowseFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Document");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"),
            new javafx.stage.FileChooser.ExtensionFilter("PDF Documents", "*.pdf"),
            new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(courseFileField.getScene().getWindow());
        if (selectedFile != null) {
            courseFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    public void handleBack(javafx.scene.input.MouseEvent event) {
        courseToEdit = null;
        returnToCourses(event);
    }
}
