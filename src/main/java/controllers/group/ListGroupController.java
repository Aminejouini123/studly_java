package controllers.group;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.Group;
import services.GroupService;
import services.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ListGroupController {
    @FXML
    private TextField searchField;

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<Group> groupsList;

    @FXML
    private VBox emptyStateBox;

    private final GroupService groupService = new GroupService();
    private final UserService userService = new UserService();
    private final UserLabelResolver userLabelResolver = new UserLabelResolver(userService);
    private final ObservableList<Group> masterData = FXCollections.observableArrayList();
    private FilteredList<Group> filtered;

    @FXML
    public void initialize() {
        filtered = new FilteredList<>(masterData, g -> true);
        groupsList.setItems(filtered);
        groupsList.setCellFactory(lv -> new GroupCardCell());

        searchField.textProperty().addListener((obs, oldV, newV) -> applySearch());

        refresh();
    }

    @FXML
    private void applySearch() {
        String q = safeTrim(searchField.getText()).toLowerCase(Locale.ROOT);
        filtered.setPredicate(g -> {
            if (q.isEmpty()) return true;
            String cat = g.getCategory() == null ? "" : g.getCategory().toLowerCase(Locale.ROOT);
            return cat.contains(q);
        });
        updateEmptyState();
    }

    @FXML
    private void openAddGroup() {
        StackPane host = findGroupHost(groupsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Unable to open Add Group (host not found).");
            return;
        }

        loadIntoHost(host, "/gestion_group/add_group.fxml", loader -> {
            AddGroupController controller = loader.getController();
            controller.setOnSaved(this::refresh);
            controller.setOnDone(() -> loadListIntoHost(host));
            controller.setOnCancel(() -> loadListIntoHost(host));
        });
    }

    private void openDetails(Group group) {
        StackPane host = findGroupHost(groupsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Unable to open Details (host not found).");
            return;
        }

        loadIntoHost(host, "/gestion_group/group_details.fxml", loader -> {
            GroupDetailsController controller = loader.getController();
            controller.setGroup(group);
            controller.setOnBack(() -> loadListIntoHost(host));
        });
    }

    private void openEdit(Group group) {
        StackPane host = findGroupHost(groupsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Unable to open Edit (host not found).");
            return;
        }

        loadIntoHost(host, "/gestion_group/edit_group.fxml", loader -> {
            EditGroupController controller = loader.getController();
            controller.setGroup(group);
            controller.setOnSaved(this::refresh);
            controller.setOnDone(() -> loadListIntoHost(host));
            controller.setOnCancel(() -> loadListIntoHost(host));
        });
    }

    private void deleteGroup(Group group) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce groupe ?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            groupService.supprimer(group.getId());
            refresh();
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible: " + e.getMessage());
        }
    }

    private void refresh() {
        statusLabel.setText("");
        masterData.clear();
        try {
            List<Group> groups = groupService.recuperer();
            masterData.addAll(groups);
            statusLabel.setText(groups.size() + " groupe(s).");
        } catch (SQLException | RuntimeException e) {
            statusLabel.setText("Impossible de charger les groupes: " + e.getMessage());
        }
        applySearch();
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = filtered == null || filtered.isEmpty();
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        groupsList.setVisible(!empty);
        groupsList.setManaged(!empty);
    }

    private void loadListIntoHost(StackPane host) {
        loadIntoHost(host, "/gestion_group/list_group.fxml", loader -> {
        });
    }

    private interface LoaderConsumer {
        void accept(FXMLLoader loader);
    }

    private void loadIntoHost(StackPane host, String fxmlPath, LoaderConsumer consumer) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            consumer.accept(loader);
            host.getChildren().setAll(view);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'ecran: " + e.getMessage());
        }
    }

    private static StackPane findGroupHost(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof StackPane) {
                StackPane sp = (StackPane) current;
                if ("groupContentHost".equals(sp.getId())) {
                    return sp;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private final class GroupCardCell extends ListCell<Group> {
        private final VBox card = new VBox(12);
        private final HBox header = new HBox(10);
        private final VBox titleBox = new VBox(3);
        private final Label nameLabel = new Label();
        private final Label creatorLabel = new Label();
        private final Region spacer = new Region();
        private final Label badgeLibre = new Label("LIBRE");

        private final VBox infoStrip = new VBox(8);
        private final HBox infoRow = new HBox(18);
        private final Label placesLabel = new Label();
        private final Label dateLabel = new Label();
        private final Label photoLabel = new Label("Photo");

        private final Label tagLabel = new Label("Groupe");
        private final Button detailsButton = new Button("Voir les details ->");
        private final HBox actionsRow = new HBox(10);
        private final Button editButton = new Button("Modifier");
        private final Button deleteButton = new Button("Supprimer");

        GroupCardCell() {
            card.getStyleClass().add("group-card");

            nameLabel.getStyleClass().add("group-name");
            creatorLabel.getStyleClass().add("group-meta");
            badgeLibre.getStyleClass().addAll("pill", "pill-danger");

            spacer.setMinWidth(0);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            titleBox.getChildren().addAll(nameLabel, creatorLabel);
            header.getChildren().addAll(titleBox, spacer, badgeLibre);

            infoStrip.getStyleClass().add("info-strip");
            placesLabel.getStyleClass().add("info-text");
            dateLabel.getStyleClass().add("info-text");
            photoLabel.getStyleClass().add("info-text");
            infoRow.getChildren().addAll(placesLabel, dateLabel, photoLabel);
            infoStrip.getChildren().add(infoRow);

            tagLabel.getStyleClass().add("tag");

            detailsButton.getStyleClass().add("btn-primary");
            detailsButton.setMaxWidth(Double.MAX_VALUE);

            editButton.getStyleClass().add("btn-secondary");
            deleteButton.getStyleClass().add("btn-danger");
            editButton.setMaxWidth(Double.MAX_VALUE);
            deleteButton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editButton, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(deleteButton, javafx.scene.layout.Priority.ALWAYS);
            actionsRow.getChildren().addAll(editButton, deleteButton);

            card.getChildren().addAll(header, infoStrip, tagLabel, detailsButton, actionsRow);

            detailsButton.setOnAction(e -> {
                Group g = getItem();
                if (g != null) openDetails(g);
            });
            editButton.setOnAction(e -> {
                Group g = getItem();
                if (g != null) openEdit(g);
            });
            deleteButton.setOnAction(e -> {
                Group g = getItem();
                if (g != null) deleteGroup(g);
            });
        }

        @Override
        protected void updateItem(Group group, boolean empty) {
            super.updateItem(group, empty);
            if (empty || group == null) {
                setGraphic(null);
                return;
            }

            nameLabel.setText(nullToDash(group.getCategory()));
            creatorLabel.setText(userLabelResolver.resolve(group.getCreatorId()) + " (Createur)");
            placesLabel.setText(group.getCapacity() + " places");
            dateLabel.setText("Cree le " + formatDate(group));

            setGraphic(card);
        }

        private String formatDate(Group group) {
            if (group.getCreatedAt() == null) {
                return LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            LocalDate d = group.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        private String nullToDash(String value) {
            return (value == null || value.isBlank()) ? "-" : value;
        }
    }
}

