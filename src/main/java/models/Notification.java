package models;

public class Notification {
    private int id;
    private String content;
    private String link;
    private int is_read;
    private java.sql.Timestamp created_at;
    private int user_id;

    public Notification() {}

    public Notification(String content, String link, int is_read, java.sql.Timestamp created_at, int user_id) {
        this.content = content;
        this.link = link;
        this.is_read = is_read;
        this.created_at = created_at;
        this.user_id = user_id;
    }

    public Notification(int id, String content, String link, int is_read, java.sql.Timestamp created_at, int user_id) {
        this.id = id;
        this.content = content;
        this.link = link;
        this.is_read = is_read;
        this.created_at = created_at;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public int getIs_read() { return is_read; }
    public void setIs_read(int is_read) { this.is_read = is_read; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id
 + ", content=" + content
 + ", link=" + link
 + ", is_read=" + is_read
 + ", created_at=" + created_at
 + ", user_id=" + user_id
                + '}';
    }
}
