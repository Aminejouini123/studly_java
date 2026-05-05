package controllers.group;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import models.Group;
import models.Project;
import models.ProjectTask;
import models.User;
import services.GroupService;
import services.ProjectTaskService;
import utils.SessionManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Assignee submits their work for a task:
 * - saves deliverable + attachment
 * - sets status DONE + completed_at now
 *
 * Creator can view submission (read-only).
 */
public class SubmitProjectTaskController {
    @FXML private Label titleLabel;
    @FXML private Label projectHintLabel;
    @FXML private Label taskHintLabel;
    @FXML private Label statusLabel;

    @FXML private TextArea deliverableArea;
    @FXML private TextField attachmentField;
    @FXML private Button browseButton;
    @FXML private Button downloadButton;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private final ProjectTaskService projectTaskService = new ProjectTaskService();
    private final GroupService groupService = new GroupService();

    private Group group;
    private Project project;
    private ProjectTask task;
    private Runnable onDone;
    private Runnable onCancel;

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setProject(Project project) {
        this.project = project;
        renderHints();
    }

    public void setTask(ProjectTask task) {
        this.task = task;
        renderHints();
        renderForm();
    }

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @FXML
    private void initialize() {
        setStatus("");
        renderHints();
        renderForm();
    }

    private void renderHints() {
        if (projectHintLabel != null) {
            if (project == null) {
                projectHintLabel.setText("");
            } else {
                String name = safeTrim(project.getTitle());
                projectHintLabel.setText(name.isEmpty() ? ("Projet #" + project.getId()) : ("Projet: " + name));
            }
        }
        if (taskHintLabel != null) {
            if (task == null) {
                taskHintLabel.setText("");
            } else {
                String name = safeTrim(task.getTitle());
                taskHintLabel.setText(name.isEmpty() ? ("Tache #" + task.getId()) : ("Tache: " + name));
            }
        }
    }

    private void renderForm() {
        if (task == null) {
            return;
        }
        if (deliverableArea != null) {
            deliverableArea.setText(task.getDeliverable() == null ? "" : task.getDeliverable());
        }
        if (attachmentField != null) {
            attachmentField.setText(task.getAttachment() == null ? "" : task.getAttachment());
        }

        User current = SessionManager.getCurrentUser();
        int currentId = current == null ? 0 : current.getId();
        boolean isAssignee = currentId > 0 && currentId == task.getAssigned_user_id();
        boolean isCreator = groupService.isGroupCreator(group, current);

        boolean canView = isAssignee || isCreator;
        if (!canView) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'assigne ou le createur peut voir ce depot.");
            if (onCancel != null) onCancel.run();
            else if (onDone != null) onDone.run();
            return;
        }

        boolean editable = isAssignee;
        if (deliverableArea != null) {
            deliverableArea.setEditable(editable);
        }
        if (attachmentField != null) {
            attachmentField.setEditable(false); // always chosen via file picker
        }
        if (browseButton != null) {
            browseButton.setDisable(!editable);
        }
        if (downloadButton != null) {
            boolean hasAttachment = task.getAttachment() != null && !task.getAttachment().trim().isEmpty();
            downloadButton.setVisible(hasAttachment);
            downloadButton.setManaged(hasAttachment);
            downloadButton.setDisable(!hasAttachment);
        }
        if (submitButton != null) {
            submitButton.setVisible(editable);
            submitButton.setManaged(editable);
        }
        if (titleLabel != null) {
            if (editable) {
                titleLabel.setText("Deposer le Travail");
            } else {
                titleLabel.setText("Travail Depose");
            }
        }
    }

    @FXML
    private void browseAttachment() {
        if (attachmentField == null) {
            return;
        }
        Window owner = null;
        if (browseButton != null && browseButton.getScene() != null) {
            owner = browseButton.getScene().getWindow();
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        java.io.File file = chooser.showOpenDialog(owner);
        if (file != null) {
            attachmentField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void downloadAttachment() {
        if (task == null) {
            return;
        }
        User current = SessionManager.getCurrentUser();
        boolean canView = current != null
                && (groupService.isGroupCreator(group, current) || current.getId() == task.getAssigned_user_id());
        if (!canView) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'assigne ou le createur peut telecharger ce fichier.");
            return;
        }
        String stored = safeTrim(task.getAttachment());
        if (stored.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Telechargement", "Aucun fichier depose.");
            return;
        }

        File src = new File(stored);
        if (!src.exists() || !src.isFile()) {
            showAlert(Alert.AlertType.ERROR, "Telechargement", "Fichier introuvable sur ce poste: " + stored);
            return;
        }

        Window owner = null;
        if (downloadButton != null && downloadButton.getScene() != null) {
            owner = downloadButton.getScene().getWindow();
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le fichier");
        chooser.setInitialFileName(src.getName());
        File dest = chooser.showSaveDialog(owner);
        if (dest == null) {
            return;
        }

        try {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert(Alert.AlertType.INFORMATION, "Telechargement", "Fichier enregistre: " + dest.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Telechargement", "Impossible d'enregistrer: " + e.getMessage());
        }
    }

    @FXML
    private void submit() {
        setStatus("");
        if (task == null) {
            showAlert(Alert.AlertType.ERROR, "Depot", "Tache introuvable.");
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.WARNING, "Connexion requise", "Veuillez vous connecter.");
            return;
        }
        if (current.getId() != task.getAssigned_user_id()) {
            showAlert(Alert.AlertType.WARNING, "Depot", "Seul l'assigne peut deposer le travail.");
            return;
        }

        String deliverable = safeTrim(deliverableArea != null ? deliverableArea.getText() : null);
        String attachment = safeTrim(attachmentField != null ? attachmentField.getText() : null);
        if (deliverable.isEmpty() && attachment.isEmpty()) {
            setStatus("Ajoutez une description ou un fichier.");
            return;
        }

        // If a file was chosen, copy it into a shared uploads folder so the creator can download it too.
        // (This assumes the app is used on the same machine or a shared filesystem.)
        String storedAttachment = null;
        if (!attachment.isEmpty()) {
            File f = new File(attachment);
            if (!f.exists() || !f.isFile()) {
                setStatus("Fichier invalide: " + attachment);
                return;
            }
            try {
                Path dest = copyToSharedUploads(f.toPath(), task.getId());
                storedAttachment = dest.toString();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Depot", "Impossible de copier le fichier: " + e.getMessage());
                return;
            }
        }

        ProjectTask updated = new ProjectTask();
        updated.setId(task.getId());
        updated.setTitle(task.getTitle());
        updated.setDescription(task.getDescription());
        updated.setStatus("DONE");
        updated.setDeadline(task.getDeadline());
        updated.setCompleted_at(new Timestamp(System.currentTimeMillis()));
        updated.setDeliverable(deliverable.isEmpty() ? null : deliverable);
        updated.setGrade(task.getGrade());
        updated.setAttachment(storedAttachment);
        updated.setResource_path(task.getResource_path());
        updated.setProject_id(task.getProject_id());
        updated.setAssigned_user_id(task.getAssigned_user_id());

        try {
            projectTaskService.modifier(updated);
            if (onDone != null) {
                onDone.run();
            }
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Depot", "Enregistrement impossible: " + e.getMessage());
        }
    }

    private static Path copyToSharedUploads(Path src, int taskId) throws Exception {
        String publicDir = System.getenv("PUBLIC");
        Path base = (publicDir != null && !publicDir.trim().isEmpty())
                ? Paths.get(publicDir, "studly_uploads")
                : Paths.get(System.getProperty("user.home"), "studly_uploads");

        String name = src.getFileName() == null ? "file" : src.getFileName().toString();
        // Prevent weird paths; keep filename only.
        name = name.replaceAll("[\\\\/\\:\\*\\?\\\"\\<\\>\\|]", "_");

        Path dir = base.resolve("project_task").resolve(String.valueOf(taskId));
        Files.createDirectories(dir);

        Path dest = dir.resolve(System.currentTimeMillis() + "_" + name);
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    @FXML
    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        } else if (onDone != null) {
            onDone.run();
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg == null ? "" : msg);
        }
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
}
