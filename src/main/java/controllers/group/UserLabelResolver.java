package controllers.group;

import models.User;
import services.UserService;
import utils.MyDatabase;
import utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a user id into a human-friendly label (e.g. "First Last").
 * Uses SessionManager for the current user and a simple in-memory cache to avoid repeated DB scans.
 */
final class UserLabelResolver {
    private final UserService userService;
    private final Map<Integer, String> cache = new HashMap<>();
    private boolean loadedAll;

    UserLabelResolver(UserService userService) {
        this.userService = userService;
    }

    String resolve(int userId) {
        if (userId <= 0) {
            return "Utilisateur";
        }

        User current = SessionManager.getCurrentUser();
        if (current != null && current.getId() == userId) {
            return displayName(current);
        }

        String cached = cache.get(userId);
        if (cached != null) {
            return cached;
        }

        // Load all users once and fill cache; simplest approach given current UserService API.
        if (!loadedAll) {
            try {
                List<User> users = userService.recuperer();
                for (User u : users) {
                    if (u != null) {
                        cache.put(u.getId(), displayName(u));
                    }
                }
            } catch (SQLException | RuntimeException ignored) {
                // Keep fallback.
            } finally {
                loadedAll = true;
            }
        }

        cached = cache.get(userId);
        if (cached != null) {
            return cached;
        }

        String crossTable = resolveFromLegacyTables(userId);
        if (crossTable != null) {
            cache.put(userId, crossTable);
            return crossTable;
        }

        return "user #" + userId;
    }

    private String resolveFromLegacyTables(int userId) {
        Connection c = MyDatabase.getInstance().getConnection();
        if (c == null) {
            return null;
        }

        String fromUsers = queryDisplayName(c, "users", userId);
        if (fromUsers != null) {
            return fromUsers;
        }
        return queryDisplayName(c, "user", userId);
    }

    private static String queryDisplayName(Connection c, String table, int userId) {
        String sql = "select first_name, last_name, email from `" + table + "` where id = ? limit 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String first = rs.getString("first_name");
                String last = rs.getString("last_name");
                String email = rs.getString("email");
                String full = ((first == null ? "" : first.trim()) + " " + (last == null ? "" : last.trim())).trim();
                if (!full.isEmpty()) {
                    return full;
                }
                if (email != null && !email.trim().isEmpty()) {
                    return email.trim().toLowerCase(Locale.ROOT);
                }
                return null;
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private static String displayName(User u) {
        String full = u.getFullName().trim();
        if (!full.isEmpty()) {
            return full;
        }

        String email = u.getEmail() == null ? "" : u.getEmail().trim();
        if (!email.isEmpty()) {
            return email.toLowerCase(Locale.ROOT);
        }

        return "Utilisateur";
    }
}

