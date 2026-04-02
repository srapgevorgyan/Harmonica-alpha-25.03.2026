package com.harmonica.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        db = new MoodDatabase(getContext());
        gemini = new GeminiService();

        // Retrieve sessionId from arguments or create a temporary one
        if (getArguments() != null) sessionId = getArguments().getLong("sessionId", -1);
        if (sessionId == -1) sessionId = db.createSession("New Conversation...");

        recyclerView = v.findViewById(R.id.chatRecyclerView);
        editInput = v.findViewById(R.id.editMoodInput);
        ImageButton btnSend = v.findViewById(R.id.btnSend);

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadHistory();
        btnSend.setOnClickListener(view -> sendMessage());

        return v;

    }

    private void loadHistory() {
        android.database.Cursor cursor = db.getMessages(sessionId);
        messageList.clear();
        if (cursor.moveToFirst()) {
            do {
                messageList.add(new MessageAdapter.Message(cursor.getString(3), cursor.getString(2)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
        if (messageList.size() > 0) recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // 1. Save & Show User Message
        db.saveMessage(sessionId, "user", text);
        messageList.add(new MessageAdapter.Message(text, "user"));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
        editInput.setText("");

        // 2. Show Typing Indicator
        MessageAdapter.Message typingIndicator = MessageAdapter.Message.typing();
        messageList.add(typingIndicator);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        // 3. Get AI Response with full history context
        gemini.analyzeMood(messageList, text, new GeminiService.AnalysisCallback() {
            @Override
            public void onResult(GeminiService.MoodAnalysis analysis) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // A. Remove typing indicator
                    int index = messageList.indexOf(typingIndicator);
                    if (index != -1) {
                        messageList.remove(index);
                        adapter.notifyItemRemoved(index);
                    }

                    // B. Save AI Response and show it
                    String aiText = analysis.insight + "\n\n" + analysis.advice;
                    db.saveMessage(sessionId, "ai", aiText);
                    db.saveMood(analysis.score);

                    messageList.add(new MessageAdapter.Message(aiText, "ai"));
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    // C. SMART TITLE LOGIC: If this was the first message, update the sidebar title
                    android.database.Cursor checkCursor = db.getMessages(sessionId);
                    // Count is 2 because we have 1 user message and 1 AI message now
                    if (checkCursor.getCount() <= 2 && analysis.chatTitle != null) {
                        db.updateSessionTitle(sessionId, analysis.chatTitle);

                        // Refresh the sidebar in MainActivity
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateMenuWithSessions();
                        }
                    }
                    checkCursor.close();
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