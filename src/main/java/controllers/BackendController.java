package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

public class BackendController {

    @FXML
    private TableView<UserModel> usersTable;
    @FXML
    private TableColumn<UserModel, String> colAvatar;
    @FXML
    private TableColumn<UserModel, UserModel> colUser;
    @FXML
    private TableColumn<UserModel, String> colRole;
    @FXML
    private TableColumn<UserModel, String> colStatus;
    @FXML
    private TableColumn<UserModel, String> colLastLogin;
    @FXML
    private TableColumn<UserModel, String> colActions;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDummyData();
    }

    private void setupTableColumns() {
        // Avatar Column (simulated with a circle)
        colAvatar.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAvatar.setCellFactory(column -> new TableCell<UserModel, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setGraphic(null);
                } else {
                    Circle avatar = new Circle(15, Color.web("#" + Integer.toHexString((name.hashCode() & 0xffffff) | 0x888888).substring(0, 6)));
                    HBox box = new HBox(avatar);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        // User Details Column (Name + Email)
        colUser.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        colUser.setCellFactory(column -> new TableCell<UserModel, UserModel>() {
            @Override
            protected void updateItem(UserModel user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    Label nameLbl = new Label(user.getName());
                    nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
                    Label emailLbl = new Label(user.getEmail());
                    emailLbl.setStyle("-fx-text-fill: #5a6b8c; -fx-font-size: 11px;");
                    VBox box = new VBox(2, nameLbl, emailLbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        // Role Column (styled pill)
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(column -> new TableCell<UserModel, String>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(role.toUpperCase());
                    if (role.equalsIgnoreCase("admin")) lbl.getStyleClass().add("role-pill-admin");
                    else if (role.equalsIgnoreCase("developer")) lbl.getStyleClass().add("role-pill-developer");
                    else lbl.getStyleClass().add("role-pill-support");
                    setGraphic(lbl);
                }
            }
        });

        // Status Column (styled pill + dot)
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<UserModel, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Circle dot = new Circle(4);
                    Label lbl = new Label(status);
                    lbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                    
                    if (status.equalsIgnoreCase("Active")) {
                        dot.setFill(Color.web("#4ade80"));
                    } else {
                        dot.setFill(Color.web("#8b9bb4"));
                        lbl.setStyle("-fx-text-fill: #8b9bb4; -fx-font-size: 12px;");
                    }
                    
                    HBox box = new HBox(8, dot, lbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        // Last Login Column
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLoginFull"));
        colLastLogin.setCellFactory(column -> new TableCell<UserModel, String>() {
            @Override
            protected void updateItem(String loginData, boolean empty) {
                super.updateItem(loginData, empty);
                if (empty || loginData == null) {
                    setGraphic(null);
                } else {
                    String[] parts = loginData.split("\\|");
                    Label timeLbl = new Label(parts[0]);
                    timeLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                    Label ipLbl = new Label(parts.length > 1 ? parts[1] : "");
                    ipLbl.setStyle("-fx-text-fill: #5a6b8c; -fx-font-size: 10px;");
                    VBox box = new VBox(2, timeLbl, ipLbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });
        
        // Actions empty placeholder for now
        colActions.setCellFactory(column -> new TableCell<UserModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setGraphic(null);
                } else {
                    Label dots = new Label("•••");
                    dots.setStyle("-fx-text-fill: #8b9bb4; -fx-font-size: 16px; -fx-cursor: hand;");
                    setGraphic(dots);
                }
            }
        });
    }

    private void loadDummyData() {
        ObservableList<UserModel> data = FXCollections.observableArrayList(
            new UserModel("Sarah Jenkins", "s.jenkins@hyperflux.io", "Admin", "Active", "2 mins ago", "192.168.1.1"),
            new UserModel("Marcus Thorne", "m.thorne@hyperflux.io", "Developer", "Offline", "5 hours ago", "172.16.25.4"),
            new UserModel("Elena Rodriguez", "e.rod@hyperflux.io", "Support", "Active", "14 mins ago", "203.0.113.88")
        );
        usersTable.setItems(data);
    }

    // Inner class for TableView Data Model
    public static class UserModel {
        private String name;
        private String email;
        private String role;
        private String status;
        private String timeAgo;
        private String ipAddress;

        public UserModel(String name, String email, String role, String status, String timeAgo, String ipAddress) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.status = status;
            this.timeAgo = timeAgo;
            this.ipAddress = ipAddress;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public String getLastLoginFull() { return timeAgo + "|" + ipAddress; }
    }
}
