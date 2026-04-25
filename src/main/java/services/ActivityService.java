package services;

import models.Activity;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityService implements IService<Activity> {
    private Connection connection;

    public ActivityService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Activity entity) throws SQLException {
        String sql = "insert into `activity` (title, description, file, link, duration, status, difficulty, level, type, instructions, expected_output, hints, completed_at, course_id, assigned_user_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getFile());
        ps.setString(4, entity.getLink());
        ps.setInt(5, entity.getDuration());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getDifficulty());
        ps.setString(8, entity.getLevel());
        ps.setString(9, entity.getType());
        ps.setString(10, entity.getInstructions());
        ps.setString(11, entity.getExpected_output());
        ps.setString(12, entity.getHints());
        ps.setTimestamp(13, entity.getCompleted_at());
        ps.setInt(14, entity.getCourse_id());
        ps.setInt(15, entity.getAssigned_user_id());
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            entity.setId(rs.getInt(1));
        }
    }

    @Override
    public void modifier(Activity entity) throws SQLException {
        String sql = "update `activity` set title = ?, description = ?, file = ?, link = ?, duration = ?, status = ?, difficulty = ?, level = ?, type = ?, instructions = ?, expected_output = ?, hints = ?, completed_at = ?, course_id = ?, assigned_user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getFile());
        ps.setString(4, entity.getLink());
        ps.setInt(5, entity.getDuration());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getDifficulty());
        ps.setString(8, entity.getLevel());
        ps.setString(9, entity.getType());
        ps.setString(10, entity.getInstructions());
        ps.setString(11, entity.getExpected_output());
        ps.setString(12, entity.getHints());
        ps.setTimestamp(13, entity.getCompleted_at());
        ps.setInt(14, entity.getCourse_id());
        ps.setInt(15, entity.getAssigned_user_id());
        ps.setInt(16, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `activity` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Activity> recuperer() throws SQLException {
        String sql = "select * from `activity`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Activity> list = new ArrayList<>();
        while (rs.next()) {
            Activity entity = new Activity();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDescription(rs.getString("description"));
            entity.setFile(rs.getString("file"));
            entity.setLink(rs.getString("link"));
            entity.setDuration(rs.getInt("duration"));
            entity.setStatus(rs.getString("status"));
            entity.setDifficulty(rs.getString("difficulty"));
            entity.setLevel(rs.getString("level"));
            entity.setType(rs.getString("type"));
            entity.setInstructions(rs.getString("instructions"));
            entity.setExpected_output(rs.getString("expected_output"));
            entity.setHints(rs.getString("hints"));
            entity.setCompleted_at(rs.getTimestamp("completed_at"));
            entity.setCourse_id(rs.getInt("course_id"));
            entity.setAssigned_user_id(rs.getInt("assigned_user_id"));
            list.add(entity);
        }
        return list;
    }

    public List<Activity> recupererParCours(int courseId) throws SQLException {
        String sql = "select * from `activity` where course_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, courseId);
        ResultSet rs = ps.executeQuery();
        List<Activity> list = new ArrayList<>();
        while (rs.next()) {
            Activity entity = new Activity();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDescription(rs.getString("description"));
            entity.setFile(rs.getString("file"));
            entity.setLink(rs.getString("link"));
            entity.setDuration(rs.getInt("duration"));
            entity.setStatus(rs.getString("status"));
            entity.setDifficulty(rs.getString("difficulty"));
            entity.setLevel(rs.getString("level"));
            entity.setType(rs.getString("type"));
            entity.setInstructions(rs.getString("instructions"));
            entity.setExpected_output(rs.getString("expected_output"));
            entity.setHints(rs.getString("hints"));
            entity.setCompleted_at(rs.getTimestamp("completed_at"));
            entity.setCourse_id(rs.getInt("course_id"));
            entity.setAssigned_user_id(rs.getInt("assigned_user_id"));
            list.add(entity);
        }
        return list;
    }

    public void supprimerParCours(int courseId) throws SQLException {
        String sql = "delete from `activity` where course_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, courseId);
        ps.executeUpdate();
    }
}
