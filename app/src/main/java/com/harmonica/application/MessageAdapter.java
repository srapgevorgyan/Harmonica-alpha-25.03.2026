package com.harmonica.application;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
        public GeminiService.Practice suggestedPractice;

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
            return new AiMsgViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = messages.get(position);

        if (holder instanceof MsgViewHolder) {
            MsgViewHolder vh = (MsgViewHolder) holder;
            vh.tv.setText(m.text);
            applyStyle(vh.tv, m.sender.equals("user"));
        } else if (holder instanceof AiMsgViewHolder) {
            AiMsgViewHolder vh = (AiMsgViewHolder) holder;
            vh.tv.setText(m.text);
            applyStyle(vh.tv, false);

            if (m.suggestedPractice != null) {
                vh.btnPractice.setVisibility(View.VISIBLE);
                vh.btnPractice.setText("Start " + m.suggestedPractice.title);
                vh.btnPractice.setOnClickListener(v -> {
                    if (v.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) v.getContext();
                        activity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, ZenSpaceFragment.newInstance(
                                        m.suggestedPractice.type,
                                        m.suggestedPractice.title,
                                        m.suggestedPractice.instruction
                                ))
                                .addToBackStack(null)
                                .commit();
                    }
                });
            } else {
                vh.btnPractice.setVisibility(View.GONE);
            }
        }
    }

    private void applyStyle(TextView tv, boolean isUser) {
        if (isIncognito) {
            tv.setTextColor(Color.WHITE);
            int color = isUser ? Color.parseColor("#424242") : Color.parseColor("#303030");
            tv.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        } else {
            tv.setTextColor(Color.parseColor("#2D2D2D"));
            tv.getBackground().clearColorFilter();
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MsgViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        MsgViewHolder(View v) { super(v); tv = v.findViewById(R.id.txtMessage); }
    }

    static class AiMsgViewHolder extends MsgViewHolder {
        Button btnPractice;
        AiMsgViewHolder(View v) {
            super(v);
            btnPractice = v.findViewById(R.id.btnStartPractice);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View v) { super(v); }
    }
}
