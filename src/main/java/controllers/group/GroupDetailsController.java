package controllers.group;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.StackPane;
import models.Group;
import models.Invitation;
import models.Message;
import models.Project;
import models.ProjectTask;
import models.User;
import services.GroupService;
import services.InvitationService;
import services.MessageService;
import services.ProjectService;
import services.ProjectTaskService;
import services.SmsService;
import services.UserService;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

    @FXML
    private Button inviteButton;

    private final MessageService messageService = new MessageService();
    private final ProjectService projectService = new ProjectService();
    private final ProjectTaskService projectTaskService = new ProjectTaskService();
    private final InvitationService invitationService = new InvitationService();
    private final GroupService groupService = new GroupService();
    private final UserService userService = new UserService();
    private final UserLabelResolver userLabelResolver = new UserLabelResolver(userService);
    private final SmsService smsService = new SmsService();

    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<String> members = FXCollections.observableArrayList();

    private final Map<Integer, List<ProjectTask>> projectTasksByProjectId = new HashMap<>();

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
                String author = userLabelResolver.resolve(item.getUser_id());
                String time = formatTime(item.getTimestamp());
                setText(time + " " + author + " : " + item.getContent());
            }
        });

        projectsList.setCellFactory(lv -> new ListCell<>() {
            private final VBox card = new VBox(8);
            private final HBox topRow = new HBox(10);
            private final Label title = new Label();
            private final Region spacer = new Region();
            private final Button editBtn = iconButton(pencilPath(), false);
            private final Button deleteBtn = iconButton(trashPath(), true);

            private final HBox tagRow = new HBox(8);
            private final Label typeTag = new Label();

            private final Label desc = new Label();

            private final HBox metaRow = new HBox(10);
            private final Label deadline = new Label();
            private final Region metaSpacer = new Region();
            private final Label resourceLink = new Label("Telecharger Resources");

            private final Separator sep = new Separator();
            private final HBox tasksHeader = new HBox(10);
            private final Label tasksTitle = new Label("Taches");
            private final Region tasksSpacer = new Region();
            private final Label addTaskLink = new Label("+ Ajouter une Tache");
            private final VBox tasksBox = new VBox(6);
            private final Label tasksEmpty = new Label("Aucune tache pour ce projet.");

            {
                card.getStyleClass().add("project-item");

                title.getStyleClass().add("project-title");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                editBtn.setOnAction(e -> {
                    Project p = getItem();
                    if (p != null) {
                        editProject(p);
                    }
                });
                deleteBtn.setOnAction(e -> {
                    Project p = getItem();
                    if (p != null) {
                        deleteProject(p);
                    }
                });
                topRow.getChildren().addAll(title, spacer, editBtn, deleteBtn);

                typeTag.getStyleClass().add("project-tag");
                tagRow.getChildren().add(typeTag);

                desc.getStyleClass().add("project-desc");
                desc.setWrapText(true);

                deadline.getStyleClass().add("project-meta");
                HBox.setHgrow(metaSpacer, Priority.ALWAYS);
                resourceLink.getStyleClass().add("project-link");
                resourceLink.setOnMouseClicked(e -> {
                    Project p = getItem();
                    if (p != null) {
                        openResource(p.getResource());
                    }
                });
                metaRow.getChildren().addAll(deadline, metaSpacer, resourceLink);

                tasksTitle.getStyleClass().add("project-subhead");
                tasksEmpty.getStyleClass().add("muted");
                HBox.setHgrow(tasksSpacer, Priority.ALWAYS);
                addTaskLink.getStyleClass().add("linkish");
                addTaskLink.setOnMouseClicked(e -> {
                    Project p = getItem();
                    if (p != null) {
                        addProjectTask(p);
                    }
                });
                tasksHeader.getChildren().addAll(tasksTitle, tasksSpacer, addTaskLink);

                card.getChildren().addAll(topRow, tagRow, desc, metaRow, sep, tasksHeader, tasksBox, tasksEmpty);
                setText(null);
            }

            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                title.setText(safeText(item.getTitle(), "(Sans titre)"));
                typeTag.setText(safeUpper(item.getType(), "TYPE"));
                desc.setText(truncate(safeText(item.getDescription(), ""), 140));
                deadline.setText(formatDeadline(item.getDeadline()));
                resourceLink.setVisible(item.getResource() != null && !item.getResource().trim().isEmpty());
                resourceLink.setManaged(resourceLink.isVisible());

                boolean creator = isCurrentUserCreator();
                addTaskLink.setDisable(!creator);

                List<ProjectTask> ts = projectTasksByProjectId.getOrDefault(item.getId(), Collections.emptyList());
                tasksBox.getChildren().clear();
                for (ProjectTask t : ts) {
                    if (t == null) {
                        continue;
                    }
                    tasksBox.getChildren().add(buildTaskRow(t, creator));
                }
                boolean hasTasks = !ts.isEmpty();
                tasksBox.setManaged(hasTasks);
                tasksBox.setVisible(hasTasks);
                tasksEmpty.setVisible(!hasTasks);
                tasksEmpty.setManaged(!hasTasks);

                setGraphic(card);
            }

            private Node buildTaskRow(ProjectTask task, boolean creator) {
                HBox row = new HBox(10);
                row.getStyleClass().add("task-row");

                VBox left = new VBox(2);
                Label tTitle = new Label(safeText(task.getTitle(), "(Sans titre)"));
                tTitle.getStyleClass().add("task-title");
                Label meta = new Label(formatTaskMeta(task));
                meta.getStyleClass().add("task-meta");
                left.getChildren().addAll(tTitle, meta);

                Label status = new Label(safeUpper(task.getStatus(), "TO_DO"));
                status.getStyleClass().add("task-status");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Button submit = iconButton(uploadPath(), false);
                // Default hidden; enabled only for creator or assignee.
                submit.setDisable(true);
                submit.setVisible(false);
                submit.setManaged(false);
                configureSubmitButton(submit, getItem(), task);

                Button edit = iconButton(pencilPath(), false);
                edit.setDisable(!creator);
                edit.setOnAction(e -> editProjectTask(getItem(), task));

                Button del = iconButton(trashPath(), true);
                del.setDisable(!creator);
                del.setOnAction(e -> deleteProjectTask(task));

                row.getChildren().addAll(left, status, sp, submit, edit, del);
                return row;
            }
        });

        render();
    }

    private static String formatDeadline(java.sql.Date d) {
        if (d == null) {
            return "Expire le: -";
        }
        try {
            LocalDate ld = d.toLocalDate();
            return "Expire le: " + ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException e) {
            return "Expire le: -";
        }
    }

    private void deleteProject(Project project) {
        if (project == null) {
            return;
        }
        if (!isCurrentUserCreator()) {
            showAlert(Alert.AlertType.WARNING, "Projet", "Seul le createur peut supprimer un projet.");
            return;
        }
        try {
            projectService.supprimer(project.getId());
            loadProjects();
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Projet", "Suppression impossible: " + e.getMessage());
        }
    }

    private void editProject(Project project) {
        if (group == null || project == null) {
            return;
        }
        if (!isCurrentUserCreator()) {
            showAlert(Alert.AlertType.WARNING, "Projet", "Seul le createur peut modifier un projet.");
            return;
        }

        StackPane host = findGroupHost(projectsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir la modification de projet (host introuvable).");
            return;
        }

        final Group currentGroup = group;
        final Runnable back = onBack;

        loadIntoHost(host, "/gestion_group/add_project.fxml", loader -> {
            AddProjectController controller = loader.getController();
            controller.setGroup(currentGroup);
            controller.setProject(project);
            controller.setOnDone(() -> reopenDetails(host, currentGroup, back));
            controller.setOnCancel(() -> reopenDetails(host, currentGroup, back));
        });
    }

    private static void openResource(String urlOrPath) {
        String v = urlOrPath == null ? "" : urlOrPath.trim();
        if (v.isEmpty()) {
            return;
        }
        try {
            if (v.startsWith("http://") || v.startsWith("https://")) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(v));
            } else {
                java.io.File f = new java.io.File(v);
                if (f.exists()) {
                    java.awt.Desktop.getDesktop().open(f);
                } else {
                    // If it's not a file, try to open it as a URI (e.g. file:///...)
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(v));
                }
            }
        } catch (Exception ignored) {
            // Keep silent: resource links are best-effort.
        }
    }

    private static Button iconButton(String svgPath, boolean danger) {
        Button b = new Button();
        b.getStyleClass().add("icon-btn");
        if (danger) {
            b.getStyleClass().add("icon-btn-danger");
        }
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setStyle("-fx-fill: #0F172A; -fx-opacity: 0.75;");
        if (danger) {
            icon.setStyle("-fx-fill: #DC2626; -fx-opacity: 0.9;");
        }
        b.setGraphic(icon);
        b.setFocusTraversable(false);
        b.setMnemonicParsing(false);
        return b;
    }

    private static String pencilPath() {
        return "M4 20h4l10.5-10.5-4-4L4 16v4Zm13.9-13.9 1.6-1.6a1 1 0 0 1 1.4 0l.6.6a1 1 0 0 1 0 1.4l-1.6 1.6-1.4-1.4Z";
    }

    private static String uploadPath() {
        // Simple upload icon path.
        return "M12 16V4m0 0 4 4m-4-4-4 4M4 20h16";
    }

    private static String trashPath() {
        return "M6 7h12l-1 13H7L6 7Zm3-3h6l1 2H8l1-2Z";
    }

    private static String safeText(String v, String fallback) {
        String s = v == null ? "" : v.trim();
        return s.isEmpty() ? fallback : s;
    }

    private static String safeUpper(String v, String fallback) {
        String s = v == null ? "" : v.trim();
        if (s.isEmpty()) {
            return fallback;
        }
        return s.toUpperCase(Locale.ROOT);
    }

    private static String truncate(String v, int max) {
        if (v == null) {
            return "";
        }
        String s = v.trim();
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private void render() {
        if (groupNameLabel == null || group == null) {
            return;
        }

        groupNameLabel.setText(nullToDash(group.getCategory()));
        statusPill.setText("LIBRE");
        placesChip.setText(group.getCapacity() + " places");
        createdChip.setText("Cree le " + formatDate(group.getCreatedAt()));
        creatorChip.setText(userLabelResolver.resolve(group.getCreatorId()));

        descriptionLabel.setText("Groupe d'etude dedie a " + nullToDash(group.getCategory())
                + ". Rejoignez ce groupe pour collaborer, partager des ressources et atteindre vos objectifs.");

        loadPhoto(group.getGroupPhoto());
        applyPermissions();
    }

    private void applyPermissions() {
        if (group == null || inviteEmailField == null) {
            return;
        }

        boolean creator = isCurrentUserCreator();
        inviteEmailField.setDisable(!creator);
        if (inviteButton != null) {
            inviteButton.setDisable(!creator);
        }

        if (!creator) {
            inviteStatusLabel.setText("Seul le createur peut inviter des membres.");
        } else {
            // Keep any success/error message until user changes it.
            if (inviteStatusLabel != null && "Seul le createur peut inviter des membres.".equals(inviteStatusLabel.getText())) {
                inviteStatusLabel.setText("");
            }
        }
    }

    private boolean isCurrentUserCreator() {
        return groupService.isGroupCreator(group, SessionManager.getCurrentUser());
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
        String creator = userLabelResolver.resolve(group.getCreatorId());
        members.add(creator + " (Createur)");

        // "Membership" is currently derived from accepted invitations.
        // Creator is always a member.
        try {
            List<Invitation> all = invitationService.recuperer();
            all.stream()
                    .filter(i -> i.getGroup_id() == group.getId())
                    .filter(i -> i.getStatus() != null && i.getStatus().toUpperCase(Locale.ROOT).contains("ACCEPTED"))
                    .map(Invitation::getReceiver_id)
                    .distinct()
                    .filter(id -> id > 0 && id != group.getCreatorId())
                    .forEach(id -> members.add(userLabelResolver.resolve(id) + " (Membre)"));
        } catch (SQLException | RuntimeException ignored) {
            // Keep at least creator.
        }

        // No scoring system wired yet.
        pointsLabel.setText("0 pts");
    }

    private void loadMessages() {
        messages.clear();
        try {
            messages.addAll(messageService.getMessagesByGroup(group.getId()));
        } catch (SQLException | RuntimeException e) {
            // Leave empty; show placeholder line.
        }
    }

    private void loadProjects() {
        projects.clear();
        try {
            projects.addAll(projectService.getProjectsByGroup(group.getId()));
        } catch (SQLException | RuntimeException e) {
            // Leave empty.
        }

        projectsHintLabel.setText(projects.isEmpty()
                ? "Aucun projet actif. Demandez au createur d'en creer un !"
                : "");

        loadProjectTasks();
    }

    private void loadProjectTasks() {
        projectTasksByProjectId.clear();
        try {
            List<ProjectTask> all = projectTaskService.recuperer();
            for (ProjectTask t : all) {
                if (t == null || t.getProject_id() <= 0) {
                    continue;
                }
                projectTasksByProjectId.computeIfAbsent(t.getProject_id(), k -> new ArrayList<>()).add(t);
            }
        } catch (SQLException | RuntimeException ignored) {
            // best-effort
        }
    }

    private void addProjectTask(Project project) {
        if (group == null || project == null) {
            return;
        }
        if (!isCurrentUserCreator()) {
            showAlert(Alert.AlertType.WARNING, "Tache", "Seul le createur peut ajouter des taches.");
            return;
        }

        StackPane host = findGroupHost(projectsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir l'ajout de tache (host introuvable).");
            return;
        }

        final Group currentGroup = group;
        final Runnable back = onBack;

        loadIntoHost(host, "/gestion_group/add_task.fxml", loader -> {
            AddProjectTaskController controller = loader.getController();
            controller.setGroup(currentGroup);
            controller.setProject(project);
            controller.setOnDone(() -> reopenDetails(host, currentGroup, back));
            controller.setOnCancel(() -> reopenDetails(host, currentGroup, back));
        });
    }

    private void editProjectTask(Project project, ProjectTask task) {
        if (group == null || project == null || task == null) {
            return;
        }
        if (!isCurrentUserCreator()) {
            showAlert(Alert.AlertType.WARNING, "Tache", "Seul le createur peut modifier des taches.");
            return;
        }

        StackPane host = findGroupHost(projectsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir la modification de tache (host introuvable).");
            return;
        }

        final Group currentGroup = group;
        final Runnable back = onBack;

        loadIntoHost(host, "/gestion_group/add_task.fxml", loader -> {
            AddProjectTaskController controller = loader.getController();
            controller.setGroup(currentGroup);
            controller.setProject(project);
            controller.setTask(task);
            controller.setOnDone(() -> reopenDetails(host, currentGroup, back));
            controller.setOnCancel(() -> reopenDetails(host, currentGroup, back));
        });
    }

    private void deleteProjectTask(ProjectTask task) {
        if (task == null) {
            return;
        }
        if (!isCurrentUserCreator()) {
            showAlert(Alert.AlertType.WARNING, "Tache", "Seul le createur peut supprimer des taches.");
            return;
        }
        try {
            projectTaskService.supprimer(task.getId());
            loadProjectTasks();
            if (projectsList != null) {
                projectsList.refresh();
            }
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Tache", "Suppression impossible: " + e.getMessage());
        }
    }

    private String formatTaskMeta(ProjectTask t) {
        if (t == null) {
            return "";
        }
        String d = "-";
        if (t.getDeadline() != null) {
            try {
                LocalDateTime ldt = t.getDeadline().toLocalDateTime();
                d = ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (RuntimeException ignored) {
            }
        }

        String assignee = "-";
        if (t.getAssigned_user_id() > 0) {
            assignee = userLabelResolver.resolve(t.getAssigned_user_id());
        }

        String submitted = "";
        if (canSeeSubmission(t) && hasSubmission(t)) {
            submitted = " | Travail depose";
        }

        return "Limite: " + d + " | Assigne a: " + assignee + submitted;
    }

    private void configureSubmitButton(Button submitBtn, Project project, ProjectTask task) {
        if (submitBtn == null || task == null || project == null) {
            return;
        }
        User current = SessionManager.getCurrentUser();
        int currentId = current == null ? 0 : current.getId();

        boolean creator = groupService.isGroupCreator(group, current);
        boolean assignee = currentId > 0 && currentId == task.getAssigned_user_id();
        boolean canOpen = creator || assignee;
        if (!canOpen) {
            submitBtn.setVisible(false);
            submitBtn.setManaged(false);
            return;
        }

        boolean alreadySubmitted = hasSubmission(task);
        String tip = assignee
                ? (alreadySubmitted ? "Voir / mettre a jour le depot" : "Deposer le travail")
                : (alreadySubmitted ? "Voir le depot" : "Voir (aucun depot)");

        submitBtn.setVisible(true);
        submitBtn.setManaged(true);
        submitBtn.setDisable(false);
        submitBtn.setOnAction(e -> openSubmitTask(project, task));
        Tooltip.install(submitBtn, new Tooltip(tip));
    }

    private void openSubmitTask(Project project, ProjectTask task) {
        if (group == null || project == null || task == null) {
            return;
        }
        StackPane host = findGroupHost(projectsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir le depot (host introuvable).");
            return;
        }

        final Group currentGroup = group;
        final Runnable back = onBack;

        loadIntoHost(host, "/gestion_group/submit_task.fxml", loader -> {
            SubmitProjectTaskController controller = loader.getController();
            controller.setGroup(currentGroup);
            controller.setProject(project);
            controller.setTask(task);
            controller.setOnDone(() -> reopenDetails(host, currentGroup, back));
            controller.setOnCancel(() -> reopenDetails(host, currentGroup, back));
        });
    }

    private boolean canSeeSubmission(ProjectTask t) {
        if (t == null) return false;
        User current = SessionManager.getCurrentUser();
        int currentId = current == null ? 0 : current.getId();
        return groupService.isGroupCreator(group, current)
                || (currentId > 0 && currentId == t.getAssigned_user_id());
    }

    private static boolean hasSubmission(ProjectTask t) {
        if (t == null) return false;
        if (t.getCompleted_at() != null) return true;
        String d = t.getDeliverable();
        if (d != null && !d.trim().isEmpty()) return true;
        String a = t.getAttachment();
        return a != null && !a.trim().isEmpty();
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

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.WARNING, "Connexion requise", "Veuillez vous connecter pour envoyer un message.");
            return;
        }

        Message m = new Message();
        m.setContent(content);
        m.setTimestamp(new Timestamp(System.currentTimeMillis()));
        m.setUser_id(current.getId());
        m.setGroup_id(group.getId());

        try {
            messageService.addMessage(m);
            messageField.clear();
            loadMessages();
            if (!messages.isEmpty()) {
                messagesList.scrollTo(messages.size() - 1);
            }
        } catch (SQLException e) {
            String details = e.getMessage();
            if (details == null || details.trim().isEmpty()) {
                details = e.toString();
            }
            // Same class of issue seen elsewhere in this project:
            // DB FK may reference `user(id)` while the application uses `users(id)`.
            if (e.getErrorCode() == 1452
                    && details.contains("message")
                    && details.contains("sender_id")
                    && details.contains("REFERENCES `user`")) {
                details = "La base de donnees a une contrainte FK qui reference `user(id)` au lieu de `users(id)`.\n"
                        + "Comme l'application utilise la table `users`, certains comptes (ex: membres invites) ne peuvent pas envoyer de messages.\n\n"
                        + "Correctif SQL (exemple):\n"
                        + "ALTER TABLE message DROP FOREIGN KEY fk_message_sender;\n"
                        + "ALTER TABLE message ADD CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE;\n\n"
                        + "Si le nom de la FK est different, verifiez-le via votre outil SQL (phpMyAdmin / Workbench) puis adaptez la commande.\n\n"
                        + "Details origine:\n" + details;
            } else {
                details = details + "\nerrorCode=" + e.getErrorCode() + ", sqlState=" + e.getSQLState();
            }
            showAlert(Alert.AlertType.ERROR, "Erreur", "Envoi impossible: " + details);
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Envoi impossible: " + e.getMessage());
        }
    }

    @FXML
    private void inviteMember() {
        inviteStatusLabel.setText("");
        if (group == null) {
            return;
        }
        if (group.getId() <= 0) {
            inviteStatusLabel.setText("Groupe invalide (id manquant).");
            return;
        }

        if (!isCurrentUserCreator()) {
            inviteStatusLabel.setText("Seul le createur peut inviter des membres.");
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            inviteStatusLabel.setText("Veuillez vous connecter.");
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

            if (receiver.getId() == current.getId()) {
                inviteStatusLabel.setText("Vous ne pouvez pas vous inviter vous-meme.");
                return;
            }

            // DB must contain this group id, otherwise the invitation insert will fail with a FK constraint error.
            try {
                if (!groupService.existsById(group.getId())) {
                    inviteStatusLabel.setText("Ce groupe n'existe pas en base. Creez le groupe avant d'inviter.");
                    return;
                }
            } catch (SQLException e) {
                inviteStatusLabel.setText("Impossible de verifier le groupe en base: " + e.getMessage());
                return;
            }

            Invitation inv = new Invitation();
            inv.setStatus("PENDING");
            inv.setCreated_at(new Timestamp(System.currentTimeMillis()));
            inv.setSender_id(current.getId());
            inv.setReceiver_id(receiver.getId());
            inv.setGroup_id(group.getId());

            invitationService.ajouter(inv);

            SmsService.Result smsResult = smsService.sendGroupInvitationSms(
                    receiver.getPhoneNumber(),
                    resolveGroupNameForSms(group)
            );
            inviteEmailField.clear();
            if (smsResult.isSent()) {
                inviteStatusLabel.setText("Invitation envoyee.");
            } else {
                // Keep existing internal invitation behaviour even if SMS cannot be sent.
                // We intentionally avoid exposing low-level provider errors in the UI.
                inviteStatusLabel.setText("Invitation envoyee (SMS non envoye).");
            }
        } catch (SQLException | RuntimeException e) {
            inviteStatusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private static String resolveGroupNameForSms(Group group) {
        if (group == null) return "Groupe";
        String name = group.getCategory();
        if (name == null) return "Groupe #" + group.getId();
        String t = name.trim();
        return t.isEmpty() ? ("Groupe #" + group.getId()) : t;
    }

    @FXML
    private void addProject() {
        if (group == null) {
            return;
        }
        if (!isCurrentUserCreator()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul le createur du groupe peut ajouter un projet.");
            return;
        }

        StackPane host = findGroupHost(projectsList);
        if (host == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir l'ajout de projet (host introuvable).");
            return;
        }

        final Group currentGroup = group;
        final Runnable back = onBack;

        loadIntoHost(host, "/gestion_group/add_project.fxml", loader -> {
            AddProjectController controller = loader.getController();
            controller.setGroup(currentGroup);
            controller.setOnSaved(() -> {
                // no-op: refresh happens when re-opening details
            });
            controller.setOnDone(() -> reopenDetails(host, currentGroup, back));
            controller.setOnCancel(() -> reopenDetails(host, currentGroup, back));
        });
    }

    @FXML
    private void backToGroups() {
        if (onBack != null) {
            onBack.run();
        }
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

    private static String formatTime(Timestamp ts) {
        LocalTime t;
        if (ts == null) {
            t = LocalTime.now(ZoneId.systemDefault());
        } else {
            t = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        }
        return t.format(DateTimeFormatter.ofPattern("[HH:mm]"));
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

    private void reopenDetails(StackPane host, Group group, Runnable back) {
        loadIntoHost(host, "/gestion_group/group_details.fxml", loader -> {
            GroupDetailsController controller = loader.getController();
            controller.setGroup(group);
            if (back != null) {
                controller.setOnBack(back);
            }
        });
    }

    private static StackPane findGroupHost(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof StackPane) {
                StackPane sp = (StackPane) current;
                String id = sp.getId();
                if ("groupContentHost".equals(id) || "contentHost".equals(id)) {
                    return sp;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private static String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}

