package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class BackendController {

    @FXML private StackPane mainContentHost;
    
    @FXML private Button overviewBtn;
    @FXML private Button usersBtn;
    @FXML private Button timeBtn;

    @FXML
    public void initialize() {
        System.out.println("Initializing BackendController Shell...");
        // Load Users by default
        showUsers();
    }

    @FXML
    public void showOverview() {
        System.out.println("Navigating to Overview...");
        setActiveButton(overviewBtn);
        // Implement overview content if needed, or clear host
        mainContentHost.getChildren().clear();
    }

    @FXML
    public void showUsers() {
        System.out.println("Navigating to Users Management...");
        setActiveButton(usersBtn);
        loadContent("/TEMPLATE/backend_users.fxml");
    }

    @FXML
    public void showTimeManagement() {
        System.out.println("Navigating to Time Management...");
        setActiveButton(timeBtn);
        loadContent("/TEMPLATE/backend_time.fxml");
    }

    private void loadContent(String fxmlPath) {
        try {
            System.out.println("Loading content: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            mainContentHost.getChildren().setAll(content);
        } catch (IOException e) {
            System.err.println("Error loading FXML content: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] buttons = {overviewBtn, usersBtn, timeBtn};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
                // Reset icon color
                if (btn.getGraphic() instanceof javafx.scene.shape.SVGPath) {
                    javafx.scene.shape.SVGPath svg = (javafx.scene.shape.SVGPath) btn.getGraphic();
                    if (svg.getStroke() != null && svg.getStroke() != javafx.scene.paint.Color.TRANSPARENT) {
                        svg.setStroke(javafx.scene.paint.Color.web("#64748B"));
                    } else {
                        svg.setFill(javafx.scene.paint.Color.web("#64748B"));
                    }
                }
            }
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-button");
            activeBtn.getStyleClass().add("nav-button-active");
            // Set active icon color
            if (activeBtn.getGraphic() instanceof javafx.scene.shape.SVGPath) {
                javafx.scene.shape.SVGPath svg = (javafx.scene.shape.SVGPath) activeBtn.getGraphic();
                if (svg.getStroke() != null && svg.getStroke() != javafx.scene.paint.Color.TRANSPARENT) {
                    svg.setStroke(javafx.scene.paint.Color.web("#004fb0"));
                } else {
                    svg.setFill(javafx.scene.paint.Color.web("#38bdf8"));
                }
            }
        }
    }

    @FXML
    public void handleExportExcel() {
        // This might be called from descendants if not handled there
        System.out.println("Export logic should be handled by sub-controllers.");
    }
}
