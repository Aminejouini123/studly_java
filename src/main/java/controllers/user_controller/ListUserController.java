package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.User;
import services.UserService;

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
                boolean matchesFirstName = user.getFirst_name() != null && user.getFirst_name().toLowerCase().contains(currentSearchQuery);
                boolean matchesLastName = user.getLast_name() != null && user.getLast_name().toLowerCase().contains(currentSearchQuery);
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
            if (u1.getCreated_at() == null || u2.getCreated_at() == null) return 0;
            return descending ? u2.getCreated_at().compareTo(u1.getCreated_at()) 
                              : u1.getCreated_at().compareTo(u2.getCreated_at());
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
        colAvatar.setCellValueFactory(new PropertyValueFactory<>("first_name"));
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
                    Label nameLbl = new Label(user.getFirst_name() + " " + user.getLast_name());
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
                    } else if (status.equalsIgnoreCase("Flagged")) {
                        dot.setFill(Color.web("#f43f5e")); // Red for flagged
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

        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("created_at"));
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

                    // Flag Button (New)
                    Button flagBtn = new Button();
                    SVGPath flagIcon = new SVGPath();
                    flagIcon.setContent("M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6h-5.6z");
                    boolean isFlagged = "Flagged".equalsIgnoreCase(user.getStatut());
                    flagIcon.setFill(isFlagged ? Color.web("#f43f5e") : Color.web("#64748B"));
                    flagIcon.setScaleX(0.7); flagIcon.setScaleY(0.7);
                    flagBtn.setGraphic(flagIcon);
                    flagBtn.setTooltip(new Tooltip(isFlagged ? "Unflag User" : "Flag User"));
                    flagBtn.setOnAction(e -> handleToggleFlag(user));

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

    private void handleToggleFlag(User user) {
        try {
            String currentStatus = user.getStatut();
            if ("Flagged".equalsIgnoreCase(currentStatus)) {
                user.setStatut("Active");
            } else {
                user.setStatut("Flagged");
            }
            userService.modifier(user);
            refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        alert.setHeaderText("Delete " + user.getFirst_name() + " " + user.getLast_name());
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
