package models;

public class User {
    private int id;
    private String google_id;
    private int is_verified;
    private String verification_code;
    private String email;
    private String roles;
    private String password;
    private String first_name;
    private String last_name;
    private java.sql.Date date_of_birth;
    private String phone_number;
    private String address;
    private java.sql.Timestamp created_at;
    private java.sql.Timestamp updated_at;
    private String statut;
    private String profile_picture;
    private String education_level;
    private String job_title;
    private String website;
    private String bio;
    private String skills;
    private int score;
    private String google_access_token;
    private String google_refresh_token;
    private java.sql.Timestamp google_token_expires_at;

    public User() {}

    public User(String google_id, int is_verified, String verification_code, String email, String roles, String password, String first_name, String last_name, java.sql.Date date_of_birth, String phone_number, String address, java.sql.Timestamp created_at, java.sql.Timestamp updated_at, String statut, String profile_picture, String education_level, String job_title, String website, String bio, String skills, int score, String google_access_token, String google_refresh_token, java.sql.Timestamp google_token_expires_at) {
        this.google_id = google_id;
        this.is_verified = is_verified;
        this.verification_code = verification_code;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.first_name = first_name;
        this.last_name = last_name;
        this.date_of_birth = date_of_birth;
        this.phone_number = phone_number;
        this.address = address;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.statut = statut;
        this.profile_picture = profile_picture;
        this.education_level = education_level;
        this.job_title = job_title;
        this.website = website;
        this.bio = bio;
        this.skills = skills;
        this.score = score;
        this.google_access_token = google_access_token;
        this.google_refresh_token = google_refresh_token;
        this.google_token_expires_at = google_token_expires_at;
    }

    public User(int id, String google_id, int is_verified, String verification_code, String email, String roles, String password, String first_name, String last_name, java.sql.Date date_of_birth, String phone_number, String address, java.sql.Timestamp created_at, java.sql.Timestamp updated_at, String statut, String profile_picture, String education_level, String job_title, String website, String bio, String skills, int score, String google_access_token, String google_refresh_token, java.sql.Timestamp google_token_expires_at) {
        this.id = id;
        this.google_id = google_id;
        this.is_verified = is_verified;
        this.verification_code = verification_code;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.first_name = first_name;
        this.last_name = last_name;
        this.date_of_birth = date_of_birth;
        this.phone_number = phone_number;
        this.address = address;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.statut = statut;
        this.profile_picture = profile_picture;
        this.education_level = education_level;
        this.job_title = job_title;
        this.website = website;
        this.bio = bio;
        this.skills = skills;
        this.score = score;
        this.google_access_token = google_access_token;
        this.google_refresh_token = google_refresh_token;
        this.google_token_expires_at = google_token_expires_at;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getGoogle_id() { return google_id; }
    public void setGoogle_id(String google_id) { this.google_id = google_id; }
    public int getIs_verified() { return is_verified; }
    public void setIs_verified(int is_verified) { this.is_verified = is_verified; }
    public String getVerification_code() { return verification_code; }
    public void setVerification_code(String verification_code) { this.verification_code = verification_code; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirst_name() { return first_name; }
    public void setFirst_name(String first_name) { this.first_name = first_name; }
    public String getLast_name() { return last_name; }
    public void setLast_name(String last_name) { this.last_name = last_name; }
    public java.sql.Date getDate_of_birth() { return date_of_birth; }
    public void setDate_of_birth(java.sql.Date date_of_birth) { this.date_of_birth = date_of_birth; }
    public String getPhone_number() { return phone_number; }
    public void setPhone_number(String phone_number) { this.phone_number = phone_number; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public java.sql.Timestamp getUpdated_at() { return updated_at; }
    public void setUpdated_at(java.sql.Timestamp updated_at) { this.updated_at = updated_at; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getProfile_picture() { return profile_picture; }
    public void setProfile_picture(String profile_picture) { this.profile_picture = profile_picture; }
    public String getEducation_level() { return education_level; }
    public void setEducation_level(String education_level) { this.education_level = education_level; }
    public String getJob_title() { return job_title; }
    public void setJob_title(String job_title) { this.job_title = job_title; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getGoogle_access_token() { return google_access_token; }
    public void setGoogle_access_token(String google_access_token) { this.google_access_token = google_access_token; }
    public String getGoogle_refresh_token() { return google_refresh_token; }
    public void setGoogle_refresh_token(String google_refresh_token) { this.google_refresh_token = google_refresh_token; }
    public java.sql.Timestamp getGoogle_token_expires_at() { return google_token_expires_at; }
    public void setGoogle_token_expires_at(java.sql.Timestamp google_token_expires_at) { this.google_token_expires_at = google_token_expires_at; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id
 + ", google_id=" + google_id
 + ", is_verified=" + is_verified
 + ", verification_code=" + verification_code
 + ", email=" + email
 + ", roles=" + roles
 + ", password=" + password
 + ", first_name=" + first_name
 + ", last_name=" + last_name
 + ", date_of_birth=" + date_of_birth
 + ", phone_number=" + phone_number
 + ", address=" + address
 + ", created_at=" + created_at
 + ", updated_at=" + updated_at
 + ", statut=" + statut
 + ", profile_picture=" + profile_picture
 + ", education_level=" + education_level
 + ", job_title=" + job_title
 + ", website=" + website
 + ", bio=" + bio
 + ", skills=" + skills
 + ", score=" + score
 + ", google_access_token=" + google_access_token
 + ", google_refresh_token=" + google_refresh_token
 + ", google_token_expires_at=" + google_token_expires_at
                + '}';
    }
}
