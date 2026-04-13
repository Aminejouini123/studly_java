package services;

import models.Exam;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamService implements IService<Exam> {
    private Connection connection;
    public ExamService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Exam entity) throws SQLException {
        String sql = "insert into `exam` (title, date, duration, grade, difficulty, status, file, link, course_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setDate(2, entity.getDate());
        ps.setInt(3, entity.getDuration());
        ps.setDouble(4, entity.getGrade());
        ps.setString(5, entity.getDifficulty());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getFile());
        ps.setString(8, entity.getLink());
        ps.setInt(9, entity.getCourse_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Exam entity) throws SQLException {
        String sql = "update `exam` set title = ?, date = ?, duration = ?, grade = ?, difficulty = ?, status = ?, file = ?, link = ?, course_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setDate(2, entity.getDate());
        ps.setInt(3, entity.getDuration());
        ps.setDouble(4, entity.getGrade());
        ps.setString(5, entity.getDifficulty());
        ps.setString(6, entity.getStatus());
        ps.setString(7, entity.getFile());
        ps.setString(8, entity.getLink());
        ps.setInt(9, entity.getCourse_id());
        ps.setInt(10, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `exam` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Exam> recuperer() throws SQLException {
        String sql = "select * from `exam`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Exam> list = new ArrayList<>();
        while (rs.next()) {
            Exam entity = new Exam();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDate(rs.getDate("date"));
            entity.setDuration(rs.getInt("duration"));
            entity.setGrade(rs.getDouble("grade"));
            entity.setDifficulty(rs.getString("difficulty"));
            entity.setStatus(rs.getString("status"));
            entity.setFile(rs.getString("file"));
            entity.setLink(rs.getString("link"));
            entity.setCourse_id(rs.getInt("course_id"));
            list.add(entity);
        }
        return list;
    }
}
