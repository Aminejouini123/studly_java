package controllers.group;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import models.Group;
import models.Invitation;
import models.Project;
import models.ProjectTask;
import models.User;
import services.GroupService;
import services.InvitationService;
import services.ProjectTaskService;
import services.UserService;
import services.ai.DescriptionAiService;
import utils.SessionManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class AddProjectTaskController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label projectHintLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField taskTitleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private DatePicker deadlinePicker;

    @FXML
    private TextField deadlineTimeField;

    @FXML
    private ComboBox<User> assigneeCombo;

    @FXML
    private Button saveButton;

    @FXML
    private Button generateDescriptionButton;

    private final ProjectTaskService projectTaskService = new ProjectTaskService();
    private final GroupService groupService = new GroupService();
    private final InvitationService invitationService = new InvitationService();
    private final UserService userService = new UserService();
    private final DescriptionAiService descriptionAiService = new DescriptionAiService();

    private Group group;
    private Project project;
    private ProjectTask taskToEdit;
    private Runnable onDone;
    private Runnable onCancel;

    public void setGroup(Group group) {
        this.group = group;
        refreshAssignees();
    }

    public void setProject(Project project) {
        this.project = project;
        renderProjectHint();
    }

    public void setTask(ProjectTask task) {
        this.taskToEdit = task;
        renderEditState();
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @FXML
    private void initialize() {
        if (statusCombo != null) {
            statusCombo.getItems().setAll("TO_DO", "IN_PROGRESS", "DONE");
            statusCombo.getSelectionModel().select("TO_DO");
        }
        if (deadlineTimeField != null) {
            deadlineTimeField.setText("18:00");
        }

        if (assigneeCombo != null) {
            assigneeCombo.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : displayUser(item));
                }
            });
            assigneeCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? "" : displayUser(item));
                }
            });
        }

        renderProjectHint();
        setStatus("");
        refreshAssignees();
        renderEditState();
    }

    @FXML
    private void generateDescription() {
        if (descriptionArea == null) {
            return;
        }

        String taskTitle = safeTrim(taskTitleField != null ? taskTitleField.getText() : null);
        String projectTitle = project == null ? "" : safeTrim(project.getTitle());
        String groupName = group == null ? "" : safeTrim(group.getCategory());
        String status = selectedStatus();
        LocalDate deadline = deadlinePicker != null ? deadlinePicker.getValue() : null;

        setStatus("");
        if (generateDescriptionButton != null) {
            generateDescriptionButton.setDisable(true);
        }
        descriptionArea.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return descriptionAiService.generateTaskDescription(taskTitle, projectTitle, groupName, status, deadline);
            }
        };
        task.setOnSucceeded(evt -> {
            String text = task.getValue();
            if (text != null && !text.trim().isEmpty()) {
                descriptionArea.setText(text.trim());
            }
            descriptionArea.setDisable(false);
            if (generateDescriptionButton != null) {
                generateDescriptionButton.setDisable(false);
            }
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            String msg = ex == null ? "Generation impossible." : ("Generation impossible: " + ex.getMessage());
            setStatus(msg);
            descriptionArea.setDisable(false);
            if (generateDescriptionButton != null) {
                generateDescriptionButton.setDisable(false);
            }
        });

        Thread th = new Thread(task, "ai-generate-task-description");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void save() {
        setStatus("");

        if (project == null) {
            showAlert(Alert.AlertType.ERROR, "Tache", "Aucun projet selectionne.");
            return;
        }
        if (project.getId() <= 0) {
            showAlert(Alert.AlertType.ERROR, "Tache", "Projet invalide (id=" + project.getId() + "). Rafraichissez la liste des projets puis reessayez.");
            return;
        }
        if (!groupService.isGroupCreator(group, SessionManager.getCurrentUser())) {
            showAlert(Alert.AlertType.WARNING, "Tache", "Acces refuse: seul le createur du groupe peut ajouter/modifier/assigner une tache.");
            return;
        }

        String title = safeTrim(taskTitleField != null ? taskTitleField.getText() : null);
        if (title.isEmpty()) {
            setStatus("Titre requis.");
            return;
        }

        ProjectTask t = new ProjectTask();
        if (taskToEdit != null) {
            t.setId(taskToEdit.getId());
        }
        t.setTitle(title);
        t.setDescription(safeTrim(descriptionArea != null ? descriptionArea.getText() : null));
        t.setStatus(selectedStatus());
        t.setDeadline(toTimestamp(deadlinePicker != null ? deadlinePicker.getValue() : null, deadlineTimeField != null ? deadlineTimeField.getText() : null));
        t.setCompleted_at(taskToEdit != null ? taskToEdit.getCompleted_at() : null);
        t.setDeliverable(taskToEdit != null ? taskToEdit.getDeliverable() : null);
        t.setGrade(taskToEdit != null ? taskToEdit.getGrade() : 0);
        t.setAttachment(taskToEdit != null ? taskToEdit.getAttachment() : null);
        t.setResource_path(taskToEdit != null ? taskToEdit.getResource_path() : null);
        t.setProject_id(project.getId());
        t.setAssigned_user_id(selectedAssigneeId());

        try {
            if (taskToEdit != null) {
                projectTaskService.modifier(t);
            } else {
                projectTaskService.ajouter(t);
            }
            if (onDone != null) {
                onDone.run();
            }
        } catch (SQLException | RuntimeException e) {
            String base = "Enregistrement impossible";
            String details = e.getMessage();
            if (details == null || details.trim().isEmpty()) {
                details = e.toString();
            }
            // Add context to help identify which FK fails (project_id vs assigned_user_id).
            if (e instanceof SQLException) {
                SQLException se = (SQLException) e;
                // Common case seen in this project: DB FK points to `user(id)` while the app uses `users(id)`.
                // In that case, any assigned_user_id coming from `users` will fail with error 1452.
                if (se.getErrorCode() == 1452
                        && details.contains("project_task")
                        && details.contains("assigned_user_id")
                        && details.contains("REFERENCES `user`")) {
                    details = "La base de donnees a une contrainte FK qui reference `user(id)` au lieu de `users(id)`.\n"
                            + "Comme l'application lit les utilisateurs depuis la table `users`, l'assignation echoue.\n\n"
                            + "Correctif SQL (exemple):\n"
                            + "ALTER TABLE project_task DROP FOREIGN KEY FK_6BEF133DADF66B1A;\n"
                            + "ALTER TABLE project_task ADD CONSTRAINT fk_project_task_user FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL;\n\n"
                            + "Puis reessayez.\n\n"
                            + "Details origine:\n" + details;
                }
                details = details
                        + "\nproject_id=" + t.getProject_id()
                        + ", assigned_user_id=" + t.getAssigned_user_id()
                        + "\nsqlState=" + se.getSQLState()
                        + ", errorCode=" + se.getErrorCode();
            } else {
                details = details + "\nproject_id=" + t.getProject_id() + ", assigned_user_id=" + t.getAssigned_user_id();
            }
            showAlert(Alert.AlertType.ERROR, "Tache", base + ": " + details);
        }
    }

    @FXML
    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        } else if (onDone != null) {
            onDone.run();
        }
    }

    private void renderProjectHint() {
        if (projectHintLabel == null) {
            return;
        }
        if (project == null) {
            projectHintLabel.setText("");
            return;
        }
        String name = safeTrim(project.getTitle());
        projectHintLabel.setText(name.isEmpty() ? ("Projet #" + project.getId()) : ("Projet: " + name));
    }

    private void renderEditState() {
        if (titleLabel == null && saveButton == null) {
            return;
        }

        if (taskToEdit == null) {
            if (titleLabel != null) {
                titleLabel.setText("Ajouter une Tache");
            }
            if (saveButton != null) {
                saveButton.setText("Ajouter la Tache");
            }
            return;
        }

        if (titleLabel != null) {
            titleLabel.setText("Modifier la Tache");
        }
        if (saveButton != null) {
            saveButton.setText("Mettre a jour");
        }
        if (taskTitleField != null) {
            taskTitleField.setText(taskToEdit.getTitle());
        }
        if (descriptionArea != null) {
            descriptionArea.setText(taskToEdit.getDescription());
        }
        if (statusCombo != null) {
            String s = taskToEdit.getStatus();
            if (s != null && !s.trim().isEmpty()) {
                statusCombo.getSelectionModel().select(s.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (taskToEdit.getDeadline() != null) {
            LocalDateTime ldt = taskToEdit.getDeadline().toLocalDateTime();
            if (deadlinePicker != null) {
                deadlinePicker.setValue(ldt.toLocalDate());
            }
            if (deadlineTimeField != null) {
                deadlineTimeField.setText(ldt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        }
        if (assigneeCombo != null) {
            User selected = assigneeCombo.getItems().stream()
                    .filter(u -> u != null && u.getId() == taskToEdit.getAssigned_user_id())
                    .findFirst()
                    .orElse(null);
            if (selected != null) {
                assigneeCombo.getSelectionModel().select(selected);
            }
        }
    }

    private void refreshAssignees() {
        if (assigneeCombo == null) {
            return;
        }
        assigneeCombo.getItems().clear();
        if (group == null) {
            return;
        }
        try {
            Set<Integer> memberIds = resolveMemberIds(group.getId(), group.getCreatorId());
            List<User> users = userService.recuperer();
            List<User> members = users.stream()
                    .filter(u -> u != null && memberIds.contains(u.getId()))
                    .sorted(Comparator.comparing(AddProjectTaskController::displayUser, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            // Ensure group creator can always assign tasks to themselves, even on legacy DBs
            // where creator_id may not align with the current users table ids.
            User currentUser = SessionManager.getCurrentUser();
            if (groupService.isGroupCreator(group, currentUser)
                    && currentUser != null
                    && currentUser.getId() > 0
                    && members.stream().noneMatch(u -> u != null && u.getId() == currentUser.getId())) {
                User self = users.stream()
                        .filter(u -> u != null && u.getId() == currentUser.getId())
                        .findFirst()
                        .orElse(currentUser);
                members.add(self);
                members.sort(Comparator.comparing(AddProjectTaskController::displayUser, String.CASE_INSENSITIVE_ORDER));
            }

            assigneeCombo.getItems().addAll(members);
            if (!members.isEmpty() && assigneeCombo.getSelectionModel().getSelectedItem() == null) {
                assigneeCombo.getSelectionModel().select(0);
            }
        } catch (SQLException | RuntimeException ignored) {
            // leave empty
        }
    }

    private Set<Integer> resolveMemberIds(int groupId, int creatorId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        ids.add(creatorId);
        List<Invitation> all = invitationService.recuperer();
        all.stream()
                .filter(i -> i != null && i.getGroup_id() == groupId)
                // Only allow assigning tasks to accepted members.
                .filter(i -> {
                    String s = i.getStatus() == null ? "" : i.getStatus().trim().toUpperCase(Locale.ROOT);
                    return s.equals("ACCEPTED");
                })
                .map(Invitation::getReceiver_id)
                .distinct()
                .filter(id -> id > 0)
                .forEach(ids::add);
        return ids.stream().collect(Collectors.toSet());
    }

    private String selectedStatus() {
        if (statusCombo == null) {
            return "TO_DO";
        }
        String v = statusCombo.getSelectionModel().getSelectedItem();
        return (v == null || v.trim().isEmpty()) ? "TO_DO" : v.trim().toUpperCase(Locale.ROOT);
    }

    private int selectedAssigneeId() {
        if (assigneeCombo == null) {
            return 0;
        }
        User u = assigneeCombo.getSelectionModel().getSelectedItem();
        return u == null ? 0 : u.getId();
    }

    private static Timestamp toTimestamp(LocalDate date, String timeText) {
        if (date == null) {
            return null;
        }
        LocalTime time = LocalTime.of(0, 0);
        String t = safeTrim(timeText);
        if (!t.isEmpty()) {
            try {
                time = LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException ignored) {
                // keep 00:00
            }
        }
        return Timestamp.valueOf(LocalDateTime.of(date, time));
    }

    private void setStatus(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg == null ? "" : msg);
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String displayUser(User u) {
        String first = u.getFirst_name() == null ? "" : u.getFirst_name().trim();
        String last = u.getLast_name() == null ? "" : u.getLast_name().trim();
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) {
            return full;
        }
        String email = u.getEmail() == null ? "" : u.getEmail().trim();
        if (!email.isEmpty()) {
            return email.toLowerCase(Locale.ROOT);
        }
        return "Utilisateur #" + u.getId();
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);

        String text = content == null ? "" : content;
        // Keep the main dialog readable; put long content into an expandable area.
        if (text.length() > 220) {
            alert.setContentText(text.substring(0, 220) + "...");

            TextArea area = new TextArea(text);
            area.setEditable(false);
            area.setWrapText(true);
            area.setMaxWidth(Double.MAX_VALUE);
            area.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(area, Priority.ALWAYS);
            GridPane.setHgrow(area, Priority.ALWAYS);

            GridPane pane = new GridPane();
            pane.setMaxWidth(Double.MAX_VALUE);
            pane.add(area, 0, 0);

            alert.getDialogPane().setExpandableContent(pane);
            alert.getDialogPane().setExpanded(true);
        } else {
            alert.setContentText(text);
        }
        alert.showAndWait();
    }
}
