package services;

import models.Event;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService implements IService<Event> {
    private Connection connection;
    public EventService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Event entity) throws SQLException {
        String sql = "insert into `event` (title, description, type, duration, location, status, priority, difficulty, date, start_time, end_time, color, category, notes, all_day, reminder_minutes, google_event_id, motivation_id, user_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getType());
        ps.setInt(4, entity.getDuration());
        ps.setString(5, entity.getLocation());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getPriority());
        ps.setInt(8, entity.getDifficulty());
        ps.setDate(9, entity.getDate());
        ps.setTimestamp(10, entity.getStart_time());
        ps.setTimestamp(11, entity.getEnd_time());
        ps.setString(12, entity.getColor());
        ps.setString(13, entity.getCategory());
        ps.setString(14, entity.getNotes());
        ps.setInt(15, entity.getAll_day());
        ps.setInt(16, entity.getReminder_minutes());
        ps.setString(17, entity.getGoogle_event_id());
        ps.setInt(18, entity.getMotivation_id());
        ps.setInt(19, entity.getUser_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Event entity) throws SQLException {
        String sql = "update `event` set title = ?, description = ?, type = ?, duration = ?, location = ?, status = ?, priority = ?, difficulty = ?, date = ?, start_time = ?, end_time = ?, color = ?, category = ?, notes = ?, all_day = ?, reminder_minutes = ?, google_event_id = ?, motivation_id = ?, user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getType());
        ps.setInt(4, entity.getDuration());
        ps.setString(5, entity.getLocation());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getPriority());
        ps.setInt(8, entity.getDifficulty());
        ps.setDate(9, entity.getDate());
        ps.setTimestamp(10, entity.getStart_time());
        ps.setTimestamp(11, entity.getEnd_time());
        ps.setString(12, entity.getColor());
        ps.setString(13, entity.getCategory());
        ps.setString(14, entity.getNotes());
        ps.setInt(15, entity.getAll_day());
        ps.setInt(16, entity.getReminder_minutes());
        ps.setString(17, entity.getGoogle_event_id());
        ps.setInt(18, entity.getMotivation_id());
        ps.setInt(19, entity.getUser_id());
        ps.setInt(20, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `event` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Event> recuperer() throws SQLException {
        String sql = "select * from `event`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Event> list = new ArrayList<>();
        while (rs.next()) {
            Event entity = new Event();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDescription(rs.getString("description"));
            entity.setType(rs.getString("type"));
            entity.setDuration(rs.getInt("duration"));
            entity.setLocation(rs.getString("location"));
            entity.setStatus(rs.getString("status"));
            entity.setPriority(rs.getString("priority"));
            entity.setDifficulty(rs.getInt("difficulty"));
            entity.setDate(rs.getDate("date"));
            entity.setStart_time(rs.getTimestamp("start_time"));
            entity.setEnd_time(rs.getTimestamp("end_time"));
            entity.setColor(rs.getString("color"));
            entity.setCategory(rs.getString("category"));
            entity.setNotes(rs.getString("notes"));
            entity.setAll_day(rs.getInt("all_day"));
            entity.setReminder_minutes(rs.getInt("reminder_minutes"));
            entity.setGoogle_event_id(rs.getString("google_event_id"));
            entity.setMotivation_id(rs.getInt("motivation_id"));
            entity.setUser_id(rs.getInt("user_id"));
            list.add(entity);
        }
        return list;
    }
}
