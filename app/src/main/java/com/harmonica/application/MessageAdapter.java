package com.harmonica.application;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;
    private static final int TYPE_TYPING = 3;

    public static class Message {
        public String text, sender;
        public boolean isTyping;

        public Message(String t, String s) {
            this.text = t;
            this.sender = s;
            this.isTyping = false;
        }

        // Helper for typing indicator
        public static Message typing() {
            Message m = new Message("", "ai");
            m.isTyping = true;
            return m;
        }
    }

    private List<Message> messages;

    public MessageAdapter(List<Message> messages) { this.messages = messages; }

    @Override
    public int getItemViewType(int position) {
        Message m = messages.get(position);
        if (m.isTyping) return TYPE_TYPING;
        return m.sender.equals("user") ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new MsgViewHolder(v);
        } else if (viewType == TYPE_TYPING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_typing, parent, false);
            return new TypingViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_ai, parent, false);
            return new MsgViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MsgViewHolder) {
            Message m = messages.get(position);
            ((MsgViewHolder) holder).tv.setText(m.text);
            // Text is black by default in the XML I'll provide below
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MsgViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        MsgViewHolder(View v) { super(v); tv = v.findViewById(R.id.txtMessage); }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View v) { super(v); }
    }
}