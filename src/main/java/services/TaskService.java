package services;

import models.Task;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskService implements IService<Task> {
    private Connection connection;
    public TaskService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Task entity) throws SQLException {
        String sql = "insert into `task` (title, description, repeat_count, status, difficulty, impact, deadline, completed_at, objective_id, assigned_user_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setInt(3, entity.getRepeat_count());
        ps.setString(4, entity.getStatus());
        ps.setInt(5, entity.getDifficulty());
        ps.setDouble(6, entity.getImpact());
        ps.setTimestamp(7, entity.getDeadline());
        ps.setTimestamp(8, entity.getCompleted_at());
        ps.setInt(9, entity.getObjective_id());
        ps.setInt(10, entity.getAssigned_user_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Task entity) throws SQLException {
        String sql = "update `task` set title = ?, description = ?, repeat_count = ?, status = ?, difficulty = ?, impact = ?, deadline = ?, completed_at = ?, objective_id = ?, assigned_user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setInt(3, entity.getRepeat_count());
        ps.setString(4, entity.getStatus());
        ps.setInt(5, entity.getDifficulty());
        ps.setDouble(6, entity.getImpact());
        ps.setTimestamp(7, entity.getDeadline());
        ps.setTimestamp(8, entity.getCompleted_at());
        ps.setInt(9, entity.getObjective_id());
        ps.setInt(10, entity.getAssigned_user_id());
        ps.setInt(11, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `task` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Task> recuperer() throws SQLException {
        String sql = "select * from `task`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Task> list = new ArrayList<>();
        while (rs.next()) {
            Task entity = new Task();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDescription(rs.getString("description"));
            entity.setRepeat_count(rs.getInt("repeat_count"));
            entity.setStatus(rs.getString("status"));
            entity.setDifficulty(rs.getInt("difficulty"));
            entity.setImpact(rs.getDouble("impact"));
            entity.setDeadline(rs.getTimestamp("deadline"));
            entity.setCompleted_at(rs.getTimestamp("completed_at"));
            entity.setObjective_id(rs.getInt("objective_id"));
            entity.setAssigned_user_id(rs.getInt("assigned_user_id"));
            list.add(entity);
        }
        return list;
    }
}
