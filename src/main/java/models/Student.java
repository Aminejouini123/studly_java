package models;

/**
 * Student role class.
 * Represents a student user with limited permissions.
 */
public class Student extends Role {

    public Student() {
        super("ROLE_STUDENT", "Student account with learning access");
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "VIEW_COURSES",
            "SUBMIT_ACTIVITIES",
            "VIEW_GRADES",
            "VIEW_EXAMS",
            "UPDATE_PROFILE",
            "VIEW_GROUPS",
            "JOIN_GROUPS"
        };
    }

    @Override
    public boolean hasPermission(String permission) {
        String[] permissions = getPermissions();
        for (String p : permissions) {
            if (p.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Student";
    }
}

