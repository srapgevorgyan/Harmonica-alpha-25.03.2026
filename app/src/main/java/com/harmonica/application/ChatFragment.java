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

        // Get sessionId from arguments (passed from MainActivity)
        if (getArguments() != null) sessionId = getArguments().getLong("sessionId", -1);

        // If no session, create a new one
        if (sessionId == -1) sessionId = db.createSession("New Conversation " + System.currentTimeMillis());

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
        if (messageList.size() > 0) {
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // 1. Save & Show User Message
        db.saveMessage(sessionId, "user", text);
        messageList.add(new MessageAdapter.Message(text, "user"));
        adapter.notifyItemInserted(messageList.size() - 1);
        if (messageList.size() > 0) {
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
        editInput.setText("");

        // 2. Get AI Response
        gemini.analyzeMood(text, new GeminiService.AnalysisCallback() {
            @Override
            public void onResult(GeminiService.MoodAnalysis analysis) {
                String aiText = analysis.insight + "\n\n" + analysis.advice;
                db.saveMessage(sessionId, "ai", aiText);
                db.saveMood(analysis.score); // Still track mood for graph

                messageList.add(new MessageAdapter.Message(aiText, "ai"));
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}