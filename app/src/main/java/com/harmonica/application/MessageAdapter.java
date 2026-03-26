package com.harmonica.application;
//person and Ai chat bubbles

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MsgViewHolder> {

    public static class Message {
        public String text, sender;
        public Message(String t, String s) { this.text = t; this.sender = s; }
    }

    private List<Message> messages;

    public MessageAdapter(List<Message> messages) { this.messages = messages; }

    @NonNull
    @Override
    public MsgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new MsgViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgViewHolder holder, int position) {
        Message m = messages.get(position);
        holder.tv.setText(m.text);

        // Safer way to handle alignment
        if (m.sender.equals("user")) {
            holder.tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            holder.tv.setTextColor(android.graphics.Color.BLACK);
        } else {
            holder.tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            holder.tv.setTextColor(android.graphics.Color.BLACK);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MsgViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        MsgViewHolder(View v) { super(v); tv = v.findViewById(android.R.id.text1); }
    }
}
