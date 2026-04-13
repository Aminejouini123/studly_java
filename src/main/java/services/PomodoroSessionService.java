package services;

import models.PomodoroSession;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PomodoroSessionService implements IService<PomodoroSession> {
    private Connection connection;
    public PomodoroSessionService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(PomodoroSession entity) throws SQLException {
        String sql = "insert into `pomodoro_session` (type, duration, status, started_at, ended_at, focus_score, focus_logs, event_id) values(?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getType());
        ps.setInt(2, entity.getDuration());
        ps.setString(3, entity.getStatus());
        ps.setTimestamp(4, entity.getStarted_at());
        ps.setTimestamp(5, entity.getEnded_at());
        ps.setDouble(6, entity.getFocus_score());
        ps.setString(7, entity.getFocus_logs());
        ps.setInt(8, entity.getEvent_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(PomodoroSession entity) throws SQLException {
        String sql = "update `pomodoro_session` set type = ?, duration = ?, status = ?, started_at = ?, ended_at = ?, focus_score = ?, focus_logs = ?, event_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getType());
        ps.setInt(2, entity.getDuration());
        ps.setString(3, entity.getStatus());
        ps.setTimestamp(4, entity.getStarted_at());
        ps.setTimestamp(5, entity.getEnded_at());
        ps.setDouble(6, entity.getFocus_score());
        ps.setString(7, entity.getFocus_logs());
        ps.setInt(8, entity.getEvent_id());
        ps.setInt(9, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `pomodoro_session` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<PomodoroSession> recuperer() throws SQLException {
        String sql = "select * from `pomodoro_session`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<PomodoroSession> list = new ArrayList<>();
        while (rs.next()) {
            PomodoroSession entity = new PomodoroSession();
            entity.setId(rs.getInt("id"));
            entity.setType(rs.getString("type"));
            entity.setDuration(rs.getInt("duration"));
            entity.setStatus(rs.getString("status"));
            entity.setStarted_at(rs.getTimestamp("started_at"));
            entity.setEnded_at(rs.getTimestamp("ended_at"));
            entity.setFocus_score(rs.getDouble("focus_score"));
            entity.setFocus_logs(rs.getString("focus_logs"));
            entity.setEvent_id(rs.getInt("event_id"));
            list.add(entity);
        }
        return list;
    }
}
