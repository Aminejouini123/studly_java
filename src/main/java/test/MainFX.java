package test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import utils.DatabaseInitializer;
import utils.AdminBootstrap;
import utils.SchemaFixer;

import java.net.URL;

public class MainFX extends Application {
    private static final String DASHBOARD_FXML = "/getion_user/auth_page.fxml";

    @Override
    public void start(Stage stage) throws Exception {
        // Set global exception handler for the FX thread early (before any DB work).
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception: " + throwable.getMessage());
            throwable.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An unexpected error occurred");
                alert.setContentText(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
                alert.showAndWait();
            });
        });

        // Show something immediately; DB initialization can block if MySQL is slow/unresponsive.
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(64, 64);
        StackPane splashRoot = new StackPane(spinner);
        Scene splash = new Scene(splashRoot, 1100, 800);
        stage.setScene(splash);
        stage.setWidth(1100);
        stage.setHeight(800);
        stage.setTitle("Studly");
        stage.show();

        // Run DB/schema initialization off the FX thread, then load the real UI.
        Thread init = new Thread(() -> {
            Throwable initFailure = null;
            try {
                // Fix legacy `groups` vs `group` naming before initialization creates new tables.
                SchemaFixer.repairBeforeInitialization();
                // Ensure DB schema exists before any controllers/services query it (e.g., login hits `users`).
                DatabaseInitializer.initializeDefault();
                // Repair legacy FK constraints (e.g., invitation -> `user` instead of `users`) so group invites work.
                SchemaFixer.repairDefault();
                // Optional: create/update an admin account when STUDLY_ADMIN_EMAIL and STUDLY_ADMIN_PASSWORD are set.
                AdminBootstrap.ensureAdminFromEnv();
            } catch (Throwable t) {
                initFailure = t;
                System.err.println("Startup initialization failed: " + t.getMessage());
                t.printStackTrace();
            }

            Throwable finalInitFailure = initFailure;
            Platform.runLater(() -> {
                if (finalInitFailure != null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Startup Warning");
                    alert.setHeaderText("Initialization did not complete");
                    alert.setContentText(finalInitFailure.getMessage() != null ? finalInitFailure.getMessage() : finalInitFailure.toString());
                    alert.showAndWait();
                }

                URL dashboardResource = getClass().getResource(DASHBOARD_FXML);
                if (dashboardResource == null) {
                    throw new IllegalStateException("Missing FXML resource: " + DASHBOARD_FXML);
                }

                try {
                    FXMLLoader fxmlloader = new FXMLLoader(dashboardResource);
                    Parent root = fxmlloader.load();
                    stage.setScene(new Scene(root));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
        init.setName("studly-startup-init");
        init.setDaemon(true);
        init.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
