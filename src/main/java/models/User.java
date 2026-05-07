package models;

import java.sql.Date;
import java.sql.Timestamp;

public class User {
    private int id;
    private String googleId;
    private int isVerified;
    private String verificationCode;
    private String email;
    private Role role;
    private String password;
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
    private String phoneNumber;
    private String address;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String statut;
    private String profilePicture;
    private String educationLevel;
    private String jobTitle;
    private String website;
    private String bio;
    private String skills;
    private int score;
    private String googleAccessToken;
    private String googleRefreshToken;
    private Timestamp googleTokenExpiresAt;
    private String banReason;
    private String githubId;

    public User() {}

    public User(int id, String googleId, int isVerified, String verificationCode, String email, Role role, 
                String password, String firstName, String lastName, Date dateOfBirth, String phoneNumber, 
                String address, Timestamp createdAt, Timestamp updatedAt, String statut, String profilePicture, 
                String educationLevel, String jobTitle, String website, String bio, String skills, int score, 
                String googleAccessToken, String googleRefreshToken, Timestamp googleTokenExpiresAt) {
        this.id = id;
        this.googleId = googleId;
        this.isVerified = isVerified;
        this.verificationCode = verificationCode;
        this.email = email;
        this.role = role;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statut = statut;
        this.profilePicture = profilePicture;
        this.educationLevel = educationLevel;
        this.jobTitle = jobTitle;
        this.website = website;
        this.bio = bio;
        this.skills = skills;
        this.score = score;
        this.googleAccessToken = googleAccessToken;
        this.googleRefreshToken = googleRefreshToken;
        this.googleTokenExpiresAt = googleTokenExpiresAt;
    }

    // Business Logic Methods (The "Rich" part of the model)

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getInitials() {
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) initials += firstName.substring(0, 1).toUpperCase();
        if (lastName != null && !lastName.isEmpty()) initials += lastName.substring(0, 1).toUpperCase();
        return initials;
    }

    public boolean isAdmin() {
        return role instanceof Admin || (getRoles() != null && getRoles().contains("ROLE_ADMIN"));
    }

    public boolean isTeacher() {
        return role instanceof Teacher || (getRoles() != null && getRoles().contains("ROLE_TEACHER"));
    }

    public boolean isVerifiedAccount() {
        return isVerified == 1;
    }

    public boolean hasPermission(String permission) {
        return role != null && role.hasPermission(permission);
    }

    /**
     * Converts JSON-style skills string ["a","b"] to human-readable "a, b"
     */
    public String getFormattedSkills() {
        if (skills != null && skills.startsWith("[") && skills.endsWith("]")) {
            return skills.replace("[", "").replace("]", "").replace("\"", "").replace(",", ", ");
        }
        return skills != null ? skills : "";
    }

    /**
     * Converts comma-separated string "a, b" to JSON-style ["a","b"]
     */
    public void setFormattedSkills(String input) {
        if (input == null || input.trim().isEmpty()) {
            this.skills = "[]";
            return;
        }
        String[] parts = input.split(",");
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            json.append("\"").append(parts[i].trim()).append("\"");
            if (i < parts.length - 1) json.append(",");
        }
        json.append("]");
        this.skills = json.toString();
    }

    // Getters and Setters (standard camelCase)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public int getIsVerified() { return isVerified; }
    public void setIsVerified(int isVerified) { this.isVerified = isVerified; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    /** Backward compatibility for DB/Services */
    public String getRoles() {
        return role != null ? role.getRoleName() : "ROLE_USER";
    }

    /** Backward compatibility for DB/Services */
    public void setRoles(String roleString) {
        if (roleString != null) {
            if (roleString.contains("ADMIN")) this.role = new Admin();
            else if (roleString.contains("TEACHER")) this.role = new Teacher();
            else this.role = new Student();
        } else {
            this.role = new Student();
        }
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getGoogleAccessToken() { return googleAccessToken; }
    public void setGoogleAccessToken(String googleAccessToken) { this.googleAccessToken = googleAccessToken; }

    public String getGoogleRefreshToken() { return googleRefreshToken; }
    public void setGoogleRefreshToken(String googleRefreshToken) { this.googleRefreshToken = googleRefreshToken; }

    public Timestamp getGoogleTokenExpiresAt() { return googleTokenExpiresAt; }
    public void setGoogleTokenExpiresAt(Timestamp googleTokenExpiresAt) { this.googleTokenExpiresAt = googleTokenExpiresAt; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public String getGithubId() { return githubId; }
    public void setGithubId(String githubId) { this.githubId = githubId; }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", email='" + email + '\'' + ", role=" + role + '}';
    }
}
