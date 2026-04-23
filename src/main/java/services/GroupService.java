package services;

import models.Group;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

public class GroupService implements IService<Group> {
    private final Connection connection;
    private final Set<String> groupColumns = new HashSet<>();
    private final Map<String, ForeignKeyRef> foreignKeysByColumn = new HashMap<>();
    private final Map<String, Boolean> groupNullableByColumn = new HashMap<>();

    private static final String CREATOR_ID_COL = "creator_id";

    public GroupService() {
        this.connection = MyDatabase.getInstance().getConnection();
        loadGroupColumns();
        loadForeignKeys();
    }

    @Override
    public void ajouter(Group group) throws SQLException {
        requireConnection();
        boolean hasCreatedAt = hasColumn("created_at");
        boolean hasCreatorId = hasColumn("creator_id");

        StringBuilder columns = new StringBuilder("capacity, group_photo, category");
        StringBuilder values = new StringBuilder("?, ?, ?");

        if (hasCreatorId) {
            columns.append(", creator_id");
            values.append(", ?");
        }

        if (hasCreatedAt) {
            columns.append(", created_at");
            values.append(", CURRENT_TIMESTAMP");
        }

        String sql = "insert into `groups` (" + columns + ") values (" + values + ")";
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 1;
        ps.setInt(i++, group.getCapacity());
        ps.setString(i++, group.getGroupPhoto());
        ps.setString(i++, group.getCategory());
        if (hasCreatorId) {
            int creatorId = group.getCreatorId();
            if (creatorId > 0) {
                ps.setInt(i, requireValidForeignKeyId(CREATOR_ID_COL, creatorId));
            } else if (isNullableColumn("creator_id")) {
                // Temporary: allow groups without user while User module is not ready.
                // Convention: creatorId <= 0 means "no creator".
                // TODO remplacer par vrai User quand module pret
                ps.setNull(i, Types.INTEGER);
            } else {
                Integer resolvedCreatorId = resolveDefaultCreatorId();
                if (resolvedCreatorId == null) {
                    throw new IllegalStateException("Invalid creator_id: no users exist. Create a user first (or make groups.creator_id nullable).");
                }
                ps.setInt(i, requireValidForeignKeyId(CREATOR_ID_COL, resolvedCreatorId));
            }
        }
        ps.executeUpdate();
    }

    @Override
    public void modifier(Group group) throws SQLException {
        requireConnection();

        boolean hasCreatorId = hasColumn("creator_id");
        String sql = hasCreatorId
                ? "update `groups` set capacity = ?, group_photo = ?, category = ?, creator_id = ? where id = ?"
                : "update `groups` set capacity = ?, group_photo = ?, category = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        int i = 1;
        ps.setInt(i++, group.getCapacity());
        ps.setString(i++, group.getGroupPhoto());
        ps.setString(i++, group.getCategory());
        if (hasCreatorId) {
            int creatorId = group.getCreatorId();
            if (creatorId > 0) {
                ps.setInt(i++, requireValidForeignKeyId(CREATOR_ID_COL, creatorId));
            } else if (isNullableColumn("creator_id")) {
                // Temporary: allow groups without user while User module is not ready.
                // Convention: creatorId <= 0 means "no creator".
                // TODO remplacer par vrai User quand module pret
                ps.setNull(i++, Types.INTEGER);
            } else {
                Integer resolvedCreatorId = resolveDefaultCreatorId();
                if (resolvedCreatorId == null) {
                    throw new IllegalStateException("Invalid creator_id: no users exist. Create a user first (or make groups.creator_id nullable).");
                }
                ps.setInt(i++, requireValidForeignKeyId(CREATOR_ID_COL, resolvedCreatorId));
            }
        }
        ps.setInt(i, group.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        requireConnection();
        String sql = "delete from `groups` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Group> recuperer() throws SQLException {
        requireConnection();
        boolean hasCreatorId = hasColumn("creator_id");
        boolean hasCreatedAt = hasColumn("created_at");

        String sql;
        if (hasCreatorId && hasCreatedAt) {
            sql = "select id, capacity, group_photo, category, creator_id, created_at from `groups`";
        } else if (hasCreatorId) {
            sql = "select id, capacity, group_photo, category, creator_id from `groups`";
        } else if (hasCreatedAt) {
            sql = "select id, capacity, group_photo, category, created_at from `groups`";
        } else {
            sql = "select id, capacity, group_photo, category from `groups`";
        }
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Group> list = new ArrayList<>();
        while (rs.next()) {
            Group group = new Group();
            group.setId(rs.getInt("id"));
            group.setCapacity(rs.getInt("capacity"));
            group.setGroupPhoto(rs.getString("group_photo"));
            group.setCategory(rs.getString("category"));
            if (hasCreatorId) {
                group.setCreatorId(rs.getInt("creator_id"));
            }
            if (hasCreatedAt) {
                group.setCreatedAt(rs.getTimestamp("created_at"));
            }
            list.add(group);
        }
        return list;
    }

    public List<Integer> listForeignKeyIds(String fkColumn) {
        requireConnection();
        if ("creator_id".equalsIgnoreCase(fkColumn)) {
            ensureSampleUsers(2);
        }
        ForeignKeyRef ref = foreignKeysByColumn.get(fkColumn.toLowerCase());
        if (ref == null) {
            return Collections.emptyList();
        }
        String sql = "select " + ref.pkColumn + " from `" + ref.pkTable + "` order by " + ref.pkColumn + " asc limit 200";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    public void ensureSampleUsers(int desiredCount) {
        if (connection == null) {
            return;
        }

        ForeignKeyRef creatorRef = foreignKeysByColumn.get(CREATOR_ID_COL);
        if (creatorRef == null || !looksLikeUsersTable(creatorRef.pkTable)) {
            return;
        }

        String usersTable = creatorRef.pkTable;
        int existing = countRows(usersTable);
        if (existing >= desiredCount) {
            return;
        }

        for (int i = existing; i < desiredCount; i++) {
            insertDemoUser(usersTable, i + 1);
        }
    }

    private void requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not available. Check MySQL and utils.MyDatabase settings.");
        }
    }

    private void loadGroupColumns() {
        if (connection == null) {
            return;
        }
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "groups", null);
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name != null) {
                    String lower = name.toLowerCase(Locale.ROOT);
                    groupColumns.add(lower);

                    // Track nullability for temporary "creator nullable" test mode.
                    // TODO remplacer par vrai User quand module pret
                    String isNullable = rs.getString("IS_NULLABLE");
                    groupNullableByColumn.put(lower, "YES".equalsIgnoreCase(isNullable));
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private void loadForeignKeys() {
        if (connection == null) {
            return;
        }
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getImportedKeys(null, null, "groups");
            while (rs.next()) {
                String fkColumn = rs.getString("FKCOLUMN_NAME");
                String pkTable = rs.getString("PKTABLE_NAME");
                String pkColumn = rs.getString("PKCOLUMN_NAME");
                if (fkColumn != null && pkTable != null && pkColumn != null) {
                    foreignKeysByColumn.put(fkColumn.toLowerCase(), new ForeignKeyRef(pkTable, pkColumn));
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private boolean hasColumn(String columnName) {
        return groupColumns.contains(columnName.toLowerCase());
    }

    private boolean isNullableColumn(String columnName) {
        Boolean nullable = groupNullableByColumn.get(columnName.toLowerCase(Locale.ROOT));
        return nullable != null && nullable;
    }

    public boolean hasCreatorIdColumn() {
        return hasColumn("creator_id");
    }

    private int requireValidForeignKeyId(String fkColumn, int id) {
        if (id <= 0) {
            throw new IllegalStateException("Invalid " + fkColumn + " value (must be > 0).");
        }

        ForeignKeyRef ref = foreignKeysByColumn.get(fkColumn.toLowerCase());
        if (ref == null) {
            return id;
        }

        if (!existsById(ref.pkTable, ref.pkColumn, id)) {
            if (CREATOR_ID_COL.equalsIgnoreCase(fkColumn)) {
                // If caller gave a bad creator id, try to fall back to an existing (or demo) user.
                ensureSampleUsers(1);
                Integer first = firstId(ref.pkTable, ref.pkColumn);
                if (first != null) {
                    return first;
                }
            }
            throw new IllegalStateException("Invalid " + fkColumn + ": user id " + id + " does not exist.");
        }

        return id;
    }

    private boolean existsById(String table, String column, int id) {
        String sql = "select 1 from `" + table + "` where " + column + " = ? limit 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private Integer firstId(String table, String column) {
        String sql = "select " + column + " from `" + table + "` order by " + column + " asc limit 1";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    private int countRows(String table) {
        String sql = "select count(*) from `" + table + "`";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private void insertDemoUser(String usersTable, int index) {
        try {
            List<ColumnSpec> requiredColumns = getRequiredColumns(usersTable);
            if (requiredColumns.isEmpty()) {
                // Fallback: try minimal insert if schema allows defaults.
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("insert into `" + usersTable + "` () values ()");
                }
                return;
            }

            long nonce = System.currentTimeMillis();
            String email = "demo" + index + "_" + nonce + "@studly.local";

            StringBuilder cols = new StringBuilder();
            StringBuilder vals = new StringBuilder();
            List<ColumnSpec> bindColumns = new ArrayList<>();

            for (ColumnSpec col : requiredColumns) {
                if (cols.length() > 0) {
                    cols.append(", ");
                    vals.append(", ");
                }
                cols.append(col.name);
                vals.append("?");
                bindColumns.add(col);
            }

            String sql = "insert into `" + usersTable + "` (" + cols + ") values (" + vals + ")";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int p = 1;
                for (ColumnSpec col : bindColumns) {
                    setDemoValue(ps, p++, col, email, index);
                }
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    private List<ColumnSpec> getRequiredColumns(String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet rs = meta.getColumns(null, null, table, null);
        List<ColumnSpec> required = new ArrayList<>();
        while (rs.next()) {
            String columnName = rs.getString("COLUMN_NAME");
            String isAuto = rs.getString("IS_AUTOINCREMENT");
            String isNullable = rs.getString("IS_NULLABLE");
            String defaultValue = rs.getString("COLUMN_DEF");
            int dataType = rs.getInt("DATA_TYPE");

            if (columnName == null) {
                continue;
            }

            String colLower = columnName.toLowerCase(Locale.ROOT);
            boolean autoInc = "YES".equalsIgnoreCase(isAuto) || colLower.equals("id");
            boolean notNull = "NO".equalsIgnoreCase(isNullable);
            boolean hasDefault = defaultValue != null;

            if (autoInc) {
                continue;
            }

            if (notNull && !hasDefault) {
                required.add(new ColumnSpec(columnName, dataType));
            }
        }
        return required;
    }

    private void setDemoValue(PreparedStatement ps, int parameterIndex, ColumnSpec col, String email, int index) throws SQLException {
        String name = col.name.toLowerCase(Locale.ROOT);
        int type = col.jdbcType;

        if (name.contains("email")) {
            ps.setString(parameterIndex, email);
            return;
        }
        if (name.contains("password")) {
            ps.setString(parameterIndex, "demo");
            return;
        }
        if (name.contains("roles")) {
            ps.setString(parameterIndex, "[\"ROLE_USER\"]");
            return;
        }
        if (name.contains("first_name")) {
            ps.setString(parameterIndex, "Demo" + index);
            return;
        }
        if (name.contains("last_name")) {
            ps.setString(parameterIndex, "User");
            return;
        }
        if (name.contains("is_verified")) {
            ps.setInt(parameterIndex, 1);
            return;
        }
        if (name.contains("score")) {
            ps.setInt(parameterIndex, 0);
            return;
        }
        if (name.contains("created_at") || name.contains("updated_at") || type == Types.TIMESTAMP) {
            ps.setTimestamp(parameterIndex, new java.sql.Timestamp(System.currentTimeMillis()));
            return;
        }
        if (name.contains("date_of_birth") || type == Types.DATE) {
            LocalDate dob = LocalDate.now(ZoneId.systemDefault()).minusYears(20);
            ps.setDate(parameterIndex, java.sql.Date.valueOf(dob));
            return;
        }

        switch (type) {
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
                ps.setInt(parameterIndex, 0);
                break;
            case Types.BOOLEAN:
            case Types.BIT:
                ps.setBoolean(parameterIndex, false);
                break;
            default:
                ps.setString(parameterIndex, "demo");
        }
    }

    private static final class ColumnSpec {
        private final String name;
        private final int jdbcType;

        private ColumnSpec(String name, int jdbcType) {
            this.name = name;
            this.jdbcType = jdbcType;
        }
    }

    private static int defaultCreatorId() {
        String fromEnv = System.getenv("STUDLY_CREATOR_ID");
        String fromProp = System.getProperty("studly.creatorId");
        return parseIntOrDefault(fromProp != null ? fromProp : fromEnv, 1);
    }

    private Integer resolveDefaultCreatorId() {
        // If the DB already has users, pick the first id. If not, try to create a demo user (best-effort).
        ForeignKeyRef ref = foreignKeysByColumn.get(CREATOR_ID_COL);
        if (ref != null && looksLikeUsersTable(ref.pkTable)) {
            ensureSampleUsers(1);
            Integer first = firstId(ref.pkTable, ref.pkColumn);
            if (first != null) {
                return first;
            }
            int configured = defaultCreatorId();
            if (configured > 0 && existsById(ref.pkTable, ref.pkColumn, configured)) {
                return configured;
            }
            return null;
        }

        // Fallback: configured id (may still fail validation if FK exists, but we don't know the referenced table).
        return defaultCreatorId();
    }

    private static boolean looksLikeUsersTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String lower = tableName.toLowerCase(Locale.ROOT);
        return "users".equals(lower) || "user".equals(lower) || lower.endsWith("_users") || lower.endsWith("_user");
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static final class ForeignKeyRef {
        private final String pkTable;
        private final String pkColumn;

        private ForeignKeyRef(String pkTable, String pkColumn) {
            this.pkTable = pkTable;
            this.pkColumn = pkColumn;
        }
    }
}
