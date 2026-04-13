package models;

public class Activity {
    private int id;
    private String title;
    private String description;
    private String file;
    private String link;
    private int duration;
    private String status;
    private String difficulty;
    private String level;
    private String type;
    private String instructions;
    private String expected_output;
    private String hints;
    private java.sql.Timestamp completed_at;
    private int course_id;
    private int assigned_user_id;

    public Activity() {}

    public Activity(String title, String description, String file, String link, int duration, String status, String difficulty, String level, String type, String instructions, String expected_output, String hints, java.sql.Timestamp completed_at, int course_id, int assigned_user_id) {
        this.title = title;
        this.description = description;
        this.file = file;
        this.link = link;
        this.duration = duration;
        this.status = status;
        this.difficulty = difficulty;
        this.level = level;
        this.type = type;
        this.instructions = instructions;
        this.expected_output = expected_output;
        this.hints = hints;
        this.completed_at = completed_at;
        this.course_id = course_id;
        this.assigned_user_id = assigned_user_id;
    }

    public Activity(int id, String title, String description, String file, String link, int duration, String status, String difficulty, String level, String type, String instructions, String expected_output, String hints, java.sql.Timestamp completed_at, int course_id, int assigned_user_id) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.file = file;
        this.link = link;
        this.duration = duration;
        this.status = status;
        this.difficulty = difficulty;
        this.level = level;
        this.type = type;
        this.instructions = instructions;
        this.expected_output = expected_output;
        this.hints = hints;
        this.completed_at = completed_at;
        this.course_id = course_id;
        this.assigned_user_id = assigned_user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getExpected_output() { return expected_output; }
    public void setExpected_output(String expected_output) { this.expected_output = expected_output; }
    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }
    public java.sql.Timestamp getCompleted_at() { return completed_at; }
    public void setCompleted_at(java.sql.Timestamp completed_at) { this.completed_at = completed_at; }
    public int getCourse_id() { return course_id; }
    public void setCourse_id(int course_id) { this.course_id = course_id; }
    public int getAssigned_user_id() { return assigned_user_id; }
    public void setAssigned_user_id(int assigned_user_id) { this.assigned_user_id = assigned_user_id; }

    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id
 + ", title=" + title
 + ", description=" + description
 + ", file=" + file
 + ", link=" + link
 + ", duration=" + duration
 + ", status=" + status
 + ", difficulty=" + difficulty
 + ", level=" + level
 + ", type=" + type
 + ", instructions=" + instructions
 + ", expected_output=" + expected_output
 + ", hints=" + hints
 + ", completed_at=" + completed_at
 + ", course_id=" + course_id
 + ", assigned_user_id=" + assigned_user_id
                + '}';
    }
}
