package test;

import utils.MyDatabase;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateAdmin {
    public static void main(String[] args) {
        String email = "superadmin@studly.com";
        String roles = "[\"ROLE_ADMIN\"]";
        String password = "$2a$12$uR/haoK2Bq.W21dXBcwdrevnRBW2VACmXnmD9xIvFPl4ZafLolSk2"; // password: admin
        
        String sql = "INSERT INTO users (email, roles, password, first_name, last_name, is_verified) " +
                     "VALUES (?, ?, ?, 'Super', 'Admin', 1) " +
                     "ON DUPLICATE KEY UPDATE password = ?";
                     
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, roles);
            ps.setString(3, password);
            ps.setString(4, password);
            ps.executeUpdate();
            System.out.println("✅ Admin account created/updated successfully!");
            System.out.println("Email: " + email);
            System.out.println("Password: admin");
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
}
