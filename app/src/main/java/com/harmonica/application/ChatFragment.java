package com.harmonica.application;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<MessageAdapter.Message> messageList = new ArrayList<>();
    private EditText editInput;
    private MoodDatabase db;
    private long sessionId = -1;
    private GeminiService gemini;
    private LinearLayout layoutWelcome;
    private ImageButton btnSend;
    private View root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_chat, container, false);

        db = new MoodDatabase(getContext());
        gemini = new GeminiService();

        // Retrieve sessionId from arguments or create a temporary one
        if (getArguments() != null) sessionId = getArguments().getLong("sessionId", -1);
        if (sessionId == -1) sessionId = db.createSession("New Conversation...");

        recyclerView = root.findViewById(R.id.chatRecyclerView);
        editInput = root.findViewById(R.id.editMoodInput);
        btnSend = root.findViewById(R.id.btnSend);
        layoutWelcome = root.findViewById(R.id.layoutWelcome);

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        applyIncognitoStyling();
        loadHistory();
        btnSend.setOnClickListener(view -> sendMessage());

        return root;
    }

    private void applyIncognitoStyling() {
        boolean isIncognito = (sessionId == -2);
        adapter.setIncognito(isIncognito);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setIncognitoMode(isIncognito);
        }

        if (isIncognito) {
            root.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.incognito_bg));
            recyclerView.setBackgroundColor(Color.TRANSPARENT);
            
            View inputContainer = root.findViewById(R.id.layout_input_container);
            if (inputContainer != null) {
                inputContainer.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_input_bar_incognito));
            }

            editInput.setTextColor(ContextCompat.getColor(getContext(), R.color.incognito_text));
            editInput.setHintTextColor(ContextCompat.getColor(getContext(), R.color.incognito_hint));
            
            // Fix Send Button for Incognito
            btnSend.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.incognito_primary), PorterDuff.Mode.SRC_IN);
            btnSend.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

            // Update Welcome Text for Incognito
            if (layoutWelcome != null) {
                TextView title = (TextView) layoutWelcome.getChildAt(1);
                TextView sub = (TextView) layoutWelcome.getChildAt(2);
                title.setText("Incognito Space");
                title.setTextColor(ContextCompat.getColor(getContext(), R.color.incognito_primary));
                sub.setText("Your messages won't be saved in your history. Talk freely.");
                sub.setTextColor(ContextCompat.getColor(getContext(), R.color.incognito_hint));
            }
        } else {
            // Standard Styling Reset
            btnSend.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.harmonica_primary), PorterDuff.Mode.SRC_IN);
            btnSend.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
    }

    private void updateWelcomeVisibility() {
        if (layoutWelcome != null) {
            layoutWelcome.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void loadHistory() {
        if (sessionId == -2) {
            messageList.clear();
            adapter.notifyDataSetChanged();
            updateWelcomeVisibility();
            return;
        }
        android.database.Cursor cursor = db.getMessages(sessionId);
        messageList.clear();
        if (cursor.moveToFirst()) {
            do {
                messageList.add(new MessageAdapter.Message(cursor.getString(3), cursor.getString(2)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
        updateWelcomeVisibility();
        if (messageList.size() > 0) recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (text.isEmpty()) return;

        db.saveMessage(sessionId, "user", text);
        messageList.add(new MessageAdapter.Message(text, "user"));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
        editInput.setText("");
        updateWelcomeVisibility();

        MessageAdapter.Message typingIndicator = MessageAdapter.Message.typing();
        messageList.add(typingIndicator);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        gemini.analyzeMood(messageList, text, new GeminiService.AnalysisCallback() {
            @Override
            public void onResult(GeminiService.MoodAnalysis analysis) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    int index = messageList.indexOf(typingIndicator);
                    if (index != -1) {
                        messageList.remove(index);
                        adapter.notifyItemRemoved(index);
                    }

                    String aiText = analysis.insight + "\n\n" + analysis.advice;
                    db.saveMessage(sessionId, "ai", aiText);
                    if (sessionId != -2) db.saveMood(analysis.score);

                    messageList.add(new MessageAdapter.Message(aiText, "ai"));
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    if (sessionId != -2) {
                        android.database.Cursor checkCursor = db.getMessages(sessionId);
                        if (checkCursor.getCount() <= 2 && analysis.chatTitle != null) {
                            db.updateSessionTitle(sessionId, analysis.chatTitle);
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).updateMenuWithSessions();
                            }
                        }
                        checkCursor.close();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    messageList.remove(typingIndicator);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}