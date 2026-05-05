package controllers.group;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import models.Group;
import models.Project;
import models.User;
import services.GroupService;
import services.ProjectService;
import services.ai.DescriptionAiService;
import utils.SessionManager;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;

public class AddProjectController {
    @FXML
    private Label groupHintLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TextField typeField;

    @FXML
    private DatePicker deadlinePicker;

    @FXML
    private TextField resourceField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button saveButton;

    @FXML
    private Button generateDescriptionButton;

    private final ProjectService projectService = new ProjectService();
    private final GroupService groupService = new GroupService();
    private final DescriptionAiService descriptionAiService = new DescriptionAiService();
    private Group group;
    private Project projectToEdit;
    private Runnable onSaved;
    private Runnable onDone;
    private Runnable onCancel;

    public void setGroup(Group group) {
        this.group = group;
        renderGroupHint();
    }

    public void setProject(Project project) {
        this.projectToEdit = project;
        renderEditState();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
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
            statusCombo.getItems().setAll("ACTIVE", "PLANNED", "DONE", "CANCELLED");
            statusCombo.getSelectionModel().select("ACTIVE");
        }
        renderGroupHint();
        setStatus("");
        renderEditState();
    }

    @FXML
    private void generateDescription() {
        if (descriptionArea == null) {
            return;
        }
        String title = safeTrim(titleField != null ? titleField.getText() : null);
        String type = safeTrim(typeField != null ? typeField.getText() : null);
        String status = selectedStatus();
        LocalDate deadline = deadlinePicker != null ? deadlinePicker.getValue() : null;
        String resource = safeTrim(resourceField != null ? resourceField.getText() : null);
        String groupName = group == null ? "" : safeTrim(group.getCategory());

        setStatus("");
        if (generateDescriptionButton != null) {
            generateDescriptionButton.setDisable(true);
        }
        descriptionArea.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return descriptionAiService.generateProjectDescription(title, type, status, deadline, resource, groupName);
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
        Thread th = new Thread(task, "ai-generate-project-description");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void save() {
        setStatus("");

        if (group == null) {
            showAlert(Alert.AlertType.ERROR, "Projet", "Aucun groupe selectionne.");
            return;
        }
        User current = SessionManager.getCurrentUser();
        if (!groupService.isGroupCreator(group, current)) {
            showAlert(Alert.AlertType.WARNING, "Projet", "Acces refuse: seul le createur du groupe peut ajouter/modifier un projet.");
            return;
        }

        String title = safeTrim(titleField.getText());
        if (title.isEmpty()) {
            setStatus("Titre requis.");
            return;
        }

        Project p = new Project();
        if (projectToEdit != null) {
            p.setId(projectToEdit.getId());
        }
        p.setTitle(title);
        p.setDescription(safeTrim(descriptionArea.getText()));
        p.setStatus(selectedStatus());
        p.setType(safeTrim(typeField.getText()));
        p.setResource(safeTrim(resourceField.getText()));
        p.setDeadline(toSqlDate(deadlinePicker != null ? deadlinePicker.getValue() : null));
        p.setGroup_id(group.getId());

        try {
            if (projectToEdit != null) {
                projectService.modifier(p);
            } else {
                projectService.ajouter(p);
            }
            if (onSaved != null) {
                onSaved.run();
            }
            if (onDone != null) {
                onDone.run();
            }
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Projet", "Ajout impossible: " + e.getMessage());
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

    @FXML
    private void browseResource() {
        if (resourceField == null) {
            return;
        }
        Window owner = null;
        if (saveButton != null && saveButton.getScene() != null) {
            owner = saveButton.getScene().getWindow();
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une ressource");
        java.io.File file = chooser.showOpenDialog(owner);
        if (file != null) {
            resourceField.setText(file.getAbsolutePath());
        }
    }

    private void renderGroupHint() {
        if (groupHintLabel == null) {
            return;
        }
        if (group == null) {
            groupHintLabel.setText("");
            return;
        }
        String name = safeTrim(group.getCategory());
        groupHintLabel.setText(name.isEmpty() ? "Groupe #" + group.getId() : ("Groupe: " + name));
    }

    private void renderEditState() {
        if (projectToEdit == null) {
            if (saveButton != null) {
                saveButton.setText("Creer le Projet");
            }
            return;
        }

        if (saveButton != null) {
            saveButton.setText("Mettre a jour");
        }
        if (titleField != null) {
            titleField.setText(projectToEdit.getTitle());
        }
        if (descriptionArea != null) {
            descriptionArea.setText(projectToEdit.getDescription());
        }
        if (typeField != null) {
            typeField.setText(projectToEdit.getType());
        }
        if (resourceField != null) {
            resourceField.setText(projectToEdit.getResource());
        }
        if (deadlinePicker != null && projectToEdit.getDeadline() != null) {
            try {
                deadlinePicker.setValue(projectToEdit.getDeadline().toLocalDate());
            } catch (RuntimeException ignored) {
            }
        }
        if (statusCombo != null) {
            String s = projectToEdit.getStatus();
            if (s != null && !s.trim().isEmpty()) {
                statusCombo.getSelectionModel().select(s.trim().toUpperCase(Locale.ROOT));
            }
        }
    }

    private String selectedStatus() {
        if (statusCombo == null) {
            return "";
        }
        String v = statusCombo.getSelectionModel().getSelectedItem();
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }

    private void setStatus(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg == null ? "" : msg);
        }
    }

    private static Date toSqlDate(LocalDate v) {
        return v == null ? null : Date.valueOf(v);
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
