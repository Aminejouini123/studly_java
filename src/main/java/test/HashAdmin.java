package test;

import org.mindrot.jbcrypt.BCrypt;

public class HashAdmin {
    public static void main(String[] args) {
        String password = "admin";
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println("RESULT_HASH:" + hashed);
    }
}
