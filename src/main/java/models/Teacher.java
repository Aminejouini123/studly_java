package models;

/**
 * Teacher role class.
 * Represents a teacher user with course creation and management permissions.
 */
public class Teacher extends Role {

    public Teacher() {
        super("ROLE_TEACHER", "Teacher account with course and content management");
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "VIEW_COURSES",
            "CREATE_COURSES",
            "EDIT_COURSES",
            "DELETE_COURSES",
            "MANAGE_ACTIVITIES",
            "CREATE_EXAMS",
            "EDIT_EXAMS",
            "VIEW_GRADES",
            "MANAGE_GROUPS",
            "UPDATE_PROFILE",
            "VIEW_REPORTS"
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
        return "Teacher";
    }
}

