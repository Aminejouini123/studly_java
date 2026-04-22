package controllers.group;

import models.User;
import services.UserService;
import utils.SessionManager;

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

        return "user #" + userId;
    }

    private static String displayName(User u) {
        String first = u.getFirst_name() == null ? "" : u.getFirst_name().trim();
        String last = u.getLast_name() == null ? "" : u.getLast_name().trim();
        String full = (first + " " + last).trim();
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

