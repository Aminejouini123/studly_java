package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainFX extends Application {
    private static final String DASHBOARD_FXML = "/getion_user/auth_page.fxml";

    @Override
    public void start(Stage stage) throws Exception {
        // Set global exception handler for the FX thread
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception: " + throwable.getMessage());
            throwable.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An unexpected error occurred");
                alert.setContentText(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
                alert.showAndWait();
            });
        });

        URL dashboardResource = getClass().getResource(DASHBOARD_FXML);
        if (dashboardResource == null) {
            throw new IllegalStateException("Missing FXML resource: " + DASHBOARD_FXML);
        }

        FXMLLoader fxmlloader = new FXMLLoader(dashboardResource);
        Parent root = fxmlloader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Studly");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
