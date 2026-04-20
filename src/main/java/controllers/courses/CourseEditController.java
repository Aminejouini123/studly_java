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

    private static Course courseToEdit;

    public static void startEdit(Course course, Stage stage) {
        try {
            // - [x] Update `frontend_add_course.fxml` with Resource sections
            // - [x] Update `CourseAddController.java` to handle new fields
            // - [x] Update `frontend_edit_course.fxml`
            // - [x] Update `CourseEditController.java`
            // - [x] Update `CourseDetailController.java` / `frontend_course_detail.fxml` to display values
            // - [x] Verification and walkthrough
            courseToEdit = course;
            FXMLLoader loader = new FXMLLoader(
                    CourseEditController.class.getResource("/gestion_cours/frontend_edit_course.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        if (courseToEdit != null) {
            populateForm(courseToEdit);
        }
    }

    private void populateForm(Course course) {
        formTitle.setText("Edit Course");
        submitBtn.setText("💾 Save Changes");

        courseNameField.setText(course.getName());
        teacherEmailField.setText(course.getTeacher_email());
        semesterComboBox.setValue(course.getSemester());
        difficultyComboBox.setValue(course.getDifficulty_level());
        typeComboBox.setValue(course.getType());
        priorityComboBox.setValue(course.getPriority());
        statusComboBox.setValue(course.getStatus());
        coefficientField.setText(String.valueOf(course.getCoefficient()));
        durationField.setText(String.valueOf(course.getDuration()));
        courseFileField.setText(course.getCourse_file());
        courseLinkField.setText(course.getCourse_link());
        commentArea.setText(course.getComment());
    }

    @FXML
    public void saveChanges(ActionEvent event) {
        if (courseToEdit == null)
            return;

        try {
            updateCourseFromForm(courseToEdit);
            new CourseService().modifier(courseToEdit);
            courseToEdit = null; // Clear
            loadScene("/gestion_cours/frontend_courses.fxml", null, (javafx.scene.Node) event.getSource());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCourseFromForm(Course course) {
        course.setName(courseNameField.getText());
        course.setTeacher_email(teacherEmailField.getText());
        course.setSemester(semesterComboBox.getValue());
        course.setDifficulty_level(difficultyComboBox.getValue());
        course.setType(typeComboBox.getValue());
        course.setPriority(priorityComboBox.getValue());
        course.setStatus(statusComboBox.getValue());
        course.setCourse_file(courseFileField.getText());
        course.setCourse_link(courseLinkField.getText());
        course.setComment(commentArea.getText());

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
    public void handleBrowseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(courseNameField.getScene().getWindow());
        if (selectedFile != null) {
            courseFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    public void handleBack(javafx.scene.input.MouseEvent event) {
        courseToEdit = null;
        goToCourses(event);
    }
}
