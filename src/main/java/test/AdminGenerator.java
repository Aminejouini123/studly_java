package test;

import models.User;
import services.UserService;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

public class AdminGenerator {
    public static void main(String[] args) {
        try {
            UserService userService = new UserService();
            
            User admin = new User();
            admin.setEmail("admin@studly.com");
            admin.setPassword("admin123");
            admin.setRoles("[\"ROLE_ADMIN\"]");
            admin.setFirst_name("System");
            admin.setLast_name("Admin");
            admin.setStatut("Active");
            admin.setIs_verified(1);
            admin.setDate_of_birth(Date.valueOf(LocalDate.of(2000, 1, 1)));
            admin.setCreated_at(new Timestamp(System.currentTimeMillis()));
            admin.setUpdated_at(new Timestamp(System.currentTimeMillis()));
            
            userService.ajouter(admin);
            
            System.out.println("✅ Admin account successfully created:");
            System.out.println("Email: admin@studly.com");
            System.out.println("Password: admin123");
        } catch (Exception e) {
            System.err.println("❌ Failed to create admin account:");
            e.printStackTrace();
        }
    }
}
