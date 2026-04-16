package test;

import models.Personne;
import services.PersonneService;

import java.sql.SQLException;

public class Main {


    public static void main(String[] args) {
        utils.DatabaseInitializer.initialize("schema.sql");
        services.UserService us = new services.UserService();
        try {
            for (models.User u : us.recuperer()) {
                if ("admin@studly.com".equals(u.getEmail())) {
                    u.setPassword("admin");
                    us.modifier(u);
                    System.out.println("Password reset to 'admin' for admin@studly.com");
                    break;
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
