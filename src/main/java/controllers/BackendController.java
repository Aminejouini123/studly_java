package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.util.List;

public class BackendController {

    @FXML private StackPane mainContentHost;
    
<<<<<<< HEAD
    @FXML public TextField searchField;
    @FXML public Button sortDateBtn;

    // Sidebar nav
    @FXML public Button navOverviewBtn;
    @FXML public Button navUsersBtn;
    @FXML public Button navGroupsBtn;

    // Views
    @FXML public VBox usersView;
    @FXML public VBox groupsView;

    // Filter Labels
    @FXML public Label filterAll;
    @FXML public Label filterAdmins;
    @FXML public Label filterUsers;

    // Dashboard Stat Labels
    @FXML public Label totalUsersLabel;
    @FXML public Label flaggedUsersLabel;

    private ListUserController listUserController;

    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing BackendController...");

            if (groupsView != null) {
                groupsView.setVisible(false);
                groupsView.setManaged(false);
            }
            
            // 1. Initialize logic
            listUserController = new ListUserController(usersTable);
            
            // 2. Setup table
            listUserController.initializeTable(
                colAvatar, 
                colUser, 
                colRole, 
                colStatus, 
                colLastLogin, 
                colActions
            );

            // 3. Setup dashboard update callback
            listUserController.setOnDataChanged(this::updateDashboardStats);
            
            // 4. Setup listeners
            setupListeners();
            
            // 5. Initial stats update
            updateDashboardStats();

            // Default view
            setActiveNav(navUsersBtn);
            
            System.out.println("BackendController initialized successfully.");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in BackendController.initialize():");
=======
    @FXML private Button overviewBtn;
    @FXML private Button timeBtn;
    @FXML private Button coursesBtn;

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
>>>>>>> 79052c32a185cf35582507e045808aa98d0c2c0e
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] buttons = {overviewBtn, usersBtn, timeBtn, coursesBtn};
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

    @FXML
<<<<<<< HEAD
    public void showUsers() {
        if (usersView != null) {
            usersView.setVisible(true);
            usersView.setManaged(true);
        }
        if (groupsView != null) {
            groupsView.setVisible(false);
            groupsView.setManaged(false);
        }
        setActiveNav(navUsersBtn);
    }

    @FXML
    public void showGroups() {
        if (usersView != null) {
            usersView.setVisible(false);
            usersView.setManaged(false);
        }
        if (groupsView != null) {
            groupsView.setVisible(true);
            groupsView.setManaged(true);
        }
        setActiveNav(navGroupsBtn);
    }

    @FXML
    public void showOverview() {
        // Not implemented yet; keep users view as default.
        showUsers();
    }

    private void setActiveNav(Button active) {
        Button[] btns = {navOverviewBtn, navUsersBtn, navGroupsBtn};
        for (Button b : btns) {
            if (b == null) continue;
            b.getStyleClass().removeAll("nav-button", "nav-button-active");
            b.getStyleClass().add(b == active ? "nav-button-active" : "nav-button");
        }
=======
    public void handleShowCourses() {
        System.out.println("Navigating to courses...");
        setActiveButton(coursesBtn);
        loadContent("/gestion_cours/backend_courses.fxml");
>>>>>>> 79052c32a185cf35582507e045808aa98d0c2c0e
    }
}
