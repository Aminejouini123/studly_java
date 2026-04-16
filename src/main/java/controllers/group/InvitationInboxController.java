package controllers.group;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Group;
import models.Invitation;
import models.User;
import services.GroupService;
import services.InvitationService;
import services.UserService;
import utils.SessionManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class InvitationInboxController {
    @FXML private ListView<Invitation> invitesList;
    @FXML private Label statusLabel;

    private final InvitationService invitationService = new InvitationService();
    private final UserService userService = new UserService();
    private final GroupService groupService = new GroupService();

    private final ObservableList<Invitation> data = FXCollections.observableArrayList();

    private final Map<Integer, User> usersById = new HashMap<>();
    private boolean usersLoaded;

    private final Map<Integer, Group> groupsById = new HashMap<>();
    private boolean groupsLoaded;

    @FXML
    public void initialize() {
        invitesList.setItems(data);
        invitesList.setCellFactory(lv -> new InvitationCell());
        refresh();
    }

    @FXML
    public void refresh() {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            data.clear();
            statusLabel.setText("Veuillez vous connecter.");
            return;
        }

        try {
            List<Invitation> all = invitationService.recuperer();
            List<Invitation> mine = all.stream()
                    .filter(i -> i.getReceiver_id() == current.getId())
                    .filter(i -> i.getStatus() != null && i.getStatus().toUpperCase(Locale.ROOT).contains("PENDING"))
                    .sorted((a, b) -> {
                        Timestamp ta = a.getCreated_at();
                        Timestamp tb = b.getCreated_at();
                        if (ta == null || tb == null) return 0;
                        return tb.compareTo(ta);
                    })
                    .collect(Collectors.toList());

            data.setAll(mine);
            statusLabel.setText(mine.isEmpty() ? "Aucune invitation en attente." : (mine.size() + " invitation(s) en attente."));
        } catch (SQLException | RuntimeException e) {
            data.clear();
            statusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private void accept(Invitation inv) {
        inv.setStatus("ACCEPTED");
        update(inv);
    }

    private void decline(Invitation inv) {
        inv.setStatus("DECLINED");
        update(inv);
    }

    private void update(Invitation inv) {
        try {
            invitationService.modifier(inv);
            refresh();
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Operation impossible: " + e.getMessage());
        }
    }

    private User resolveUser(int id) {
        if (id <= 0) return null;
        if (!usersLoaded) {
            try {
                for (User u : userService.recuperer()) {
                    usersById.put(u.getId(), u);
                }
            } catch (SQLException | RuntimeException ignored) {
            } finally {
                usersLoaded = true;
            }
        }
        return usersById.get(id);
    }

    private Group resolveGroup(int id) {
        if (id <= 0) return null;
        if (!groupsLoaded) {
            try {
                for (Group g : groupService.recuperer()) {
                    groupsById.put(g.getId(), g);
                }
            } catch (SQLException | RuntimeException ignored) {
            } finally {
                groupsLoaded = true;
            }
        }
        return groupsById.get(id);
    }

    private static String displayName(User u) {
        if (u == null) return "Utilisateur";
        String first = u.getFirst_name() == null ? "" : u.getFirst_name().trim();
        String last = u.getLast_name() == null ? "" : u.getLast_name().trim();
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        String email = u.getEmail() == null ? "" : u.getEmail().trim();
        return email.isEmpty() ? "Utilisateur" : email.toLowerCase(Locale.ROOT);
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) return "";
        return ts.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private final class InvitationCell extends ListCell<Invitation> {
        private final VBox root = new VBox(6);
        private final HBox top = new HBox(10);
        private final VBox text = new VBox(2);
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Region spacer = new Region();
        private final HBox actions = new HBox(8);
        private final Button acceptBtn = new Button("Accepter");
        private final Button declineBtn = new Button("Refuser");

        InvitationCell() {
            root.getStyleClass().add("glass-card");
            root.setStyle("-fx-background-radius: 12; -fx-padding: 12;");

            title.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A;");
            meta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;");

            HBox.setHgrow(spacer, Priority.ALWAYS);

            acceptBtn.getStyleClass().add("btn-primary");
            declineBtn.getStyleClass().add("btn-secondary");

            actions.getChildren().addAll(acceptBtn, declineBtn);
            text.getChildren().addAll(title, meta);
            top.getChildren().addAll(text, spacer, actions);
            root.getChildren().add(top);
        }

        @Override
        protected void updateItem(Invitation inv, boolean empty) {
            super.updateItem(inv, empty);
            if (empty || inv == null) {
                setGraphic(null);
                return;
            }

            Group g = resolveGroup(inv.getGroup_id());
            User sender = resolveUser(inv.getSender_id());

            String groupName = g == null ? ("Groupe #" + inv.getGroup_id()) : (g.getCategory() == null ? ("Groupe #" + g.getId()) : g.getCategory());
            title.setText(groupName);
            meta.setText("Invite par " + displayName(sender) + " • " + formatTs(inv.getCreated_at()));

            acceptBtn.setOnAction(e -> accept(inv));
            declineBtn.setOnAction(e -> decline(inv));

            setGraphic(root);
        }
    }
}

