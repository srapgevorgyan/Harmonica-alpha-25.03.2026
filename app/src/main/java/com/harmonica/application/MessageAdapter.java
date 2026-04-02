package com.harmonica.application;

import android.graphics.Color;
import android.graphics.PorterDuff;
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

    private boolean isIncognito = false;

    public static class Message {
        public String text, sender;
        public boolean isTyping;

        public Message(String t, String s) {
            this.text = t;
            this.sender = s;
            this.isTyping = false;
        }

        public static Message typing() {
            Message m = new Message("", "ai");
            m.isTyping = true;
            return m;
        }
    }

    private List<Message> messages;

    public MessageAdapter(List<Message> messages) { this.messages = messages; }

    public void setIncognito(boolean incognito) {
        this.isIncognito = incognito;
        notifyDataSetChanged();
    }

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
            MsgViewHolder vh = (MsgViewHolder) holder;
            vh.tv.setText(m.text);
            
            if (isIncognito) {
                vh.tv.setTextColor(Color.WHITE);
                if (m.sender.equals("user")) {
                    vh.tv.getBackground().setColorFilter(Color.parseColor("#424242"), PorterDuff.Mode.SRC_IN);
                } else {
                    vh.tv.getBackground().setColorFilter(Color.parseColor("#303030"), PorterDuff.Mode.SRC_IN);
                }
            } else {
                // Reset to default or standard colors if needed
                vh.tv.setTextColor(Color.parseColor("#2D2D2D"));
                // Background colors are handled by XML usually, but if filtered we might need to reset
                vh.tv.getBackground().clearColorFilter();
            }
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
