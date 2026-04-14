package services;

import models.Course;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements IService<Course> {
    private Connection connection;
    public CourseService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Course entity) throws SQLException {
        String sql = "insert into `course` (name, course_file, course_link, teacher_email, semester, difficulty_level, type, priority, coefficient, status, duration, comment, created_at, user_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
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
        ps.executeUpdate();
    }

    @Override
    public void modifier(Course entity) throws SQLException {
        String sql = "update `course` set name = ?, course_file = ?, course_link = ?, teacher_email = ?, semester = ?, difficulty_level = ?, type = ?, priority = ?, coefficient = ?, status = ?, duration = ?, comment = ?, created_at = ?, user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
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

    @Override
    public void supprimer(int id) throws SQLException {
        // 1. Cascade delete exams linked to this course
        new ExamService().supprimerParCours(id);

        // 2. Cascade delete activities linked to this course
        new ActivityService().supprimerParCours(id);

        // 3. Delete the course itself
        String sql = "delete from `course` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Course> recuperer() throws SQLException {
        String sql = "select * from `course`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Course> list = new ArrayList<>();
        while (rs.next()) {
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
            list.add(entity);
        }
        return list;
    }
}
