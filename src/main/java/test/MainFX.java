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
