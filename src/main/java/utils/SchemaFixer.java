package utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Best-effort runtime repairs for legacy / inconsistent schemas.
 *
 * Problem seen in the wild:
 * - The app uses table `users`
 * - But `invitation.receiver_id` / `invitation.sender_id` foreign keys were created against table `user`
 * This causes FK failures when inserting invitations for real app users.
 *
 * Another problem seen in the wild:
 * - The application uses table `group` (singular, backticked in SQL because GROUP is a keyword)
 * - But some databases were created with a `groups` table (plural), and FK(s) may still reference `groups(id)`
 *
 * This fixer:
 * - Renames `groups` -> `group` when `group` doesn't exist yet
 * - Drops and recreates FK(s) on `invitation.group_id`, `message.group_id`, `project.group_id` if they reference `groups`
 * - Drops FK(s) on invitation.sender_id / invitation.receiver_id if they reference `user`
 * - Tries to recreate them against `users` (if possible). If it can't, it leaves them dropped so the UI works.
 *
 * We intentionally keep this minimal and safe: no destructive data migrations here.
 */
public final class SchemaFixer {
    private SchemaFixer() {
    }

    public static void repairDefault() {
        Connection c = MyDatabase.getInstance().getConnection();
        if (c == null) {
            return;
        }
        try {
            repairGroupTableAndForeignKeys(c);
            repairInvitationUserForeignKeys(c);
            repairMessageSenderUserForeignKey(c);
            repairProjectDescriptionColumn(c);
            repairProjectTaskDescriptionColumn(c);
        } catch (SQLException e) {
            System.err.println("SchemaFixer: repair failed: " + e.getMessage());
        }
    }

    /**
     * Run this before schema initialization so we can safely rename `groups` -> `group`
     * (otherwise initialization may create `group` and block the rename).
     */
    public static void repairBeforeInitialization() {
        Connection c = MyDatabase.getInstance().getConnection();
        if (c == null) {
            return;
        }
        try {
            repairGroupTableAndForeignKeys(c);
        } catch (SQLException e) {
            System.err.println("SchemaFixer: pre-init repair failed: " + e.getMessage());
        }
    }

    private static void repairGroupTableAndForeignKeys(Connection c) throws SQLException {
        boolean groupExists = tableExists(c, "group");
        boolean groupsExists = tableExists(c, "groups");

        if (!groupExists && groupsExists) {
            try {
                exec(c, "RENAME TABLE `groups` TO `group`");
                groupExists = true;
                groupsExists = false;
                System.out.println("SchemaFixer: renamed table `groups` -> `group`.");
            } catch (SQLException e) {
                System.err.println("SchemaFixer: could not rename `groups` -> `group`: " + e.getMessage());
            }
        }

        // Fix foreign keys that still reference `groups(id)`.
        if (groupExists) {
            repairGroupForeignKey(c, "invitation", "group_id");
            repairGroupForeignKey(c, "message", "group_id");
            repairGroupForeignKey(c, "project", "group_id");
        }

        // Best-effort cleanup: if both tables exist, drop the empty one.
        if (groupExists && groupsExists) {
            int groupCount = countRows(c, "group");
            int groupsCount = countRows(c, "groups");
            if (groupsCount == 0) {
                try {
                    exec(c, "DROP TABLE `groups`");
                    System.out.println("SchemaFixer: dropped empty legacy table `groups`.");
                } catch (SQLException e) {
                    System.err.println("SchemaFixer: could not drop empty legacy table `groups`: " + e.getMessage());
                }
            } else if (groupCount == 0) {
                System.err.println("SchemaFixer: both `group` and `groups` exist; `groups` has data but `group` is empty. "
                        + "Automatic merge is not attempted. Please migrate data manually.");
            } else {
                System.err.println("SchemaFixer: both `group` and `groups` exist and both contain data. "
                        + "Automatic merge is not attempted. Please migrate data manually.");
            }
        }
    }

    private static void repairGroupForeignKey(Connection c, String table, String fkColumn) throws SQLException {
        if (!tableExists(c, table)) {
            return;
        }

        List<ImportedKey> imported = importedKeys(c, table);
        List<ImportedKey> bad = new ArrayList<>();
        for (ImportedKey k : imported) {
            if (!fkColumn.equalsIgnoreCase(k.fkColumn)) {
                continue;
            }
            if ("groups".equals(lower(k.pkTable))) {
                bad.add(k);
            }
        }

        if (bad.isEmpty()) {
            return;
        }

        for (ImportedKey k : bad) {
            if (k.fkName == null || k.fkName.isBlank()) {
                continue;
            }
            exec(c, "ALTER TABLE `" + table + "` DROP FOREIGN KEY `" + k.fkName + "`");
            System.out.println("SchemaFixer: dropped FK " + k.fkName + " on " + table + "." + fkColumn);
        }

        Set<String> existingNames = new HashSet<>();
        for (ImportedKey k : importedKeys(c, table)) {
            if (k.fkName != null) {
                existingNames.add(k.fkName);
            }
        }

        String base = "fk_" + table + "_group";
        String name = uniqueName(base, existingNames);
        String sql = "ALTER TABLE `" + table + "` "
                + "ADD CONSTRAINT `" + name + "` FOREIGN KEY (`" + fkColumn + "`) REFERENCES `group` (`id`) ON DELETE CASCADE";
        try {
            exec(c, sql);
            System.out.println("SchemaFixer: added FK " + name + " -> `group`(id) on " + table + "." + fkColumn);
        } catch (SQLException e) {
            System.err.println("SchemaFixer: could not recreate FK for " + table + "." + fkColumn + " -> `group`(id): " + e.getMessage());
        }
    }

    private static void repairInvitationUserForeignKeys(Connection c) throws SQLException {
        if (!tableExists(c, "invitation")) {
            return;
        }

        // If `users` doesn't exist, do nothing. The app expects it, but we can't fix anything.
        boolean usersExists = tableExists(c, "users");
        if (!usersExists) {
            return;
        }

        List<ImportedKey> imported = importedKeys(c, "invitation");
        List<ImportedKey> bad = new ArrayList<>();
        for (ImportedKey k : imported) {
            String col = lower(k.fkColumn);
            if (!("receiver_id".equals(col) || "sender_id".equals(col))) {
                continue;
            }
            if ("user".equals(lower(k.pkTable))) {
                bad.add(k);
            }
        }

        if (bad.isEmpty()) {
            return;
        }

        // Drop the problematic FK constraints.
        for (ImportedKey k : bad) {
            if (k.fkName == null || k.fkName.isBlank()) {
                continue; // Can't drop without a name.
            }
            String sql = "ALTER TABLE `invitation` DROP FOREIGN KEY `" + k.fkName + "`";
            exec(c, sql);
            System.out.println("SchemaFixer: dropped FK " + k.fkName + " on invitation." + k.fkColumn);
        }

        // Try to recreate FK constraints against `users`. If this fails, we keep going (constraints stay dropped).
        Set<String> existingNames = new HashSet<>();
        for (ImportedKey k : importedKeys(c, "invitation")) {
            if (k.fkName != null) {
                existingNames.add(k.fkName);
            }
        }

        for (ImportedKey k : bad) {
            String col = lower(k.fkColumn);
            String base = "receiver_id".equals(col) ? "fk_invitation_receiver_users" : "fk_invitation_sender_users";
            String name = uniqueName(base, existingNames);
            String sql = "ALTER TABLE `invitation` "
                    + "ADD CONSTRAINT `" + name + "` FOREIGN KEY (`" + col + "`) REFERENCES `users` (`id`) ON DELETE CASCADE";
            try {
                exec(c, sql);
                existingNames.add(name);
                System.out.println("SchemaFixer: added FK " + name + " -> users(id) on invitation." + col);
            } catch (SQLException e) {
                System.err.println("SchemaFixer: could not recreate FK for invitation." + col + " -> users(id): " + e.getMessage());
            }
        }
    }

    /**
     * Repairs legacy FK on message.sender_id that references `user(id)` instead of `users(id)`.
     * If recreation fails, we keep going and leave the FK dropped so chat remains usable.
     */
    private static void repairMessageSenderUserForeignKey(Connection c) throws SQLException {
        if (!tableExists(c, "message")) {
            return;
        }
        if (!tableExists(c, "users")) {
            return;
        }

        List<ImportedKey> imported = importedKeys(c, "message");
        List<ImportedKey> bad = new ArrayList<>();
        for (ImportedKey k : imported) {
            if (!"sender_id".equalsIgnoreCase(k.fkColumn)) {
                continue;
            }
            if ("user".equals(lower(k.pkTable))) {
                bad.add(k);
            }
        }

        if (bad.isEmpty()) {
            return;
        }

        for (ImportedKey k : bad) {
            if (k.fkName == null || k.fkName.isBlank()) {
                continue;
            }
            String sql = "ALTER TABLE `message` DROP FOREIGN KEY `" + k.fkName + "`";
            exec(c, sql);
            System.out.println("SchemaFixer: dropped FK " + k.fkName + " on message.sender_id");
        }

        Set<String> existingNames = new HashSet<>();
        for (ImportedKey k : importedKeys(c, "message")) {
            if (k.fkName != null) {
                existingNames.add(k.fkName);
            }
        }

        String name = uniqueName("fk_message_sender_users", existingNames);
        String sql = "ALTER TABLE `message` "
                + "ADD CONSTRAINT `" + name + "` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE";
        try {
            exec(c, sql);
            System.out.println("SchemaFixer: added FK " + name + " -> users(id) on message.sender_id");
        } catch (SQLException e) {
            System.err.println("SchemaFixer: could not recreate FK for message.sender_id -> users(id): " + e.getMessage());
        }
    }

    /**
     * Repairs legacy schema where project.description is VARCHAR and too small (causes "Data too long").
     * The current schema expects TEXT.
     */
    private static void repairProjectDescriptionColumn(Connection c) throws SQLException {
        repairDescriptionColumnToText(c, "project");
    }

    /**
     * Repairs legacy schema where project_task.description is VARCHAR and too small (causes "Data too long").
     * The current schema expects TEXT.
     */
    private static void repairProjectTaskDescriptionColumn(Connection c) throws SQLException {
        repairDescriptionColumnToText(c, "project_task");
    }

    private static void repairDescriptionColumnToText(Connection c, String table) throws SQLException {
        if (!tableExists(c, table)) {
            return;
        }

        try (ResultSet rs = c.getMetaData().getColumns(safe(c.getCatalog()), null, table, "description")) {
            if (!rs.next()) {
                return;
            }
            String typeName = rs.getString("TYPE_NAME"); // e.g., VARCHAR, TEXT, LONGTEXT
            int size = rs.getInt("COLUMN_SIZE");
            String t = lower(typeName);

            // If it's already some kind of TEXT, we are good.
            if (t.contains("text")) {
                return;
            }

            // If it's VARCHAR (or similar) and small, upgrade to TEXT.
            if (t.contains("char") || t.contains("varchar")) {
                // Even VARCHAR(65535) could exist, but the typical broken case is 255/500/etc.
                if (size > 0 && size <= 2000) {
                    try {
                        exec(c, "ALTER TABLE `" + table + "` MODIFY COLUMN `description` TEXT NULL");
                        System.out.println("SchemaFixer: upgraded " + table + ".description to TEXT.");
                    } catch (SQLException e) {
                        System.err.println("SchemaFixer: could not upgrade " + table + ".description to TEXT: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            // Best-effort; ignore.
        }
    }

    private static String uniqueName(String base, Set<String> existing) {
        String candidate = base;
        int i = 2;
        while (existing.contains(candidate)) {
            candidate = base + "_" + i;
            i++;
        }
        return candidate;
    }

    private static List<ImportedKey> importedKeys(Connection c, String table) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        String catalog = safe(c.getCatalog());
        // MySQL uses "catalog" as the database name; schema is typically null.
        try (ResultSet rs = meta.getImportedKeys(catalog, null, table)) {
            List<ImportedKey> out = new ArrayList<>();
            while (rs.next()) {
                ImportedKey k = new ImportedKey();
                k.fkName = rs.getString("FK_NAME");
                k.fkColumn = rs.getString("FKCOLUMN_NAME");
                k.pkTable = rs.getString("PKTABLE_NAME");
                k.pkColumn = rs.getString("PKCOLUMN_NAME");
                out.add(k);
            }
            return out;
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        String catalog = safe(c.getCatalog());
        String tableUpper = table.toUpperCase(Locale.ROOT);
        String tableLower = table.toLowerCase(Locale.ROOT);
        try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name == null) {
                    continue;
                }
                String nUpper = name.toUpperCase(Locale.ROOT);
                String nLower = name.toLowerCase(Locale.ROOT);
                if (nUpper.equals(tableUpper) || nLower.equals(tableLower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static int countRows(Connection c, String table) {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("select count(*) from `" + table + "`")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static final class ImportedKey {
        String fkName;
        String fkColumn;
        String pkTable;
        String pkColumn;
    }
}
