package models;

/**
 * Admin role class - Administrator with full permissions
 */
public class Admin extends Role {

    public Admin() {
        super("ROLE_ADMIN", "Administrator account with full access");
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "MANAGE_ALL", "VIEW_ALL", "CREATE_ALL", "EDIT_ALL", "DELETE_ALL"
        };
    }

    @Override
    public boolean hasPermission(String permission) {
        return true; // Admin has all permissions
    }

    @Override
    public String toString() {
        return "Admin";
    }
}
