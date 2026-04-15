package controllers.group;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import models.Group;
import models.Invitation;
import models.Message;
import models.Project;
import models.User;
import services.InvitationService;
import services.MessageService;
import services.ProjectService;
import services.UserService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GroupDetailsController {
    @FXML
    private Label groupNameLabel;

    @FXML
    private Label statusPill;

    @FXML
    private Label placesChip;

    @FXML
    private Label createdChip;

    @FXML
    private Label creatorChip;

    @FXML
    private Label descriptionLabel;

    @FXML
    private ImageView photoView;

    @FXML
    private Label photoHintLabel;

    @FXML
    private ListView<Message> messagesList;

    @FXML
    private TextField messageField;

    @FXML
    private ListView<Project> projectsList;

    @FXML
    private Label projectsHintLabel;

    @FXML
    private ListView<String> membersList;

    @FXML
    private Label pointsLabel;

    @FXML
    private TextField inviteEmailField;

    @FXML
    private Label inviteStatusLabel;

    private final MessageService messageService = new MessageService();
    private final ProjectService projectService = new ProjectService();
    private final InvitationService invitationService = new InvitationService();
    private final UserService userService = new UserService();

    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<String> members = FXCollections.observableArrayList();

    private Group group;
    private Runnable onBack;

    public void setGroup(Group group) {
        this.group = group;
        render();
        loadData();
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void initialize() {
        messagesList.setItems(messages);
        projectsList.setItems(projects);
        membersList.setItems(members);

        messagesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getContent());
            }
        });

        projectsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getTitle() == null ? "(Sans titre)" : item.getTitle());
            }
        });

        render();
    }

    private void render() {
        if (groupNameLabel == null || group == null) {
            return;
        }

        groupNameLabel.setText(nullToDash(group.getCategory()));
        statusPill.setText("LIBRE");
        placesChip.setText(group.getCapacity() + " places");
        createdChip.setText("Cree le " + formatDate(group.getCreatedAt()));
        creatorChip.setText(resolveCreatorLabel(group.getCreatorId()));

        descriptionLabel.setText("Groupe d'etude dedie a " + nullToDash(group.getCategory())
                + ". Rejoignez ce groupe pour collaborer, partager des ressources et atteindre vos objectifs.");

        loadPhoto(group.getGroupPhoto());
    }

    private void loadPhoto(String urlOrPath) {
        photoHintLabel.setText("");
        photoView.setImage(null);

        String v = urlOrPath == null ? "" : urlOrPath.trim();
        if (v.isEmpty()) {
            photoHintLabel.setText("(Aucune photo)");
            return;
        }

        try {
            // Works for http(s) URLs or local absolute paths.
            Image img = new Image(v, true);
            photoView.setImage(img);
        } catch (RuntimeException e) {
            photoHintLabel.setText("(Photo invalide)");
        }
    }

    private void loadData() {
        if (group == null) {
            return;
        }

        loadMembers();
        loadMessages();
        loadProjects();
    }

    private void loadMembers() {
        members.clear();
        String creator = resolveCreatorLabel(group.getCreatorId());
        members.add(creator + " (Createur)");

        // No membership table wired yet. Keep UI consistent with Symfony screenshot.
        pointsLabel.setText("0 pts");
    }

    private void loadMessages() {
        messages.clear();
        try {
            List<Message> all = messageService.recuperer();
            messages.addAll(all.stream().filter(m -> m.getGroup_id() == group.getId()).collect(Collectors.toList()));
        } catch (SQLException | RuntimeException e) {
            // Leave empty; show placeholder line.
        }
    }

    private void loadProjects() {
        projects.clear();
        try {
            List<Project> all = projectService.recuperer();
            projects.addAll(all.stream().filter(p -> p.getGroup_id() == group.getId()).collect(Collectors.toList()));
        } catch (SQLException | RuntimeException e) {
            // Leave empty.
        }

        projectsHintLabel.setText(projects.isEmpty()
                ? "Aucun projet actif. Demandez au createur d'en creer un !"
                : "");
    }

    @FXML
    private void sendMessage() {
        if (group == null) {
            return;
        }

        String content = safeTrim(messageField.getText());
        if (content.isEmpty()) {
            return;
        }

        Message m = new Message();
        m.setContent(content);
        m.setCreated_at(new Timestamp(System.currentTimeMillis()));
        m.setSender_id(Math.max(group.getCreatorId(), 1));
        m.setGroup_id(group.getId());

        try {
            messageService.ajouter(m);
            messageField.clear();
            loadMessages();
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Envoi impossible: " + e.getMessage());
        }
    }

    @FXML
    private void inviteMember() {
        inviteStatusLabel.setText("");
        if (group == null) {
            return;
        }

        String email = safeTrim(inviteEmailField.getText()).toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            inviteStatusLabel.setText("Email requis.");
            return;
        }

        try {
            List<User> users = userService.recuperer();
            User receiver = users.stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(email))
                    .findFirst()
                    .orElse(null);

            if (receiver == null) {
                inviteStatusLabel.setText("Utilisateur introuvable pour cet email.");
                return;
            }

            Invitation inv = new Invitation();
            inv.setStatus("PENDING");
            inv.setCreated_at(new Timestamp(System.currentTimeMillis()));
            inv.setSender_id(Math.max(group.getCreatorId(), 1));
            inv.setReceiver_id(receiver.getId());
            inv.setGroup_id(group.getId());

            invitationService.ajouter(inv);
            inviteEmailField.clear();
            inviteStatusLabel.setText("Invitation envoyee.");
        } catch (SQLException | RuntimeException e) {
            inviteStatusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void addProject() {
        showAlert(Alert.AlertType.INFORMATION, "Projet", "Ajout de projet: ecran a brancher.");
    }

    @FXML
    private void backToGroups() {
        if (onBack != null) {
            onBack.run();
        }
    }

    private String resolveCreatorLabel(int creatorId) {
        if (creatorId <= 0) {
            return "Utilisateur";
        }
        return "user #" + creatorId;
    }

    private static String formatDate(Timestamp ts) {
        LocalDate d;
        if (ts == null) {
            d = LocalDate.now(ZoneId.systemDefault());
        } else {
            d = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}

