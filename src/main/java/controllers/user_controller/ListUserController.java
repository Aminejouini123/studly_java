package controllers.user_controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.User;
import services.UserService;
import utils.EmailService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ListUserController {

    private TableView<User> usersTable;
    private final UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;
    private SortedList<User> sortedList;
    
    // Callback for dashboard updates
    private Runnable onDataChanged;
    
    // Filter State
    private String currentSearchQuery = "";
    private String currentRoleFilter = "ALL"; // ALL, ADMIN, USER
    private boolean descending = true;

    public ListUserController(TableView<User> usersTable) {
        this.usersTable = usersTable;
        this.filteredList = new FilteredList<>(userList, p -> true);
        this.sortedList = new SortedList<>(filteredList);
        
        if (usersTable != null) {
            this.sortedList.comparatorProperty().bind(usersTable.comparatorProperty());
        }
    }

    public void setOnDataChanged(Runnable callback) {
        this.onDataChanged = callback;
    }

    public List<User> getAllUsers() {
        return userList;
    }

    public void initializeTable(
            TableColumn<User, String> colAvatar,
            TableColumn<User, User> colUser,
            TableColumn<User, String> colRole,
            TableColumn<User, String> colStatus,
            TableColumn<User, Object> colLastLogin,
            TableColumn<User, String> colActions
    ) {
        setupTableColumns(colAvatar, colUser, colRole, colStatus, colLastLogin, colActions);
        refresh();
    }

    public void refresh() {
        try {
            List<User> users = userService.recuperer();
            userList.setAll(users);
            
            applyDateSort();
            
            if (usersTable != null) {
                usersTable.setItems(sortedList);
            }
            
            if (onDataChanged != null) onDataChanged.run();
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    public void applyTextFilter(String query) {
        this.currentSearchQuery = query == null ? "" : query.toLowerCase();
        updateFilterPredicate();
    }

    public void applyRoleFilter(String role) {
        this.currentRoleFilter = role == null ? "ALL" : role.toUpperCase();
        updateFilterPredicate();
    }

    private void updateFilterPredicate() {
        filteredList.setPredicate(user -> {
            if (!currentRoleFilter.equals("ALL")) {
                String roles = user.getRoles();
                if (roles == null) return false;
                boolean isAdmin = roles.contains("ROLE_ADMIN");
                if (currentRoleFilter.equals("ADMIN") && !isAdmin) return false;
                if (currentRoleFilter.equals("USER") && isAdmin) return false;
            }

            if (!currentSearchQuery.isEmpty()) {
                boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(currentSearchQuery);
                boolean matchesFirstName = user.getFirstName() != null && user.getFirstName().toLowerCase().contains(currentSearchQuery);
                boolean matchesLastName = user.getLastName() != null && user.getLastName().toLowerCase().contains(currentSearchQuery);
                if (!matchesEmail && !matchesFirstName && !matchesLastName) return false;
            }
            return true;
        });
    }

    public boolean toggleSortByDate() {
        this.descending = !this.descending;
        applyDateSort();
        return this.descending;
    }

    private void applyDateSort() {
        Comparator<User> comparator = (u1, u2) -> {
            if (u1.getCreatedAt() == null || u2.getCreatedAt() == null) return 0;
            return descending ? u2.getCreatedAt().compareTo(u1.getCreatedAt()) 
                              : u1.getCreatedAt().compareTo(u2.getCreatedAt());
        };
        sortedList.comparatorProperty().unbind();
        sortedList.setComparator(comparator);
    }

    private void setupTableColumns(
            TableColumn<User, String> colAvatar,
            TableColumn<User, User> colUser,
            TableColumn<User, String> colRole,
            TableColumn<User, String> colStatus,
            TableColumn<User, Object> colLastLogin,
            TableColumn<User, String> colActions
    ) {
        colAvatar.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colAvatar.setCellFactory(column -> new TableCell<User, String>() {
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

        colUser.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        colUser.setCellFactory(column -> new TableCell<User, User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    Label nameLbl = new Label(user.getFullName());
                    nameLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px;");
                    Label emailLbl = new Label(user.getEmail());
                    emailLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                    VBox box = new VBox(2, nameLbl, emailLbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        colRole.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colRole.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String roles, boolean empty) {
                super.updateItem(roles, empty);
                if (empty || roles == null) {
                    setGraphic(null);
                } else {
                    String displayRole = roles.replace("\"", "").replace("[", "").replace("]", "").split(",")[0].trim();
                    if (displayRole.startsWith("ROLE_")) displayRole = displayRole.substring(5);
                    Label lbl = new Label(displayRole.toUpperCase());
                    if (displayRole.equalsIgnoreCase("admin")) lbl.getStyleClass().add("role-pill-admin");
                    else lbl.getStyleClass().add("role-pill-support");
                    setGraphic(lbl);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Circle dot = new Circle(4);
                    Label lbl = new Label(status);
                    lbl.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: bold;");
                    if (status.equalsIgnoreCase("Active")) {
                        dot.setFill(Color.web("#4ade80"));
                    } else if (status.equalsIgnoreCase("BANNED")) {
                        dot.setFill(Color.web("#dc2626"));
                        lbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else if (status.equalsIgnoreCase("Flagged")) {
                        dot.setFill(Color.web("#f43f5e"));
                        lbl.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 12px; -fx-font-weight: bold;");
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

        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colLastLogin.setCellFactory(column -> new TableCell<User, Object>() {
             @Override
             protected void updateItem(Object date, boolean empty) {
                 super.updateItem(date, empty);
                 if (empty || date == null) {
                     setGraphic(null);
                 } else {
                     Label timeLbl = new Label(String.valueOf(date));
                     timeLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 12px;");
                     setGraphic(timeLbl);
                 }
             }
        });

        colActions.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    
                    // Edit Button
                    Button editBtn = new Button();
                    SVGPath editIcon = new SVGPath();
                    editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z");
                    editIcon.setFill(Color.web("#3b82f6"));
                    editIcon.setScaleX(0.7); editIcon.setScaleY(0.7);
                    editBtn.setGraphic(editIcon);
                    editBtn.getStyleClass().add("btn-edit");
                    editBtn.setOnAction(e -> handleEdit(user));

                    // Ban / Unban Button
                    Button flagBtn = new Button();
                    SVGPath flagIcon = new SVGPath();
                    flagIcon.setContent("M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6h-5.6z");
                    boolean isBanned = "BANNED".equalsIgnoreCase(user.getStatut());
                    flagIcon.setFill(isBanned ? Color.web("#dc2626") : Color.web("#64748B"));
                    flagIcon.setScaleX(0.7); flagIcon.setScaleY(0.7);
                    flagBtn.setGraphic(flagIcon);
                    flagBtn.setTooltip(new Tooltip(isBanned ? "Unban User" : "Ban User"));
                    flagBtn.setOnAction(e -> handleBanAction(user));

                    // Delete Button
                    Button deleteBtn = new Button();
                    SVGPath trashIcon = new SVGPath();
                    trashIcon.setContent("M 6 19 c 0 1.1 0.9 2 2 2 h 8 c 1.1 0 2 -0.9 2 -2 V 7 H 6 v 12 Z M 19 4 h -3.5 l -1 -1 h -5 l -1 1 H 5 v 2 h 14 V 4 Z");
                    trashIcon.setFill(Color.web("#ef4444"));
                    trashIcon.setScaleX(0.7); trashIcon.setScaleY(0.7);
                    deleteBtn.setGraphic(trashIcon);
                    deleteBtn.getStyleClass().add("btn-delete");
                    deleteBtn.setOnAction(e -> handleDelete(user));
                    
                    HBox box = new HBox(2, editBtn, flagBtn, deleteBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    private void handleBanAction(User user) {
        if ("BANNED".equalsIgnoreCase(user.getStatut())) {
            // Unban: simple confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unban User");
            confirm.setHeaderText("Unban " + user.getFullName() + "?");
            confirm.setContentText("This will restore the user's access to the platform.");
            confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
                try {
                    userService.unbanUser(user.getId());
                    refresh();
                } catch (SQLException e) {
                    showError("Failed to unban user: " + e.getMessage());
                }
            });
        } else {
            showBanDialog(user);
        }
    }

    private void showBanDialog(User user) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Ban User");
        dialog.setResizable(false);

        // Header
        Label nameLbl = new Label("Ban " + user.getFullName());
        nameLbl.setStyle("-fx-text-fill: #F1F5F9; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label emailLbl = new Label(user.getEmail());
        emailLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        VBox header = new VBox(3, nameLbl, emailLbl);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        // Reason input
        Label reasonLbl = new Label("Reason for ban:");
        reasonLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px;");
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Describe why this user is being banned…");
        reasonArea.setPrefRowCount(3);
        reasonArea.setWrapText(true);
        reasonArea.setStyle(
            "-fx-background-color: rgba(30,41,59,0.9);" +
            "-fx-text-fill: #E2E8F0;" +
            "-fx-prompt-text-fill: #475569;" +
            "-fx-border-color: rgba(99,102,241,0.4);" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-font-size: 13px;"
        );
        VBox.setVgrow(reasonArea, Priority.ALWAYS);

        Label errorLbl = new Label("Please provide a reason before confirming.");
        errorLbl.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        // Buttons
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: rgba(71,85,105,0.5);" +
            "-fx-text-fill: #CBD5E1;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-cursor: hand;"
        );
        Button confirmBtn = new Button("Confirm Ban");
        confirmBtn.setStyle(
            "-fx-background-color: linear-gradient(to right,#dc2626,#b91c1c);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian,rgba(220,38,38,0.4),8,0,0,2);"
        );

        HBox btnRow = new HBox(10, cancelBtn, confirmBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(14, header, sep, reasonLbl, reasonArea, errorLbl, btnRow);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1E293B;");

        cancelBtn.setOnAction(e -> dialog.close());
        confirmBtn.setOnAction(e -> {
            String reason = reasonArea.getText().trim();
            if (reason.isEmpty()) {
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }
            dialog.close();
            executeBan(user, reason);
        });

        dialog.setScene(new Scene(content, 460, 310));
        dialog.showAndWait();
    }

    private void executeBan(User user, String reason) {
        try {
            userService.banUser(user.getId(), reason);
        } catch (SQLException e) {
            showError("Failed to ban user: " + e.getMessage());
            return;
        }

        refresh();

        // Send notification email on a background thread
        Task<Void> emailTask = new Task<>() {
            @Override protected Void call() throws Exception {
                EmailService.sendBanNotification(user.getEmail(), reason);
                return null;
            }
        };
        emailTask.setOnSucceeded(e -> showInfo(
            "User " + user.getFullName() + " has been banned and notified by email."));
        emailTask.setOnFailed(e -> showInfo(
            "User " + user.getFullName() + " has been banned. (Email notification failed: "
            + emailTask.getException().getMessage() + ")"));
        new Thread(emailTask, "ban-email").start();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Error");
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Ban Confirmed");
        a.setContentText(message);
        a.showAndWait();
    }

    private void handleEdit(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/getion_user/edit_user.fxml"));
            VBox form = loader.load();
            EditUserController controller = loader.getController();
            controller.setListUserController(this);
            controller.setUserData(user);
            Stage stage = new Stage();
            stage.setTitle("Edit User");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(form));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete " + user.getFullName());
        alert.setContentText("Are you sure?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.supprimer(user.getId());
                refresh();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
