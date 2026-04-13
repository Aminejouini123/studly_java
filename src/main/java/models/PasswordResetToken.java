package models;

public class PasswordResetToken {
    private int id;
    private String token;
    private java.sql.Timestamp created_at;
    private java.sql.Timestamp expires_at;
    private int user_id;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, java.sql.Timestamp created_at, java.sql.Timestamp expires_at, int user_id) {
        this.token = token;
        this.created_at = created_at;
        this.expires_at = expires_at;
        this.user_id = user_id;
    }

    public PasswordResetToken(int id, String token, java.sql.Timestamp created_at, java.sql.Timestamp expires_at, int user_id) {
        this.id = id;
        this.token = token;
        this.created_at = created_at;
        this.expires_at = expires_at;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public java.sql.Timestamp getExpires_at() { return expires_at; }
    public void setExpires_at(java.sql.Timestamp expires_at) { this.expires_at = expires_at; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "PasswordResetToken{" +
                "id=" + id
 + ", token=" + token
 + ", created_at=" + created_at
 + ", expires_at=" + expires_at
 + ", user_id=" + user_id
                + '}';
    }
}
