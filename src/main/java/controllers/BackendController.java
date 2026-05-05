package controllers;

import controllers.user_controller.AddUserController;
import controllers.user_controller.ListUserController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.User;

import java.io.IOException;

public class BackendController {

    // Search / sort
    @FXML public TextField searchField;
    @FXML public Button sortDateBtn;

    // Sidebar nav
    @FXML public Button navOverviewBtn;
    @FXML public Button navUsersBtn;
    @FXML public Button navGroupsBtn;

    // Views
    @FXML public VBox usersView;
    @FXML public VBox groupsView;

    // Filter labels
    @FXML public Label filterAll;
    @FXML public Label filterAdmins;
    @FXML public Label filterUsers;

    // Dashboard stat labels
    @FXML public Label totalUsersLabel;
    @FXML public Label flaggedUsersLabel;

    // Users table
    @FXML public TableView<User> usersTable;
    @FXML public TableColumn<User, String> colAvatar;
    @FXML public TableColumn<User, User> colUser;
    @FXML public TableColumn<User, String> colRole;
    @FXML public TableColumn<User, String> colStatus;
    @FXML public TableColumn<User, Object> colLastLogin;
    @FXML public TableColumn<User, String> colActions;

    private ListUserController listUserController;

    @FXML
    public void initialize() {
        // Default view: Users
        showUsers();

        listUserController = new ListUserController(usersTable);
        listUserController.initializeTable(colAvatar, colUser, colRole, colStatus, colLastLogin, colActions);
        listUserController.setOnDataChanged(this::updateDashboardStats);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> listUserController.applyTextFilter(newV));
        }

        if (sortDateBtn != null) {
            sortDateBtn.setOnAction(e -> {
                boolean desc = listUserController.toggleSortByDate();
                sortDateBtn.setText(desc ? "Sort: Newest First" : "Sort: Oldest First");
            });
        }

        // Filters (labels act like buttons in the FXML)
        if (filterAll != null) {
            filterAll.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> applyRoleFilter("ALL"));
        }
        if (filterAdmins != null) {
            filterAdmins.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> applyRoleFilter("ADMIN"));
        }
        if (filterUsers != null) {
            filterUsers.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> applyRoleFilter("USER"));
        }

        updateDashboardStats();
    }

    @FXML
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
        // Not wired yet in the template; keep Users as the default page.
        showUsers();
    }

    @FXML
    public void handleExportExcel() {
        // Export is handled by dedicated sub-controllers/screens when needed.
    }

    @FXML
    public void handleCreateNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/getion_user/add_user.fxml"));
            VBox form = loader.load();
            AddUserController controller = loader.getController();
            controller.setListUserController(listUserController);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add User");
            stage.setScene(new Scene(form));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyRoleFilter(String role) {
        if (listUserController != null) {
            listUserController.applyRoleFilter(role);
        }

        setActiveFilter(filterAll, "ALL".equalsIgnoreCase(role));
        setActiveFilter(filterAdmins, "ADMIN".equalsIgnoreCase(role));
        setActiveFilter(filterUsers, "USER".equalsIgnoreCase(role));
    }

    private void setActiveFilter(Label label, boolean active) {
        if (label == null) return;
        label.getStyleClass().removeAll("filter-text", "filter-text-active");
        label.getStyleClass().add(active ? "filter-text-active" : "filter-text");
    }

    private void setActiveNav(Button active) {
        Button[] btns = {navOverviewBtn, navUsersBtn, navGroupsBtn};
        for (Button b : btns) {
            if (b == null) continue;
            b.getStyleClass().removeAll("nav-button", "nav-button-active");
            b.getStyleClass().add(b == active ? "nav-button-active" : "nav-button");
        }
    }

    private void updateDashboardStats() {
        if (totalUsersLabel == null || flaggedUsersLabel == null || listUserController == null) {
            return;
        }

        int total = 0;
        int flagged = 0;
        for (User u : listUserController.getAllUsers()) {
            total++;
            String st = u.getStatut();
            if (st != null && st.equalsIgnoreCase("Flagged")) {
                flagged++;
            }
        }

        totalUsersLabel.setText(String.valueOf(total));
        flaggedUsersLabel.setText(String.valueOf(flagged));
    }
}

