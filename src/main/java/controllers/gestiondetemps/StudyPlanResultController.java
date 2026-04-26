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
        fileChooser.setInitialFileName("plan-etude-" + System.currentTimeMillis() + ".pdf");
        File file = fileChooser.showSaveDialog(rootContainer.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            generateProfessionalPdf(file);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF");
            alert.setHeaderText(null);
            alert.setContentText("Plan d'etude exporte avec succes :\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export PDF");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de generer le PDF.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private void generateProfessionalPdf(File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPosition = pageHeight - margin;

            // EN-TETE AVEC FOND DEGRADE (simule avec rectangle bleu)
            content.setNonStrokingColor(33, 150, 243); // Bleu
            content.addRect(0, pageHeight - 120, pageWidth, 120);
            content.fill();

            // TITRE PRINCIPAL
            content.beginText();
            content.setNonStrokingColor(255, 255, 255); // Blanc
            content.setFont(PDType1Font.HELVETICA_BOLD, 24);
            String title = currentPlan.optString("titre", "Plan d'Etude");
            float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 24;
            content.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 60);
            content.showText(safePdfText(title));
            content.endText();

            // SOUS-TITRE
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            String subtitle = "Style : " + displayValue(currentLearningStyle, "non determine") + 
                            " | Niveau : " + displayValue(currentLevel, "non estime");
            float subtitleWidth = PDType1Font.HELVETICA.getStringWidth(subtitle) / 1000 * 12;
            content.newLineAtOffset((pageWidth - subtitleWidth) / 2, pageHeight - 85);
            content.showText(safePdfText(subtitle));
            content.endText();

            // MESSAGE MOTIVATIONNEL
            yPosition = pageHeight - 140;
            content.setNonStrokingColor(255, 235, 59); // Jaune clair
            content.addRect(margin, yPosition - 25, pageWidth - 2 * margin, 30);
            content.fill();
            
            content.beginText();
            content.setNonStrokingColor(0, 0, 0);
            content.setFont(PDType1Font.HELVETICA_OBLIQUE, 11);
            String message = currentPlan.optString("message", "Apprends sans crainte");
            content.newLineAtOffset(margin + 10, yPosition - 15);
            content.showText(safePdfText(message));
            content.endText();

            // TABLEAU DES ETAPES
            yPosition -= 60;
            JSONArray steps = currentPlan.optJSONArray("etapes");
            if (steps != null) {
                // En-tete du tableau
                content.setNonStrokingColor(200, 200, 200);
                content.addRect(margin, yPosition - 20, pageWidth - 2 * margin, 25);
                content.fill();

                content.beginText();
                content.setNonStrokingColor(0, 0, 0);
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(margin + 5, yPosition - 12);
                content.showText("Etape");
                content.newLineAtOffset(50, 0);
                content.showText("Activite");
                content.newLineAtOffset(350, 0);
                content.showText("Duree");
                content.endText();

                yPosition -= 25;

                // Lignes du tableau
                String[] colors = {"#2196F3", "#FF9800", "#4CAF50", "#F44336"};
                
                for (int index = 0; index < steps.length(); index++) {
                    JSONObject step = steps.getJSONObject(index);
                    
                    // Verifier si on a besoin d'une nouvelle page
                    if (yPosition < 150) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        yPosition = pageHeight - margin;
                    }

                    // Fond alterne
                    float rowHeight = 60; // Hauteur augmentee pour le texte multi-lignes
                    if (index % 2 == 0) {
                        content.setNonStrokingColor(245, 245, 245);
                        content.addRect(margin, yPosition - rowHeight, pageWidth - 2 * margin, rowHeight);
                        content.fill();
                    }

                    // Numero de l'etape avec cercle colore
                    int[] rgb = hexToRgb(colors[index % colors.length]);
                    content.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
                    content.addRect(margin + 5, yPosition - 20, 25, 25);
                    content.fill();

                    content.beginText();
                    content.setNonStrokingColor(255, 255, 255);
                    content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    content.newLineAtOffset(margin + 12, yPosition - 14);
                    content.showText(String.valueOf(step.optInt("numero", index + 1)));
                    content.endText();

                    // Activite avec gestion multi-lignes
                    String action = step.optString("action", "");
                    content.beginText();
                    content.setNonStrokingColor(0, 0, 0);
                    content.setFont(PDType1Font.HELVETICA, 9);
                    
                    float textX = margin + 60;
                    float textY = yPosition - 12;
                    float maxWidth = 320;
                    
                    // Decouper le texte en lignes
                    String[] words = action.split(" ");
                    StringBuilder currentLine = new StringBuilder();
                    int lineCount = 0;
                    
                    for (String word : words) {
                        String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                        float testWidth = PDType1Font.HELVETICA.getStringWidth(testLine) / 1000 * 9;
                        
                        if (testWidth > maxWidth && currentLine.length() > 0) {
                            content.newLineAtOffset(textX, textY - (lineCount * 12));
                            content.showText(safePdfText(currentLine.toString()));
                            content.newLineAtOffset(-textX, -(textY - (lineCount * 12)));
                            currentLine = new StringBuilder(word);
                            lineCount++;
                            if (lineCount >= 3) break; // Max 3 lignes
                        } else {
                            currentLine = new StringBuilder(testLine);
                        }
                    }
                    
                    // Derniere ligne
                    if (currentLine.length() > 0) {
                        content.newLineAtOffset(textX, textY - (lineCount * 12));
                        content.showText(safePdfText(currentLine.toString()));
                    }
                    content.endText();

                    // Duree
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    content.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
                    content.newLineAtOffset(margin + 430, yPosition - 12);
                    content.showText(step.optInt("duree_minutes", 0) + " min");
                    content.endText();

                    yPosition -= rowHeight + 5;
                }

                // TABLEAU RECAPITULATIF
                if (yPosition < 150) {
                    content.close();
                    page = new PDPage();
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    yPosition = pageHeight - margin;
                }

                yPosition -= 20;
                content.setNonStrokingColor(100, 181, 246);
                content.addRect(margin, yPosition - 40, pageWidth - 2 * margin, 45);
                content.fill();

                content.beginText();
                content.setNonStrokingColor(255, 255, 255);
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.newLineAtOffset(margin + 20, yPosition - 22);
                content.showText("Total des etapes : " + steps.length());
                
                int totalDuration = 0;
                for (int i = 0; i < steps.length(); i++) {
                    totalDuration += steps.getJSONObject(i).optInt("duree_minutes", 0);
                }
                content.newLineAtOffset(200, 0);
                content.showText("Duree totale : " + totalDuration + " min");
                content.endText();

                // MESSAGE FINAL
                yPosition -= 70;
                content.beginText();
                content.setNonStrokingColor(76, 175, 80);
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                String finalMessage = "Tu as tout ce qu'il faut pour reussir. Commence maintenant !";
                float finalWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(finalMessage) / 1000 * 16;
                content.newLineAtOffset((pageWidth - finalWidth) / 2, yPosition);
                content.showText(safePdfText(finalMessage));
                content.endText();
            }

            content.close();
            document.save(file);
        }
    }

    private int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new int[]{
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        };
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
