package controllers.group;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Group;
import models.User;
import services.GroupService;
import services.UserService;
import utils.SessionManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    @FXML private FlowPane groupsFlow;

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
        sorted.addListener((ListChangeListener<Group>) change -> renderCards());
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
        sorted.setComparator((a, b) -> {
            Timestamp ta = a == null ? null : a.getCreatedAt();
            Timestamp tb = b == null ? null : b.getCreatedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            int cmp = ta.compareTo(tb);
            return desc ? -cmp : cmp;
        });
        if (sortCreatedAtBtn != null) {
            sortCreatedAtBtn.setTooltip(new Tooltip(desc ? "Trier: plus recent d'abord" : "Trier: plus ancien d'abord"));
        }
        renderCards();
    }

    @FXML
    private void exportCsv() {
        // Excel can open CSV directly; we export current filtered rows.
        if (groupsFlow == null || groupsFlow.getScene() == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Excel (CSV)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        fc.setInitialFileName("groups_export.csv");
        java.io.File file = fc.showSaveDialog(groupsFlow.getScene().getWindow());
        if (file == null) {
            return;
        }

        List<Group> rows = sorted == null ? List.of() : sorted.stream().collect(Collectors.toList());
        String csv = buildCsv(rows);
        try {
            // Add UTF-8 BOM to help Excel detect encoding correctly.
            Files.writeString(file.toPath(), "\uFEFF" + csv, StandardCharsets.UTF_8);
            showAlert(Alert.AlertType.INFORMATION, "Export", "Export Excel termine.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export", "Ecriture impossible: " + e.getMessage());
        }
    }

    @FXML
    private void exportPdf() {
        if (groupsFlow == null || groupsFlow.getScene() == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName("groups_export.pdf");
        java.io.File file = fc.showSaveDialog(groupsFlow.getScene().getWindow());
        if (file == null) {
            return;
        }

        List<Group> rows = sorted == null ? List.of() : sorted.stream().collect(Collectors.toList());
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
        if (!canCurrentUserModify(g)) {
            showAlert(Alert.AlertType.ERROR, "Autorisation", "Seul le createur (ou un admin) peut modifier ce groupe.");
            return;
        }

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
        if (!canCurrentUserModify(g)) {
            showAlert(Alert.AlertType.ERROR, "Autorisation", "Seul le createur (ou un admin) peut supprimer ce groupe.");
            return;
        }

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

    private boolean canCurrentUserModify(Group g) {
        User current = SessionManager.getCurrentUser();
        return groupService.isGroupCreator(g, current) || isAdmin(current);
    }

    private boolean canCurrentUserView(Group g) {
        User current = SessionManager.getCurrentUser();
        if (current == null || g == null) {
            return false;
        }
        return isAdmin(current) || groupService.isGroupCreator(g, current);
    }

    private void updateEmptyState() {
        boolean empty = filtered == null || filtered.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(empty);
            emptyState.setManaged(empty);
        }
        if (groupsFlow != null) {
            groupsFlow.setVisible(!empty);
            groupsFlow.setManaged(!empty);
        }
    }

    private void renderCards() {
        if (groupsFlow == null) {
            return;
        }
        groupsFlow.getChildren().clear();
        if (sorted == null) {
            return;
        }
        for (Group g : sorted) {
            if (g != null) {
                groupsFlow.getChildren().add(buildGroupCard(g));
            }
        }
    }

    private VBox buildGroupCard(Group g) {
        VBox card = new VBox(10);
        card.getStyleClass().add("gm-group-card");
        card.setPrefWidth(320);
        card.setMinWidth(300);
        card.setMaxWidth(360);

        Label idLabel = new Label("ID: #" + g.getId());
        idLabel.getStyleClass().add("gm-kv-title");

        Label category = new Label(nullToDash(g.getCategory()));
        category.getStyleClass().add("gm-group-title");

        Label capacity = new Label("Capacite: " + g.getCapacity() + " membres");
        capacity.getStyleClass().add("gm-kv-sub");

        Label createdBy = new Label("Cree par: " + resolveCreatorDisplay(g.getCreatorId()));
        createdBy.getStyleClass().add("gm-kv-sub");

        Label createdAt = new Label("Date: " + (g.getCreatedAt() == null ? "-" : formatTs(g.getCreatedAt())));
        createdAt.getStyleClass().add("gm-kv-sub");

        String statusText = g.getCreatorId() > 0 ? "Assigned" : "Unassigned";
        Label status = new Label(statusText);
        status.getStyleClass().addAll("gm-status-pill", "Assigned".equalsIgnoreCase(statusText) ? "gm-status-assigned" : "gm-status-unassigned");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button view = plainIconButton(
                "M12 6.5c-3.79 0-7.17 2.13-8.82 5.5 1.65 3.37 5.03 5.5 8.82 5.5s7.17-2.13 8.82-5.5c-1.65-3.37-5.03-5.5-8.82-5.5zm0 9a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7z",
                "#64748B",
                "btn-edit",
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

        view.setDisable(!canCurrentUserView(g));
        boolean canModify = canCurrentUserModify(g);
        edit.setDisable(!canModify);
        del.setDisable(!canModify);
        if (!canModify) {
            Tooltip tip = new Tooltip("Seul le createur (ou un admin) peut modifier/supprimer.");
            edit.setTooltip(tip);
            del.setTooltip(tip);
        }

        HBox statusAndActions = new HBox(8, status, spacer, view, edit, del);
        statusAndActions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(idLabel, category, capacity, createdBy, createdAt, statusAndActions);
        return card;
    }

    private String resolveCreatorDisplay(int creatorId) {
        if (creatorId <= 0) {
            return "Createur non defini";
        }
        User creator = resolveUser(creatorId);
        if (creator != null) {
            return displayName(creator);
        }
        return "Createur inconnu";
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

    private static boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        String roles = user.getRoles();
        return roles != null && roles.toUpperCase(Locale.ROOT).contains("ROLE_ADMIN");
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
        // Keep it clean (no "sep=;" line). On FR locales, Excel typically opens ';' delimited CSV correctly.
        // Also, we keep CREATED_AT as text to avoid Excel displaying it as "#####" due to narrow columns.
        sb.append("CATEGORY;CAPACITY;CREATED_BY;CREATED_AT;STATUS\n");
        for (Group g : rows) {
            String category = safe(g.getCategory());
            String capacity = String.valueOf(g.getCapacity());
            User u = resolveUser(g.getCreatorId());
            String createdBy = u == null ? ("user #" + g.getCreatorId()) : displayName(u);
            String createdAt = "'" + formatTs(g.getCreatedAt());
            String status = g.getCreatorId() > 0 ? "Assigned" : "Unassigned";

            sb.append(csv(category, ';')).append(';')
              .append(csv(capacity, ';')).append(';')
              .append(csv(createdBy, ';')).append(';')
              .append(csv(createdAt, ';')).append(';')
              .append(csv(status, ';')).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value, char delimiter) {
        String v = value == null ? "" : value;
        boolean needsQuotes = v.indexOf(delimiter) >= 0 || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!needsQuotes) return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /**
     * Minimal, dependency-free, single-page PDF export.
     * Uses built-in Helvetica with WinAnsi encoding; keeps content mostly ASCII/Latin-1.
     */
    private void writeSimplePdf(Path out, List<Group> rows) throws IOException {
        List<PdfRow> tableRows = new java.util.ArrayList<>();
        for (Group g : rows) {
            User u = resolveUser(g.getCreatorId());
            String createdBy = u == null ? ("user #" + g.getCreatorId()) : displayName(u);
            String status = g.getCreatorId() > 0 ? "Assigned" : "Unassigned";
            tableRows.add(new PdfRow(
                    nullToDash(g.getCategory()),
                    String.valueOf(g.getCapacity()),
                    nullToDash(createdBy),
                    nullToDash(formatTs(g.getCreatedAt())),
                    status
            ));
        }

        byte[] pdf = SimplePdfTable.build(
                "Groups Management Export",
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                tableRows
        );
        Files.write(out, pdf);
    }

    private static final class PdfRow {
        private final String category;
        private final String capacity;
        private final String createdBy;
        private final String createdAt;
        private final String status;

        private PdfRow(String category, String capacity, String createdBy, String createdAt, String status) {
            this.category = category;
            this.capacity = capacity;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.status = status;
        }

        String category() { return category; }
        String capacity() { return capacity; }
        String createdBy() { return createdBy; }
        String createdAt() { return createdAt; }
        String status() { return status; }
    }

    /**
     * Minimal, dependency-free PDF "table" export (multi-page).
     * Still uses built-in base14 fonts (Helvetica / Helvetica-Bold) and ISO-8859-1 text.
     */
    private static final class SimplePdfTable {
        private static final int PAGE_W = 595; // A4 portrait
        private static final int PAGE_H = 842;

        private static final int MARGIN_X = 40;
        private static final int TITLE_Y = 800;
        private static final int META_Y = 780;
        private static final int TABLE_TOP_Y = 740;
        private static final int ROW_H = 18;
        private static final int HEADER_H = 22;
        private static final int BOTTOM_Y = 60;

        // Total available width = 595 - 2*40 = 515
        private static final int W_CATEGORY = 110;
        private static final int W_CAPACITY = 60;
        private static final int W_CREATED_BY = 165;
        private static final int W_CREATED_AT = 110;
        private static final int W_STATUS = 70;

        private static final String[] HEADERS = {"CATEGORY", "CAPACITY", "CREATED BY", "CREATED AT", "STATUS"};

        static byte[] build(String title, String meta, List<PdfRow> rows) throws IOException {
            // Object numbers:
            // 1 Catalog
            // 2 Pages
            // 3 Font regular (F1)
            // 4 Font bold (F2)
            // Then per page: Page obj + Content obj
            int rowsPerPage = Math.max(1, (TABLE_TOP_Y - BOTTOM_Y - HEADER_H) / ROW_H);
            int pageCount = Math.max(1, (int) Math.ceil(rows.size() / (double) rowsPerPage));

            int firstPageObj = 5;
            int objects = 4 + pageCount * 2;

            ByteArrayBuilder b = new ByteArrayBuilder();
            b.append("%PDF-1.4\n");

            int[] offsets = new int[objects + 1]; // 0..objects

            // 1: Catalog
            offsets[1] = b.length();
            b.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // 2: Pages (Kids list references page objects)
            String kids = buildKidsArray(firstPageObj, pageCount);
            offsets[2] = b.length();
            b.append("2 0 obj\n<< /Type /Pages /Kids ").append(kids).append(" /Count ").append(String.valueOf(pageCount)).append(" >>\nendobj\n");

            // 3: Font regular
            offsets[3] = b.length();
            b.append("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            // 4: Font bold
            offsets[4] = b.length();
            b.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

            // Pages + content streams
            for (int p = 0; p < pageCount; p++) {
                int pageObj = firstPageObj + (p * 2);
                int contentObj = pageObj + 1;

                int from = p * rowsPerPage;
                int to = Math.min(rows.size(), from + rowsPerPage);
                List<PdfRow> pageRows = rows.subList(from, to);

                byte[] contentBytes = pageContent(title, meta, p == 0, pageRows).getBytes(StandardCharsets.ISO_8859_1);

                offsets[pageObj] = b.length();
                b.append(pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] ")
                 .append("/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> ")
                 .append("/Contents " + contentObj + " 0 R >>\nendobj\n");

                offsets[contentObj] = b.length();
                b.append(contentObj + " 0 obj\n<< /Length ").append(String.valueOf(contentBytes.length)).append(" >>\nstream\n");
                b.appendBytes(contentBytes);
                b.append("\nendstream\nendobj\n");
            }

            int xrefStart = b.length();
            b.append("xref\n0 ").append(String.valueOf(objects + 1)).append("\n");
            b.append("0000000000 65535 f \n");
            for (int i = 1; i <= objects; i++) {
                b.append(xrefLine(offsets[i])).append("\n");
            }
            b.append("trailer\n<< /Size ").append(String.valueOf(objects + 1)).append(" /Root 1 0 R >>\nstartxref\n")
             .append(String.valueOf(xrefStart)).append("\n%%EOF\n");

            return b.toByteArray();
        }

        private static String buildKidsArray(int firstPageObj, int pageCount) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < pageCount; i++) {
                int pageObj = firstPageObj + (i * 2);
                sb.append(pageObj).append(" 0 R");
                if (i != pageCount - 1) {
                    sb.append(" ");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        private static String pageContent(String title, String meta, boolean includeHeader, List<PdfRow> rows) {
            StringBuilder c = new StringBuilder(8192);

            // Title + meta
            if (includeHeader) {
                c.append("BT\n");
                c.append("/F2 16 Tf\n");
                c.append("1 0 0 1 ").append(MARGIN_X).append(" ").append(TITLE_Y).append(" Tm\n");
                c.append("(").append(escapePdfText(safeLatin1(title))).append(") Tj\n");
                c.append("/F1 11 Tf\n");
                c.append("1 0 0 1 ").append(MARGIN_X).append(" ").append(META_Y).append(" Tm\n");
                c.append("(").append(escapePdfText(safeLatin1(meta))).append(") Tj\n");
                c.append("ET\n");
            }

            int tableLeft = MARGIN_X;
            int tableTop = TABLE_TOP_Y;
            int tableRight = MARGIN_X + W_CATEGORY + W_CAPACITY + W_CREATED_BY + W_CREATED_AT + W_STATUS;

            // Grid lines (header + rows)
            int totalRows = rows.size();
            int tableBottom = tableTop - HEADER_H - (totalRows * ROW_H);

            c.append("0.6 w\n"); // stroke width
            c.append("0 0 0 RG\n"); // stroke color (black)

            // Vertical lines
            int x0 = tableLeft;
            int x1 = x0 + W_CATEGORY;
            int x2 = x1 + W_CAPACITY;
            int x3 = x2 + W_CREATED_BY;
            int x4 = x3 + W_CREATED_AT;
            int x5 = x4 + W_STATUS;

            verticalLine(c, x0, tableTop, tableBottom);
            verticalLine(c, x1, tableTop, tableBottom);
            verticalLine(c, x2, tableTop, tableBottom);
            verticalLine(c, x3, tableTop, tableBottom);
            verticalLine(c, x4, tableTop, tableBottom);
            verticalLine(c, x5, tableTop, tableBottom);

            // Horizontal lines (top, header bottom, then each row)
            horizontalLine(c, tableLeft, tableRight, tableTop);
            horizontalLine(c, tableLeft, tableRight, tableTop - HEADER_H);
            for (int r = 0; r < totalRows; r++) {
                horizontalLine(c, tableLeft, tableRight, tableTop - HEADER_H - ((r + 1) * ROW_H));
            }

            // Header text
            c.append("BT\n");
            c.append("/F2 10 Tf\n");
            textInCell(c, x0, tableTop, W_CATEGORY, HEADER_H, HEADERS[0]);
            textInCell(c, x1, tableTop, W_CAPACITY, HEADER_H, HEADERS[1]);
            textInCell(c, x2, tableTop, W_CREATED_BY, HEADER_H, HEADERS[2]);
            textInCell(c, x3, tableTop, W_CREATED_AT, HEADER_H, HEADERS[3]);
            textInCell(c, x4, tableTop, W_STATUS, HEADER_H, HEADERS[4]);
            c.append("ET\n");

            // Body rows
            c.append("BT\n");
            c.append("/F1 10 Tf\n");
            for (int i = 0; i < totalRows; i++) {
                PdfRow row = rows.get(i);
                int rowTop = tableTop - HEADER_H - (i * ROW_H);

                textInCell(c, x0, rowTop, W_CATEGORY, ROW_H, row.category());
                textInCell(c, x1, rowTop, W_CAPACITY, ROW_H, row.capacity());
                textInCell(c, x2, rowTop, W_CREATED_BY, ROW_H, row.createdBy());
                textInCell(c, x3, rowTop, W_CREATED_AT, ROW_H, row.createdAt());
                textInCell(c, x4, rowTop, W_STATUS, ROW_H, row.status());
            }
            c.append("ET\n");

            return c.toString();
        }

        private static void horizontalLine(StringBuilder c, int xLeft, int xRight, int y) {
            c.append(xLeft).append(" ").append(y).append(" m ").append(xRight).append(" ").append(y).append(" l S\n");
        }

        private static void verticalLine(StringBuilder c, int x, int yTop, int yBottom) {
            c.append(x).append(" ").append(yTop).append(" m ").append(x).append(" ").append(yBottom).append(" l S\n");
        }

        private static void textInCell(StringBuilder c, int cellX, int cellTopY, int cellW, int cellH, String raw) {
            int padX = 4;
            int padY = 13; // baseline offset for 10pt text within ~18-22px row
            String s = safeLatin1(raw == null ? "" : raw.trim());

            int maxChars = Math.max(3, (int) Math.floor((cellW - (padX * 2)) / 6.0)); // rough heuristic
            s = ellipsize(s, maxChars);

            int x = cellX + padX;
            int y = (cellTopY - cellH) + padY;
            // Use absolute positioning per cell (Td is relative and would accumulate).
            c.append("1 0 0 1 ").append(x).append(" ").append(y).append(" Tm\n");
            c.append("(").append(escapePdfText(s)).append(") Tj\n");
        }

        private static String ellipsize(String s, int maxChars) {
            if (s == null) return "";
            if (s.length() <= maxChars) return s;
            if (maxChars <= 1) return s.substring(0, 1);
            if (maxChars <= 3) return s.substring(0, maxChars);
            return s.substring(0, maxChars - 3) + "...";
        }

        private static String xrefLine(int offset) {
            return String.format(Locale.ROOT, "%010d 00000 n ", offset);
        }

        private static String safeLatin1(String s) {
            if (s == null) return "";
            StringBuilder out = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                out.append(ch <= 0xFF ? ch : '?');
            }
            return out.toString();
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
