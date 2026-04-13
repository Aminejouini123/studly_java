package models;

public class Course {
    private int id;
    private String name;
    private String course_file;
    private String course_link;
    private String teacher_email;
    private String semester;
    private String difficulty_level;
    private String type;
    private String priority;
    private double coefficient;
    private String status;
    private int duration;
    private String comment;
    private java.sql.Timestamp created_at;
    private int user_id;

    public Course() {}

    public Course(String name, String course_file, String course_link, String teacher_email, String semester, String difficulty_level, String type, String priority, double coefficient, String status, int duration, String comment, java.sql.Timestamp created_at, int user_id) {
        this.name = name;
        this.course_file = course_file;
        this.course_link = course_link;
        this.teacher_email = teacher_email;
        this.semester = semester;
        this.difficulty_level = difficulty_level;
        this.type = type;
        this.priority = priority;
        this.coefficient = coefficient;
        this.status = status;
        this.duration = duration;
        this.comment = comment;
        this.created_at = created_at;
        this.user_id = user_id;
    }

    public Course(int id, String name, String course_file, String course_link, String teacher_email, String semester, String difficulty_level, String type, String priority, double coefficient, String status, int duration, String comment, java.sql.Timestamp created_at, int user_id) {
        this.id = id;
        this.name = name;
        this.course_file = course_file;
        this.course_link = course_link;
        this.teacher_email = teacher_email;
        this.semester = semester;
        this.difficulty_level = difficulty_level;
        this.type = type;
        this.priority = priority;
        this.coefficient = coefficient;
        this.status = status;
        this.duration = duration;
        this.comment = comment;
        this.created_at = created_at;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCourse_file() { return course_file; }
    public void setCourse_file(String course_file) { this.course_file = course_file; }
    public String getCourse_link() { return course_link; }
    public void setCourse_link(String course_link) { this.course_link = course_link; }
    public String getTeacher_email() { return teacher_email; }
    public void setTeacher_email(String teacher_email) { this.teacher_email = teacher_email; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getDifficulty_level() { return difficulty_level; }
    public void setDifficulty_level(String difficulty_level) { this.difficulty_level = difficulty_level; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public double getCoefficient() { return coefficient; }
    public void setCoefficient(double coefficient) { this.coefficient = coefficient; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id
 + ", name=" + name
 + ", course_file=" + course_file
 + ", course_link=" + course_link
 + ", teacher_email=" + teacher_email
 + ", semester=" + semester
 + ", difficulty_level=" + difficulty_level
 + ", type=" + type
 + ", priority=" + priority
 + ", coefficient=" + coefficient
 + ", status=" + status
 + ", duration=" + duration
 + ", comment=" + comment
 + ", created_at=" + created_at
 + ", user_id=" + user_id
                + '}';
    }
}
