package com.harmonica.application;

public class Message {
    private String text;
    private boolean isUser;
    private boolean isTyping;

    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.isTyping = false;
    }


    public static Message typingIndicator() {
        Message msg = new Message("", false);
        msg.isTyping = true;
        return msg;
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public boolean isTyping() { return isTyping; }
}