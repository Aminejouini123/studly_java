package services;

import models.Course;
import models.User;
import utils.MyDatabase;
import utils.SessionManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements IService<Course> {
    private final Connection connection;

    public CourseService() {
        connection = MyDatabase.getInstance().getConnection();
        applySelfHealing();
    }

    private void applySelfHealing() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            boolean hasValidUserFk = false;

            try (ResultSet rs = metaData.getImportedKeys(null, null, "course")) {
                while (rs.next()) {
                    String fkColumn = rs.getString("FKCOLUMN_NAME");
                    String fkName = rs.getString("FK_NAME");
                    String pkTable = rs.getString("PKTABLE_NAME");

                    if ("teacher_email".equalsIgnoreCase(fkColumn)) {
                        dropForeignKey(fkName);
                        continue;
                    }

                    if ("user_id".equalsIgnoreCase(fkColumn)) {
                        if ("users".equalsIgnoreCase(pkTable)) {
                            hasValidUserFk = true;
                        } else {
                            dropForeignKey(fkName);
                        }
                    }
                }
            }

            if (!hasValidUserFk) {
                ensureCourseUserForeignKey();
            }
        } catch (Exception ignored) {
            // Best-effort repair only.
        }
    }

    private void dropForeignKey(String fkName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE `course` DROP FOREIGN KEY `" + fkName + "`");
        }
    }

    private void ensureCourseUserForeignKey() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "ALTER TABLE `course` " +
                "ADD CONSTRAINT `fk_course_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE"
            );
        }
    }

    @Override
    public void ajouter(Course entity) throws SQLException {
        entity.setUser_id(resolveValidUserId(entity.getUser_id()));
        if (entity.getUser_id() <= 0) {
            throw new SQLException("Cannot add course: No valid user was found for this session.");
        }

        String sql = "insert into `course` (name, course_file, course_link, teacher_email, semester, difficulty_level, type, priority, coefficient, status, duration, comment, created_at, user_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getName());
            ps.setString(2, entity.getCourse_file());
            ps.setString(3, entity.getCourse_link());
            ps.setString(4, entity.getTeacher_email());
            ps.setString(5, entity.getSemester());
            ps.setString(6, entity.getDifficulty_level());
            ps.setString(7, entity.getType());
            ps.setString(8, entity.getPriority());
            ps.setDouble(9, entity.getCoefficient());
            ps.setString(10, normalizeStatus(entity.getStatus()));
            ps.setInt(11, entity.getDuration());
            ps.setString(12, entity.getComment());
            ps.setTimestamp(13, entity.getCreated_at());
            ps.setInt(14, entity.getUser_id());
            ps.executeUpdate();
        }
    }

    private String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? "Active" : status;
    }

    private int resolveValidUserId(int requestedUserId) throws SQLException {
        if (requestedUserId > 0 && userExistsById(requestedUserId)) {
            return requestedUserId;
        }

        User sessionUser = SessionManager.getCurrentUser();
        if (sessionUser != null) {
            int sessionUserId = sessionUser.getId();
            if (sessionUserId > 0 && userExistsById(sessionUserId)) {
                return sessionUserId;
            }

            String sessionEmail = sessionUser.getEmail();
            if (sessionEmail != null && !sessionEmail.isBlank()) {
                int resolvedId = findUserIdByEmail(sessionEmail.trim());
                if (resolvedId > 0) {
                    return resolvedId;
                }
            }
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM `users` ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        return 0;
    }

    private boolean userExistsById(int userId) throws SQLException {
        String sql = "SELECT 1 FROM `users` WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int findUserIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM `users` WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return 0;
    }

    @Override
    public void modifier(Course entity) throws SQLException {
        String sql = "update `course` set name = ?, course_file = ?, course_link = ?, teacher_email = ?, semester = ?, difficulty_level = ?, type = ?, priority = ?, coefficient = ?, status = ?, duration = ?, comment = ?, created_at = ?, user_id = ? where id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getName());
            ps.setString(2, entity.getCourse_file());
            ps.setString(3, entity.getCourse_link());
            ps.setString(4, entity.getTeacher_email());
            ps.setString(5, entity.getSemester());
            ps.setString(6, entity.getDifficulty_level());
            ps.setString(7, entity.getType());
            ps.setString(8, entity.getPriority());
            ps.setDouble(9, entity.getCoefficient());
            ps.setString(10, entity.getStatus());
            ps.setInt(11, entity.getDuration());
            ps.setString(12, entity.getComment());
            ps.setTimestamp(13, entity.getCreated_at());
            ps.setInt(14, entity.getUser_id());
            ps.setInt(15, entity.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        new ExamService().supprimerParCours(id);
        new ActivityService().supprimerParCours(id);

        String sql = "delete from `course` where id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Course> recuperer() throws SQLException {
        String sql = "select * from `course`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            List<Course> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapCourse(rs));
            }
            return list;
        }
    }

    public List<Course> recupererParUser(int userId) throws SQLException {
        String sql = "select * from `course` where user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Course> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapCourse(rs));
                }
                return list;
            }
        }
    }

    private Course mapCourse(ResultSet rs) throws SQLException {
        Course entity = new Course();
        entity.setId(rs.getInt("id"));
        entity.setName(rs.getString("name"));
        entity.setCourse_file(rs.getString("course_file"));
        entity.setCourse_link(rs.getString("course_link"));
        entity.setTeacher_email(rs.getString("teacher_email"));
        entity.setSemester(rs.getString("semester"));
        entity.setDifficulty_level(rs.getString("difficulty_level"));
        entity.setType(rs.getString("type"));
        entity.setPriority(rs.getString("priority"));
        entity.setCoefficient(rs.getDouble("coefficient"));
        entity.setStatus(rs.getString("status"));
        entity.setDuration(rs.getInt("duration"));
        entity.setComment(rs.getString("comment"));
        entity.setCreated_at(rs.getTimestamp("created_at"));
        entity.setUser_id(rs.getInt("user_id"));
        return entity;
    }
}
