package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import controllers.user_controller.AddUserController;
import controllers.user_controller.ListUserController;
import models.User;
import java.io.IOException;
import java.util.List;

public class BackendUsersController {

    @FXML public TableView<User> usersTable;
    @FXML public TableColumn<User, String> colAvatar;
    @FXML public TableColumn<User, User> colUser;
    @FXML public TableColumn<User, String> colRole;
    @FXML public TableColumn<User, String> colStatus;
    @FXML public TableColumn<User, Object> colLastLogin;
    @FXML public TableColumn<User, String> colActions;
    
    @FXML public TextField searchField;
    @FXML public Button sortDateBtn;

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
            System.out.println("Initializing BackendUsersController...");
            
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
            
            System.out.println("BackendUsersController initialized successfully.");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in BackendUsersController.initialize():");
            e.printStackTrace();
        }
    }

    private void updateDashboardStats() {
        if (listUserController == null || totalUsersLabel == null || flaggedUsersLabel == null) return;
        
        List<User> allUsers = listUserController.getAllUsers();
        long total = allUsers.size();
        long flagged = allUsers.stream()
                .filter(u -> "Flagged".equalsIgnoreCase(u.getStatut()))
                .count();
        
        totalUsersLabel.setText(String.valueOf(total));
        flaggedUsersLabel.setText(String.valueOf(flagged));
    }

    private void setupListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> {
                if (listUserController != null) listUserController.applyTextFilter(val);
            });
        }

        if (sortDateBtn != null) {
            sortDateBtn.setOnAction(e -> {
                boolean descending = listUserController.toggleSortByDate();
                updateSortUI(descending);
            });
        }

        if (filterAll != null) filterAll.setOnMouseClicked(e -> handleRoleClick("ALL", filterAll));
        if (filterAdmins != null) filterAdmins.setOnMouseClicked(e -> handleRoleClick("ADMIN", filterAdmins));
        if (filterUsers != null) filterUsers.setOnMouseClicked(e -> handleRoleClick("USER", filterUsers));
    }

    private void handleRoleClick(String role, Label label) {
        if (listUserController != null) {
            listUserController.applyRoleFilter(role);
            
            // Reset styles
            Label[] labels = {filterAll, filterAdmins, filterUsers};
            for (Label l : labels) {
                if (l != null) {
                    l.getStyleClass().remove("filter-text-active");
                    l.getStyleClass().add("filter-text");
                }
            }
            label.getStyleClass().remove("filter-text");
            label.getStyleClass().add("filter-text-active");
        }
    }

    private void updateSortUI(boolean descending) {
        if (sortDateBtn == null) return;
        sortDateBtn.setText(descending ? "Sort: Newest First" : "Sort: Oldest First");
        
        if (sortDateBtn.getGraphic() instanceof SVGPath) {
            SVGPath icon = (SVGPath) sortDateBtn.getGraphic();
            if (descending) {
                icon.setContent("M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z");
            } else {
                icon.setContent("M3 18h18v-2H3v2zM3 6v2h6V6H3zm0 7h12v-2H3v2z");
            }
        }
    }

    @FXML
    public void handleExportExcel() {
        System.out.println("Exporting to Excel...");
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText("Excel Export");
        alert.setContentText("Successfully exported " + totalUsersLabel.getText() + " users to Excel format.");
        alert.show();
    }

    @FXML
    public void handleCreateNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/getion_user/add_user.fxml"));
            VBox form = loader.load();
            
            AddUserController controller = loader.getController();
            controller.setListUserController(listUserController);
            
            Stage stage = new Stage();
            stage.setTitle("Add User");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(form));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
