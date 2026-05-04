package com.spendlens.app.ai;

public class AiChatMessage {

    public final String role;
    public final String content;

    public AiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
