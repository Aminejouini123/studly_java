package test;

import models.User;
import services.UserService;
import utils.PasswordUtil;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

public class AdminGenerator {
    public static void main(String[] args) {
        try {
            UserService userService = new UserService();
            
            User admin = new User();
            admin.setEmail("admin@studly.com");
            admin.setPassword(PasswordUtil.hash("admin123"));
            admin.setRoles("[\"ROLE_ADMIN\"]");
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setStatut("Active");
            admin.setIsVerified(1);
            admin.setDateOfBirth(Date.valueOf(LocalDate.of(2000, 1, 1)));
            admin.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            admin.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            
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
