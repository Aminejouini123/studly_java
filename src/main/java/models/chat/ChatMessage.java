package models.chat;

public final class ChatMessage {
    public enum Role { USER, AI }

    private final Role role;
    private final String content;

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content == null ? "" : content;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}

