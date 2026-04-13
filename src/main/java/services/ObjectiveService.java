package services;

import models.Objective;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectiveService implements IService<Objective> {
    private Connection connection;
    public ObjectiveService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Objective entity) throws SQLException {
        String sql = "insert into `objective` (title, description, estimated_duration, real_duration, priority, status, reason) values(?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getEstimated_duration());
        ps.setInt(4, entity.getReal_duration());
        ps.setString(5, entity.getPriority());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getReason());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Objective entity) throws SQLException {
        String sql = "update `objective` set title = ?, description = ?, estimated_duration = ?, real_duration = ?, priority = ?, status = ?, reason = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getEstimated_duration());
        ps.setInt(4, entity.getReal_duration());
        ps.setString(5, entity.getPriority());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getReason());
        ps.setInt(8, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `objective` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Objective> recuperer() throws SQLException {
        String sql = "select * from `objective`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Objective> list = new ArrayList<>();
        while (rs.next()) {
            Objective entity = new Objective();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDescription(rs.getString("description"));
            entity.setEstimated_duration(rs.getString("estimated_duration"));
            entity.setReal_duration(rs.getInt("real_duration"));
            entity.setPriority(rs.getString("priority"));
            entity.setStatus(rs.getString("status"));
            entity.setReason(rs.getString("reason"));
            list.add(entity);
        }
        return list;
    }
}
