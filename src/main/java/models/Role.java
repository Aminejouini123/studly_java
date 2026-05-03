package models;

/**
 * Abstract base class for user roles.
 * Represents different roles that a user can have in the system.
 */
public abstract class Role {
    protected String roleName;
    protected String description;

    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Abstract method to get role permissions
     */
    public abstract String[] getPermissions();

    /**
     * Abstract method to check if role has specific permission
     */
    public abstract boolean hasPermission(String permission);

    @Override
    public String toString() {
        return roleName;
    }
}

