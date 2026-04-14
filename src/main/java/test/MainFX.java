package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainFX extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        URL dashboardResource = getClass().getResource("/TEMPLATE/frontend_dashboard.fxml");
        if (dashboardResource == null) {
            throw new IllegalStateException("Missing FXML resource: /TEMPLATE/frontend_dashboard.fxml");
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
