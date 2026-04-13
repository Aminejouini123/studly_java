package services;

import models.Invitation;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvitationService implements IService<Invitation> {
    private Connection connection;
    public InvitationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Invitation entity) throws SQLException {
        String sql = "insert into `invitation` (status, created_at, sender_id, receiver_id, group_id) values(?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getStatus());
        ps.setTimestamp(2, entity.getCreated_at());
        ps.setInt(3, entity.getSender_id());
        ps.setInt(4, entity.getReceiver_id());
        ps.setInt(5, entity.getGroup_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Invitation entity) throws SQLException {
        String sql = "update `invitation` set status = ?, created_at = ?, sender_id = ?, receiver_id = ?, group_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getStatus());
        ps.setTimestamp(2, entity.getCreated_at());
        ps.setInt(3, entity.getSender_id());
        ps.setInt(4, entity.getReceiver_id());
        ps.setInt(5, entity.getGroup_id());
        ps.setInt(6, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `invitation` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Invitation> recuperer() throws SQLException {
        String sql = "select * from `invitation`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Invitation> list = new ArrayList<>();
        while (rs.next()) {
            Invitation entity = new Invitation();
            entity.setId(rs.getInt("id"));
            entity.setStatus(rs.getString("status"));
            entity.setCreated_at(rs.getTimestamp("created_at"));
            entity.setSender_id(rs.getInt("sender_id"));
            entity.setReceiver_id(rs.getInt("receiver_id"));
            entity.setGroup_id(rs.getInt("group_id"));
            list.add(entity);
        }
        return list;
    }
}
