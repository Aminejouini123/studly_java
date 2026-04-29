package services;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal Twilio SMS sender.
 *
 * Configuration is read from environment variables:
 * - TWILIO_ACCOUNT_SID
 * - TWILIO_AUTH_TOKEN
 * - TWILIO_FROM_NUMBER (E.164, e.g. +15005550006)
 */
public final class SmsService {
    private static final Logger LOG = Logger.getLogger(SmsService.class.getName());

    public static final String ENV_ACCOUNT_SID = "TWILIO_ACCOUNT_SID";
    public static final String ENV_AUTH_TOKEN = "TWILIO_AUTH_TOKEN";
    public static final String ENV_FROM_NUMBER = "TWILIO_FROM_NUMBER";

    // E.164: + followed by 2..15 digits (first digit 1..9)
    private static final String E164_REGEX = "^\\+[1-9]\\d{1,14}$";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private volatile boolean initialized;

    public SmsService() {
        this(System.getenv(ENV_ACCOUNT_SID), System.getenv(ENV_AUTH_TOKEN), System.getenv(ENV_FROM_NUMBER));
    }

    public SmsService(String accountSid, String authToken, String fromNumber) {
        this.accountSid = trimToNull(accountSid);
        this.authToken = trimToNull(authToken);
        this.fromNumber = trimToNull(fromNumber);
    }

    public Result sendGroupInvitationSms(String toPhoneNumber, String groupName) {
        String to = normalizeE164Candidate(toPhoneNumber);
        if (to == null) {
            return Result.skipped("Numero de telephone vide.");
        }
        if (!isValidE164(to)) {
            return Result.skipped("Numero invalide (format attendu: E.164, ex: +33612345678).");
        }

        String g = trimToNull(groupName);
        if (g == null) g = "votre groupe";

        String body = "Vous avez ete invite a rejoindre le groupe " + g + ". "
                + "Connectez-vous a votre compte et acceptez cette invitation si vous le souhaitez.";

        try {
            ensureInitialized();
            if (!isConfigured()) {
                return Result.skipped("Twilio non configure (variables d'environnement manquantes).");
            }

            String from = normalizeE164Candidate(fromNumber);
            if (from == null || !isValidE164(from)) {
                return Result.skipped("Numero expéditeur Twilio invalide (format attendu: E.164, ex: +15005550006).");
            }

            Message message = Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
            return Result.sent(message.getSid());
        } catch (ApiException e) {
            // Twilio API errors (invalid credentials, from number, etc.)
            LOG.log(Level.WARNING, "Twilio API error while sending SMS: " + e.getMessage(), e);
            return Result.failed("Erreur Twilio: " + safeMessage(e));
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Unexpected error while sending SMS", e);
            return Result.failed("Erreur SMS: " + safeMessage(e));
        }
    }

    private void ensureInitialized() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            if (accountSid != null && authToken != null) {
                Twilio.init(accountSid, authToken);
            }
            initialized = true;
        }
    }

    private boolean isConfigured() {
        String from = normalizeE164Candidate(fromNumber);
        return accountSid != null && authToken != null && from != null && isValidE164(from);
    }

    private static boolean isValidE164(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches(E164_REGEX);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Accepts "human" phone formats like "+1 229 633 7456" and normalizes them to an E.164 candidate.
     * We intentionally only strip common separators; we don't try to infer missing country codes.
     */
    private static String normalizeE164Candidate(String value) {
        String t = trimToNull(value);
        if (t == null) return null;
        // Remove spaces and common formatting characters.
        return t.replace(" ", "")
                .replace("\t", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "");
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return Objects.toString(m, t.getClass().getSimpleName());
    }

    public static final class Result {
        private final Status status;
        private final String messageSid;
        private final String error;

        private Result(Status status, String messageSid, String error) {
            this.status = status;
            this.messageSid = messageSid;
            this.error = error;
        }

        public static Result sent(String messageSid) {
            return new Result(Status.SENT, messageSid, null);
        }

        public static Result skipped(String reason) {
            return new Result(Status.SKIPPED, null, reason);
        }

        public static Result failed(String reason) {
            return new Result(Status.FAILED, null, reason);
        }

        public Status getStatus() {
            return status;
        }

        public boolean isSent() {
            return status == Status.SENT;
        }

        public String getMessageSid() {
            return messageSid;
        }

        public String getError() {
            return error;
        }

        public enum Status {
            SENT,
            SKIPPED,
            FAILED
        }
    }
}
