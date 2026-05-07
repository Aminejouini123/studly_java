package utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Properties;

public class EmailService {

    private static final Properties config = new Properties();

    static {
        try (InputStream in = EmailService.class.getResourceAsStream("/email.properties")) {
            if (in != null) config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    public static void sendVerificationCode(String toEmail, String code) throws MessagingException {
        String body = "Hello,\n\n"
            + "Your Studly email verification code is:\n\n"
            + "    " + code + "\n\n"
            + "This code expires in 10 minutes. Do not share it with anyone.\n\n"
            + "If you did not request this, please ignore this email.\n\n"
            + "– The Studly Team";
        send(toEmail, "Studly – Email Verification Code", body);
    }

    public static void sendPasswordResetCode(String toEmail, String code) throws MessagingException {
        String body = "Hello,\n\n"
            + "We received a request to reset your Studly password.\n\n"
            + "Your password reset code is:\n\n"
            + "    " + code + "\n\n"
            + "Enter this code in the app to set a new password.\n"
            + "This code expires in 30 minutes.\n\n"
            + "If you did not request a password reset, you can safely ignore this email.\n\n"
            + "– The Studly Team";
        send(toEmail, "Studly – Password Reset Code", body);
    }

    // ---- shared SMTP sender ----

    private static void send(String toEmail, String subject, String body) throws MessagingException {
        String host     = config.getProperty("mail.smtp.host", "smtp.gmail.com");
        String port     = config.getProperty("mail.smtp.port", "587");
        String from     = config.getProperty("mail.from", "");
        String password = config.getProperty("mail.password", "");

        if (from.isEmpty() || from.startsWith("YOUR_") || password.isEmpty() || password.startsWith("YOUR_")) {
            throw new MessagingException(
                "Email credentials not configured. Edit src/main/resources/email.properties.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(from, "Studly", "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(from));
        }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);
        msg.setText(body);
        Transport.send(msg);
    }
}
