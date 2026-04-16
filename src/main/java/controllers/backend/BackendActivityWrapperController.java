package controllers.backend;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import models.Course;
import controllers.activities.ActivityListController;

import java.io.IOException;

public class BackendActivityWrapperController {

    @FXML private BorderPane rootPane;
    @FXML private ActivityListController activityListController; // This injects from <fx:include fx:id="activityList">

    @FXML
    public void initialize() {
        System.out.println("BackendActivityWrapperController initialized.");
    }

    public void setCourse(Course course) {
        if (activityListController != null) {
            activityListController.setCourse(course);
            activityListController.setFromBackend(true);
        }
    }

    @FXML
    public void handleShowUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TEMPLATE/backend_management.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleShowCourses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TEMPLATE/backend_courses.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
