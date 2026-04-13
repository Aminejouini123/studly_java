package models;

public class PomodoroSession {
    private int id;
    private String type;
    private int duration;
    private String status;
    private java.sql.Timestamp started_at;
    private java.sql.Timestamp ended_at;
    private double focus_score;
    private String focus_logs;
    private int event_id;

    public PomodoroSession() {}

    public PomodoroSession(String type, int duration, String status, java.sql.Timestamp started_at, java.sql.Timestamp ended_at, double focus_score, String focus_logs, int event_id) {
        this.type = type;
        this.duration = duration;
        this.status = status;
        this.started_at = started_at;
        this.ended_at = ended_at;
        this.focus_score = focus_score;
        this.focus_logs = focus_logs;
        this.event_id = event_id;
    }

    public PomodoroSession(int id, String type, int duration, String status, java.sql.Timestamp started_at, java.sql.Timestamp ended_at, double focus_score, String focus_logs, int event_id) {
        this.id = id;
        this.type = type;
        this.duration = duration;
        this.status = status;
        this.started_at = started_at;
        this.ended_at = ended_at;
        this.focus_score = focus_score;
        this.focus_logs = focus_logs;
        this.event_id = event_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.sql.Timestamp getStarted_at() { return started_at; }
    public void setStarted_at(java.sql.Timestamp started_at) { this.started_at = started_at; }
    public java.sql.Timestamp getEnded_at() { return ended_at; }
    public void setEnded_at(java.sql.Timestamp ended_at) { this.ended_at = ended_at; }
    public double getFocus_score() { return focus_score; }
    public void setFocus_score(double focus_score) { this.focus_score = focus_score; }
    public String getFocus_logs() { return focus_logs; }
    public void setFocus_logs(String focus_logs) { this.focus_logs = focus_logs; }
    public int getEvent_id() { return event_id; }
    public void setEvent_id(int event_id) { this.event_id = event_id; }

    @Override
    public String toString() {
        return "PomodoroSession{" +
                "id=" + id
 + ", type=" + type
 + ", duration=" + duration
 + ", status=" + status
 + ", started_at=" + started_at
 + ", ended_at=" + ended_at
 + ", focus_score=" + focus_score
 + ", focus_logs=" + focus_logs
 + ", event_id=" + event_id
                + '}';
    }
}
