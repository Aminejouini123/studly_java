public class TestDb {
    public static void main(String[] args) {
        String URL = "jdbc:mysql://localhost:3306/projet_db";
        String USER = "root";
        String PASSWORD = "";
        try {
            java.sql.Connection c = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Success! " + c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
