package com.harmonica.application;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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
        public boolean isAnimated = false; // Prevents re-animating when scrolling back

        public Message(String t, String s) {
            this.text = t;
            this.sender = s;
            this.isTyping = false;
        }

        public static Message typing() {
            Message m = new Message("", "ai");
            m.isTyping = true;
            m.isAnimated = true;
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

        if (holder instanceof AiMsgViewHolder) {
            AiMsgViewHolder vh = (AiMsgViewHolder) holder;
            applyStyle(vh.tv, false);

            // Cancel any animation currently running on this view holder (recycled)
            if (vh.tv.getTag() instanceof Runnable) {
                vh.handler.removeCallbacks((Runnable) vh.tv.getTag());
                vh.tv.setTag(null);
            }

            if (!m.isAnimated && !m.isTyping && m.sender.equals("ai")) {
                animateTypewriter(vh, m);
            } else {
                vh.tv.setText(parseMarkdown(m.text));
                setupPracticeButton(vh, m);
            }
        } else if (holder instanceof MsgViewHolder) {
            MsgViewHolder vh = (MsgViewHolder) holder;
            vh.tv.setText(m.text);
            applyStyle(vh.tv, m.sender.equals("user"));
        }
    }

    private void setupPracticeButton(AiMsgViewHolder vh, Message m) {
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

    private void animateTypewriter(AiMsgViewHolder vh, Message message) {
        final String fullText = message.text;
        vh.tv.setText("");
        vh.btnPractice.setVisibility(View.GONE);

        final int[] index = {0};
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Ensure the view holder is still displaying THIS runnable
                if (vh.tv.getTag() != this) return;

                if (index[0] <= fullText.length()) {
                    String partial = fullText.substring(0, index[0]);
                    vh.tv.setText(parseMarkdown(partial));
                    index[0]++;

                    // Auto-scroll logic: only if the user is at the bottom
                    autoScrollIfAtBottom(vh);

                    vh.handler.postDelayed(this, 12); // Smooth streaming speed
                } else {
                    message.isAnimated = true;
                    vh.tv.setTag(null);
                    setupPracticeButton(vh, message);
                }
            }
        };
        
        vh.tv.setTag(runnable);
        vh.handler.post(runnable);
    }

    private void autoScrollIfAtBottom(AiMsgViewHolder vh) {
        if (vh.itemView.getParent() instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) vh.itemView.getParent();
            LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
            if (layoutManager != null) {
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                // If we are typing the last item, keep it in view
                if (lastVisible >= getItemCount() - 2) {
                    rv.scrollToPosition(getItemCount() - 1);
                }
            }
        }
    }

    private CharSequence parseMarkdown(String text) {
        if (text == null) return "";
        
        // Escape basic HTML
        String processed = text.replace("&", "&amp;")
                             .replace("<", "&lt;")
                             .replace(">", "&gt;");
                             
        // Bold: **text** -> <b>text</b>
        processed = processed.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        
        // Italics: *text* -> <i>text</i>
        processed = processed.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
        
        // Bullet points: lines starting with "- " or "* "
        processed = processed.replaceAll("(?m)^[-*] ", "• ");
        
        // Headers (Simple bolding)
        processed = processed.replaceAll("(?m)^### (.*)$", "<b>$1</b>");
        processed = processed.replaceAll("(?m)^## (.*)$", "<b>$1</b>");
        processed = processed.replaceAll("(?m)^# (.*)$", "<b>$1</b>");
        
        // Line breaks
        processed = processed.replace("\n", "<br>");
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(processed, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(processed);
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
        Handler handler = new Handler(Looper.getMainLooper());
        AiMsgViewHolder(View v) {
            super(v);
            btnPractice = v.findViewById(R.id.btnStartPractice);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(View v) { super(v); }
    }
}
