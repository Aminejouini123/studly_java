package models;

public class Exam {
    private int id;
    private String title;
    private java.sql.Date date;
    private int duration;
    private double grade;
    private String difficulty;
    private String status;
    private String file;
    private String link;
    private int course_id;

    public Exam() {}

    public Exam(String title, java.sql.Date date, int duration, double grade, String difficulty, String status, String file, String link, int course_id) {
        this.title = title;
        this.date = date;
        this.duration = duration;
        this.grade = grade;
        this.difficulty = difficulty;
        this.status = status;
        this.file = file;
        this.link = link;
        this.course_id = course_id;
    }

    public Exam(int id, String title, java.sql.Date date, int duration, double grade, String difficulty, String status, String file, String link, int course_id) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.duration = duration;
        this.grade = grade;
        this.difficulty = difficulty;
        this.status = status;
        this.file = file;
        this.link = link;
        this.course_id = course_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public java.sql.Date getDate() { return date; }
    public void setDate(java.sql.Date date) { this.date = date; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public double getGrade() { return grade; }
    public void setGrade(double grade) { this.grade = grade; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public int getCourse_id() { return course_id; }
    public void setCourse_id(int course_id) { this.course_id = course_id; }

    @Override
    public String toString() {
        return "Exam{" +
                "id=" + id
 + ", title=" + title
 + ", date=" + date
 + ", duration=" + duration
 + ", grade=" + grade
 + ", difficulty=" + difficulty
 + ", status=" + status
 + ", file=" + file
 + ", link=" + link
 + ", course_id=" + course_id
                + '}';
    }
}
