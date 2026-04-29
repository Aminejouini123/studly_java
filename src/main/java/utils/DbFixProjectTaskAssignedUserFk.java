package utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One-off DB fix:
 * Ensure project_task.assigned_user_id references users(id) (not user(id)).
 *
 * Run:
 *   mvn -q -DskipTests exec:java -Dexec.mainClass=utils.DbFixProjectTaskAssignedUserFk -Dexec.classpathScope=runtime
 */
public final class DbFixProjectTaskAssignedUserFk {
    private DbFixProjectTaskAssignedUserFk() {}

    public static void main(String[] args) throws Exception {
        try (Connection c = MyDatabase.getInstance().getConnection()) {
            if (c == null) {
                throw new IllegalStateException("DB connection is null. Check MyDatabase config.");
            }
            System.out.println("[db] Connected: " + safeDbInfo(c));

            String schema = "projet_db";
            String table = "project_task";
            String column = "assigned_user_id";

            ForeignKey fk = findFk(c, schema, table, column);
            if (fk == null) {
                System.out.println("[db] No FK found for " + table + "." + column + " (will add correct FK).");
                addCorrectFk(c, table);
                System.out.println("[db] Done.");
                return;
            }

            System.out.println("[db] Current FK: " + fk);
            if ("users".equalsIgnoreCase(fk.referencedTable)) {
                System.out.println("[db] FK already references users(id). Nothing to do.");
                return;
            }

            System.out.println("[db] Dropping FK: " + fk.constraintName);
            exec(c, "ALTER TABLE `" + table + "` DROP FOREIGN KEY `" + fk.constraintName + "`");

            // Ensure we don't fail because a constraint with the same name already exists.
            // We'll always add with our app's expected name.
            System.out.println("[db] Adding FK to users(id) with ON DELETE SET NULL");
            addCorrectFk(c, table);

            ForeignKey after = findFk(c, schema, table, column);
            System.out.println("[db] After: " + after);
            System.out.println("[db] Done.");
        }
    }

    private static void addCorrectFk(Connection c, String table) throws SQLException {
        // Use a stable name the app schema expects.
        exec(c,
                "ALTER TABLE `" + table + "` " +
                "ADD CONSTRAINT `fk_project_task_user` " +
                "FOREIGN KEY (`assigned_user_id`) REFERENCES `users`(`id`) " +
                "ON DELETE SET NULL"
        );
    }

    private static ForeignKey findFk(Connection c, String schema, String table, String column) throws SQLException {
        // information_schema is reliable for MySQL.
        String sql =
                "SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = ? " +
                "  AND TABLE_NAME = ? " +
                "  AND COLUMN_NAME = ? " +
                "  AND REFERENCED_TABLE_NAME IS NOT NULL " +
                "LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ForeignKey(
                        rs.getString("CONSTRAINT_NAME"),
                        rs.getString("REFERENCED_TABLE_NAME"),
                        rs.getString("REFERENCED_COLUMN_NAME")
                );
            }
        }
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static String safeDbInfo(Connection c) {
        try {
            DatabaseMetaData md = c.getMetaData();
            return md.getDatabaseProductName() + " " + md.getDatabaseProductVersion();
        } catch (SQLException e) {
            return "(unknown db)";
        }
    }

    private static final class ForeignKey {
        final String constraintName;
        final String referencedTable;
        final String referencedColumn;

        ForeignKey(String constraintName, String referencedTable, String referencedColumn) {
            this.constraintName = constraintName;
            this.referencedTable = referencedTable;
            this.referencedColumn = referencedColumn;
        }

        @Override
        public String toString() {
            return constraintName + " -> " + referencedTable + "(" + referencedColumn + ")";
        }
    }
}

