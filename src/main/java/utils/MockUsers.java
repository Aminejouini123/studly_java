package utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.User;

/**
 * Temporary mock data to unblock Group CRUD while the real User module is not ready.
 * // TODO remplacer par vrai User quand module pret
 */
public final class MockUsers {
    private MockUsers() {
    }

    /**
     * Generates a small set of fake users for UI tests (ComboBox, CRUD screens, etc.).
     * // TODO remplacer par vrai User quand module pret
     */
    public static ObservableList<User> generateMockUsers() {
        ObservableList<User> users = FXCollections.observableArrayList();

        // Requirement: hardcoded fake user
        users.add(createUser(1, "Test User"));

        // A few extra entries make UI testing easier.
        users.add(createUser(2, "Alice Mock"));
        users.add(createUser(3, "Bob Mock"));

        return users;
    }

    public static User createUser(int id, String name) {
        User u = new User();
        u.setId(id);

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            u.setFirstName("User");
            u.setLastName(String.valueOf(id));
            return u;
        }

        int space = trimmed.indexOf(' ');
        if (space < 0) {
            u.setFirstName(trimmed);
            u.setLastName("");
        } else {
            u.setFirstName(trimmed.substring(0, space).trim());
            u.setLastName(trimmed.substring(space + 1).trim());
        }
        return u;
    }

    /**
     * UI label for ComboBox/ListView.
     * // TODO remplacer par vrai User quand module pret
     */
    public static String label(User u) {
        if (u == null) {
            return "";
        }
        String full = u.getFullName().trim();
        if (full.isEmpty()) {
            return String.valueOf(u.getId());
        }
        return u.getId() + " - " + full;
    }
}

