package controllers.gestiondetemps;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import models.Event;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.net.URL;
public class StudyPlanResultController {

    @FXML private VBox rootContainer;
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label messageLabel;
    @FXML private VBox stepsContainer;
    private JSONObject currentPlan;
    private String currentLearningStyle;
    private String currentLevel;

    public void configure(Event event, JSONObject plan, String learningStyle, String level) {
        this.currentPlan = plan;
        this.currentLearningStyle = learningStyle;
        this.currentLevel = level;
        titleLabel.setText(plan.has("titre") ? plan.optString("titre") : plan.optString("title", "Plan d'etude"));
        metaLabel.setText("Style : " + displayValue(learningStyle, "non determine")
                + " | Niveau : " + displayValue(level, "non estime"));
        messageLabel.setText(plan.has("message") ? plan.optString("message") : plan.optString("message_motivationnel", "Bon courage dans votre session d'etude."));

        stepsContainer.getChildren().clear();
        JSONArray steps = plan.has("etapes") ? plan.optJSONArray("etapes") : plan.optJSONArray("steps");
        if (steps != null) {
            for (int index = 0; index < steps.length(); index++) {
                JSONObject step = steps.getJSONObject(index);
                VBox block = new VBox(6);
                block.getStyleClass().add("result-step-card");

                Label number = new Label("Etape " + step.optInt("numero", index + 1));
                number.getStyleClass().add("result-step-title");

                Label action = new Label(step.optString("action", ""));
                action.getStyleClass().add("result-step-action");
                action.setWrapText(true);

                Label duration = new Label(step.optInt("duree_minutes", 0) + " min");
                duration.getStyleClass().add("result-step-duration");

                block.getChildren().addAll(number, action, duration);
                stepsContainer.getChildren().add(block);
            }
        }
    }

    @FXML
    private void handleBackToEvents() {
        loadScreen("/Gestion de temps/events_list.fxml");
    }

    @FXML
    private void handleCreateAnother() {
        loadScreen("/Gestion de temps/add_event.fxml");
    }

    @FXML
    private void handleDownloadPdf() {
        if (currentPlan == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le plan d'etude en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        fileChooser.setInitialFileName("study-plan.pdf");
        File file = fileChooser.showSaveDialog(rootContainer.getScene().getWindow());
        if (file == null) {
            return;
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, 750);
                content.showText(safePdfText(currentPlan.optString("titre", "Plan d'etude")));

                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(0, -25);
                content.showText(safePdfText("Style : " + displayValue(currentLearningStyle, "non determine")));
                content.newLineAtOffset(0, -18);
                content.showText(safePdfText("Niveau : " + displayValue(currentLevel, "non estime")));
                content.newLineAtOffset(0, -18);
                content.showText(safePdfText(currentPlan.optString("message", "")));

                JSONArray steps = currentPlan.optJSONArray("etapes");
                if (steps != null) {
                    for (int index = 0; index < steps.length(); index++) {
                        JSONObject step = steps.getJSONObject(index);
                        content.newLineAtOffset(0, -24);
                        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        content.showText(safePdfText("Etape " + step.optInt("numero", index + 1)));
                        content.newLineAtOffset(0, -16);
                        content.setFont(PDType1Font.HELVETICA, 11);
                        content.showText(safePdfText(step.optString("action", "")));
                        content.newLineAtOffset(0, -16);
                        content.showText(safePdfText("Duree : " + step.optInt("duree_minutes", 0) + " min"));
                    }
                }
                content.endText();
            }

            document.save(file);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF");
            alert.setHeaderText(null);
            alert.setContentText("Plan exporte avec succes :\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export PDF");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de generer le PDF.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadScreen(String path) {
        try {
            URL resource = getClass().getResource(path);
            if (resource == null) {
                throw new IOException("Missing FXML resource: " + path);
            }
            Parent content = FXMLLoader.load(resource);
            if (rootContainer.getParent() instanceof Pane) {
                ((Pane) rootContainer.getParent()).getChildren().setAll(content);
                return;
            }
            if (rootContainer.getScene() != null) {
                rootContainer.getScene().setRoot(content);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de charger l'ecran.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private String displayValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safePdfText(String text) {
        return text == null ? "" : text.replace("\n", " ").replace("\r", " ");
    }
}
