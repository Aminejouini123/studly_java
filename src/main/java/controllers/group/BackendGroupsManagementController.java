package controllers.group;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Group;
import models.User;
import services.GroupService;
import services.UserService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BackendGroupsManagementController {
    @FXML private TextField searchField;
    @FXML private Button exportCsvBtn;
    @FXML private Button exportPdfBtn;
    @FXML private Button addGroupBtn;
    @FXML private Button sortCreatedAtBtn;

    @FXML private TableView<Group> groupsTable;
    @FXML private TableColumn<Group, String> colId;
    @FXML private TableColumn<Group, Group> colCategory;
    @FXML private TableColumn<Group, String> colCapacity;
    @FXML private TableColumn<Group, Group> colCreatedBy;
    @FXML private TableColumn<Group, Timestamp> colCreatedAt;
    @FXML private TableColumn<Group, String> colStatus;
    @FXML private TableColumn<Group, Group> colActions;

    @FXML private VBox emptyState;

    private final GroupService groupService = new GroupService();
    private final UserService userService = new UserService();

    private final ObservableList<Group> master = FXCollections.observableArrayList();
    private FilteredList<Group> filtered;
    private SortedList<Group> sorted;

    private final Map<Integer, User> usersById = new HashMap<>();
    private boolean usersLoaded;
    private boolean createdAtSortDesc = true;

    @FXML
    public void initialize() {
        filtered = new FilteredList<>(master, g -> true);
        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(groupsTable.comparatorProperty());
        groupsTable.setItems(sorted);

        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colCapacity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacity() + " members"));
        colCreatedAt.setCellValueFactory(c -> c.getValue().createdAtProperty());
        colCreatedAt.setComparator(Comparator.nullsLast(Timestamp::compareTo));
        colCreatedAt.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Timestamp ts, boolean empty) {
                super.updateItem(ts, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(ts == null ? "-" : formatTs(ts));
                setGraphic(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatorId() > 0 ? "Assigned" : "Unassigned"));

        colCategory.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colCategory.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setGraphic(null);
                    return;
                }

                HBox row = new HBox(10);
                row.getStyleClass().add("gm-cat-cell");

                VBox iconBox = new VBox();
                iconBox.getStyleClass().add("gm-cat-icon");
                SVGPath icon = new SVGPath();
                icon.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
                icon.setScaleX(0.7);
                icon.setScaleY(0.7);
                icon.getStyleClass().add("gm-cat-icon-shape");
                iconBox.getChildren().add(icon);

                VBox text = new VBox(2);
                Label title = new Label(nullToDash(g.getCategory()));
                title.getStyleClass().add("gm-cell-title");
                Label meta = new Label("Capacity: " + g.getCapacity() + " members");
                meta.getStyleClass().add("gm-cell-sub");
                text.getChildren().addAll(title, meta);

                row.getChildren().addAll(iconBox, text);
                setGraphic(row);
            }
        });

        colCreatedBy.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colCreatedBy.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setGraphic(null);
                    return;
                }

                User u = resolveUser(g.getCreatorId());
                VBox box = new VBox(2);
                Label name = new Label(u == null ? ("user #" + g.getCreatorId()) : displayName(u));
                name.getStyleClass().add("gm-cell-title");
                Label email = new Label(u == null ? "" : safe(u.getEmail()).toLowerCase(Locale.ROOT));
                email.getStyleClass().add("gm-cell-sub");
                box.getChildren().addAll(name, email);
                setGraphic(box);
            }
        });

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pill = new Label(status);
                pill.getStyleClass().add("gm-status-pill");
                pill.getStyleClass().add("Assigned".equalsIgnoreCase(status) ? "gm-status-assigned" : "gm-status-unassigned");
                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setGraphic(null);
                    return;
                }

                Button view = plainIconButton(
                        "M12 6.5c-3.79 0-7.17 2.13-8.82 5.5 1.65 3.37 5.03 5.5 8.82 5.5s7.17-2.13 8.82-5.5c-1.65-3.37-5.03-5.5-8.82-5.5zm0 9a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z",
                        "#64748B",
                        "btn-view",
                        "Voir"
                );
                Button edit = plainIconButton(
                        "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z",
                        "#3b82f6",
                        "btn-edit",
                        "Modifier"
                );
                Button del = plainIconButton(
                        "M 6 19 c 0 1.1 0.9 2 2 2 h 8 c 1.1 0 2 -0.9 2 -2 V 7 H 6 v 12 Z M 19 4 h -3.5 l -1 -1 h -5 l -1 1 H 5 v 2 h 14 V 4 Z",
                        "#ef4444",
                        "btn-delete",
                        "Supprimer"
                );

                view.setOnAction(e -> openDetails(g));
                edit.setOnAction(e -> openEdit(g));
                del.setOnAction(e -> deleteGroup(g));

                HBox box = new HBox(2, view, edit, del);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        // Make sure Actions column always renders buttons and isn't accidentally sortable.
        colActions.setSortable(false);

        // Default sort by creation date (newest first).
        applyCreatedAtSort(true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter(newV));
        }

        refresh();
    }

    @FXML
    private void toggleSortCreatedAt() {
        applyCreatedAtSort(!createdAtSortDesc);
    }

    private void applyCreatedAtSort(boolean desc) {
        createdAtSortDesc = desc;
        if (colCreatedAt != null && groupsTable != null) {
            colCreatedAt.setSortType(desc ? TableColumn.SortType.DESCENDING : TableColumn.SortType.ASCENDING);
            groupsTable.getSortOrder().clear();
            groupsTable.getSortOrder().add(colCreatedAt);
            groupsTable.sort();
        }
        if (sortCreatedAtBtn != null) {
            sortCreatedAtBtn.setTooltip(new Tooltip(desc ? "Trier: plus recent d'abord" : "Trier: plus ancien d'abord"));
        }
    }

    @FXML
    private void exportCsv() {
        // Excel can open CSV directly; we export current filtered rows.
        if (groupsTable == null || groupsTable.getScene() == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Excel (CSV)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        fc.setInitialFileName("groups_export.csv");
        java.io.File file = fc.showSaveDialog(groupsTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        List<Group> rows = groupsTable.getItems() == null ? List.of() : groupsTable.getItems().stream().collect(Collectors.toList());
        String csv = buildCsv(rows);
        try {
            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
            showAlert(Alert.AlertType.INFORMATION, "Export", "Export Excel termine.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export", "Ecriture impossible: " + e.getMessage());
        }
    }

    @FXML
    private void exportPdf() {
        if (groupsTable == null || groupsTable.getScene() == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName("groups_export.pdf");
        java.io.File file = fc.showSaveDialog(groupsTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        List<Group> rows = groupsTable.getItems() == null ? List.of() : groupsTable.getItems().stream().collect(Collectors.toList());
        try {
            writeSimplePdf(file.toPath(), rows);
            showAlert(Alert.AlertType.INFORMATION, "Export", "Export PDF termine.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export", "Ecriture impossible: " + e.getMessage());
        }
    }

    @FXML
    private void addGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_group/add_group.fxml"));
            Parent root = loader.load();
            AddGroupController controller = loader.getController();
            controller.setOnSaved(this::refresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Group");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void openEdit(Group g) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_group/edit_group.fxml"));
            Parent root = loader.load();
            EditGroupController controller = loader.getController();
            controller.setGroup(g);
            controller.setOnSaved(this::refresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Group");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'edition: " + e.getMessage());
        }
    }

    private void openDetails(Group g) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_group/group_details.fxml"));
            Parent root = loader.load();
            GroupDetailsController controller = loader.getController();
            controller.setGroup(g);

            Stage stage = new Stage();
            controller.setOnBack(stage::close);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Group Details");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les details: " + e.getMessage());
        }
    }

    private void deleteGroup(Group g) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce groupe ?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        try {
            groupService.supprimer(g.getId());
            refresh();
        } catch (SQLException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible: " + e.getMessage());
        }
    }

    private void refresh() {
        master.clear();
        usersLoaded = false;
        usersById.clear();

        try {
            List<Group> groups = groupService.recuperer();
            master.addAll(groups);
        } catch (SQLException | RuntimeException e) {
            // Leave empty; empty state will show.
        }

        updateEmptyState();
    }

    private void applyFilter(String qRaw) {
        String q = qRaw == null ? "" : qRaw.trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(g -> {
            if (q.isEmpty()) return true;
            String cat = g.getCategory() == null ? "" : g.getCategory().toLowerCase(Locale.ROOT);
            return cat.contains(q);
        });
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = filtered == null || filtered.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(empty);
            emptyState.setManaged(empty);
        }
    }

    private User resolveUser(int id) {
        if (id <= 0) return null;
        if (!usersLoaded) {
            try {
                List<User> all = userService.recuperer();
                for (User u : all) {
                    usersById.put(u.getId(), u);
                }
            } catch (SQLException | RuntimeException ignored) {
            } finally {
                usersLoaded = true;
            }
        }
        return usersById.get(id);
    }

    private static Button iconButton(String svgPath, String styleClass) {
        return iconButton(svgPath, styleClass, null);
    }

    private static Button iconButton(String svgPath, String styleClass, String tooltip) {
        Button b = new Button();
        b.getStyleClass().add(styleClass);
        b.setMinWidth(36);
        b.setPrefWidth(36);
        b.setMaxWidth(36);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("gm-icon");
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);
        b.setGraphic(icon);
        if (tooltip != null && !tooltip.isBlank()) {
            b.setTooltip(new Tooltip(tooltip));
        }
        return b;
    }

    // Match user backend action buttons (transparent button + colored icon + hover tint).
    private static Button plainIconButton(String svgPath, String fill, String styleClass, String tooltip) {
        Button b = new Button();
        b.getStyleClass().add(styleClass);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setFill(javafx.scene.paint.Color.web(fill));
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);
        b.setGraphic(icon);

        if (tooltip != null && !tooltip.isBlank()) {
            b.setTooltip(new Tooltip(tooltip));
        }
        return b;
    }

    private static String nullToDash(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String displayName(User u) {
        String first = safe(u.getFirst_name());
        String last = safe(u.getLast_name());
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        String email = safe(u.getEmail());
        return email.isEmpty() ? "Utilisateur" : email.toLowerCase(Locale.ROOT);
    }

    private static String formatTs(Timestamp ts) {
        LocalDateTime dt;
        if (ts == null) {
            dt = LocalDateTime.now();
        } else {
            dt = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String buildCsv(List<Group> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,category,capacity,created_by,created_at,status\n");
        for (Group g : rows) {
            String id = String.valueOf(g.getId());
            String category = safe(g.getCategory());
            String capacity = String.valueOf(g.getCapacity());
            User u = resolveUser(g.getCreatorId());
            String createdBy = u == null ? ("user #" + g.getCreatorId()) : displayName(u);
            String createdAt = formatTs(g.getCreatedAt());
            String status = g.getCreatorId() > 0 ? "Assigned" : "Unassigned";

            sb.append(csv(id)).append(',')
              .append(csv(category)).append(',')
              .append(csv(capacity)).append(',')
              .append(csv(createdBy)).append(',')
              .append(csv(createdAt)).append(',')
              .append(csv(status)).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        String v = value == null ? "" : value;
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!needsQuotes) return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /**
     * Minimal, dependency-free, single-page PDF export.
     * Uses built-in Helvetica with WinAnsi encoding; keeps content mostly ASCII/Latin-1.
     */
    private void writeSimplePdf(Path out, List<Group> rows) throws IOException {
        String title = "Groups Management Export";
        List<String> lines = new java.util.ArrayList<>();
        lines.add(title);
        lines.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lines.add("");
        lines.add("ID | CATEGORY | CAPACITY | CREATED BY | CREATED AT | STATUS");
        lines.add("-----------------------------------------------------------");
        for (Group g : rows) {
            User u = resolveUser(g.getCreatorId());
            String createdBy = u == null ? ("user #" + g.getCreatorId()) : displayName(u);
            String status = g.getCreatorId() > 0 ? "Assigned" : "Unassigned";
            String line = "#" + g.getId()
                    + " | " + nullToDash(g.getCategory())
                    + " | " + g.getCapacity()
                    + " | " + createdBy
                    + " | " + formatTs(g.getCreatedAt())
                    + " | " + status;
            lines.add(line);
        }

        byte[] pdf = SimplePdf.onePage(lines);
        Files.write(out, pdf);
    }

    private static final class SimplePdf {
        private static byte[] onePage(List<String> lines) throws IOException {
            // Build a single-page PDF with text lines.
            StringBuilder content = new StringBuilder();
            content.append("BT\n");
            content.append("/F1 11 Tf\n");
            content.append("14 TL\n"); // line height
            content.append("50 800 Td\n"); // Start near top-left.

            for (int i = 0; i < lines.size(); i++) {
                String line = escapePdfText(lines.get(i));
                content.append("(").append(line).append(") Tj\n");
                if (i != lines.size() - 1) {
                    content.append("T*\n"); // Move to next line.
                }
            }
            content.append("ET\n");

            byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            // Objects
            // 1: Catalog
            // 2: Pages
            // 3: Page
            // 4: Font
            // 5: Content stream
            ByteArrayBuilder b = new ByteArrayBuilder();
            b.append("%PDF-1.4\n");

            int o1 = b.markObject(1);
            b.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            int o2 = b.markObject(2);
            b.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            int o3 = b.markObject(3);
            // A4 portrait: 595x842 points
            b.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] ")
             .append("/Resources << /Font << /F1 4 0 R >> >> ")
             .append("/Contents 5 0 R >>\nendobj\n");

            int o4 = b.markObject(4);
            b.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            int o5 = b.markObject(5);
            b.append("5 0 obj\n<< /Length ").append(String.valueOf(contentBytes.length)).append(" >>\nstream\n");
            b.appendBytes(contentBytes);
            b.append("\nendstream\nendobj\n");

            int xrefStart = b.length();
            b.append("xref\n0 6\n");
            b.append("0000000000 65535 f \n");
            b.append(xrefLine(o1)).append("\n");
            b.append(xrefLine(o2)).append("\n");
            b.append(xrefLine(o3)).append("\n");
            b.append(xrefLine(o4)).append("\n");
            b.append(xrefLine(o5)).append("\n");
            b.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(String.valueOf(xrefStart)).append("\n%%EOF\n");

            return b.toByteArray();
        }

        private static String xrefLine(int offset) {
            return String.format(Locale.ROOT, "%010d 00000 n ", offset);
        }

        private static String escapePdfText(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }

    private static final class ByteArrayBuilder {
        private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(8192);

        int length() {
            return out.size();
        }

        int markObject(int objectNumber) {
            // Return byte offset of the object header line.
            return out.size();
        }

        ByteArrayBuilder append(String s) throws IOException {
            out.write(s.getBytes(StandardCharsets.ISO_8859_1));
            return this;
        }

        ByteArrayBuilder appendBytes(byte[] bytes) throws IOException {
            out.write(bytes);
            return this;
        }

        byte[] toByteArray() {
            return out.toByteArray();
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
