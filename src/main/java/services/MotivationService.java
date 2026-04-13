package services;

import models.Motivation;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MotivationService implements IService<Motivation> {
    private Connection connection;
    public MotivationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Motivation entity) throws SQLException {
        String sql = "insert into `motivation` (motivation_level, emotion, preparation, reward, user_id) values(?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, entity.getMotivation_level());
        ps.setString(2, entity.getEmotion());
        ps.setString(3, entity.getPreparation());
        ps.setString(4, entity.getReward());
        ps.setInt(5, entity.getUser_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Motivation entity) throws SQLException {
        String sql = "update `motivation` set motivation_level = ?, emotion = ?, preparation = ?, reward = ?, user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, entity.getMotivation_level());
        ps.setString(2, entity.getEmotion());
        ps.setString(3, entity.getPreparation());
        ps.setString(4, entity.getReward());
        ps.setInt(5, entity.getUser_id());
        ps.setInt(6, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `motivation` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Motivation> recuperer() throws SQLException {
        String sql = "select * from `motivation`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Motivation> list = new ArrayList<>();
        while (rs.next()) {
            Motivation entity = new Motivation();
            entity.setId(rs.getInt("id"));
            entity.setMotivation_level(rs.getInt("motivation_level"));
            entity.setEmotion(rs.getString("emotion"));
            entity.setPreparation(rs.getString("preparation"));
            entity.setReward(rs.getString("reward"));
            entity.setUser_id(rs.getInt("user_id"));
            list.add(entity);
        }
        return list;
    }
}
