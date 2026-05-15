package com.harmonica.application;

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
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.graphics.Color;
import android.graphics.PorterDuff;

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
    private String currentUserId;
    private ChipGroup chipGroupModes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_chat, container, false);

        db = new MoodDatabase(getContext());
        gemini = new GeminiService(getContext());
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        if (getArguments() != null) sessionId = getArguments().getLong("sessionId", -1);
        if (sessionId == -1) sessionId = db.createSession("New Conversation...", currentUserId);

        recyclerView = root.findViewById(R.id.chatRecyclerView);
        editInput = root.findViewById(R.id.editMoodInput);
        btnSend = root.findViewById(R.id.btnSend);
        layoutWelcome = root.findViewById(R.id.layoutWelcome);
        chipGroupModes = root.findViewById(R.id.chipGroupModes);

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        setupModes();
        applyIncognitoStyling();
        loadHistory();
        btnSend.setOnClickListener(view -> sendMessage());

        return root;
    }

    private void setupModes() {
        chipGroupModes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                WorkManager.getInstance(requireContext()).cancelUniqueWork("mode_reminders");
                return;
            }
            
            int id = checkedIds.get(0);
            String modeName = "";
            String initialMessage = "";

            if (id == R.id.chipPanic) {
                modeName = "Panic Attack Mode";
                initialMessage = "I think I'm having a panic attack. Please help me stay calm.";
            } else if (id == R.id.chipDepression) {
                modeName = "Depression Mode";
                initialMessage = "I'm feeling very low and unmotivated. Can we talk about it?";
            } else if (id == R.id.chipAnxiety) {
                modeName = "Anxiety Mode";
                initialMessage = "I feel very anxious and my heart is racing. What should I do?";
            } else if (id == R.id.chipStress) {
                modeName = "Elevated Stress";
                initialMessage = "My stress levels are through the roof. I need some peace.";
            }

            if (!initialMessage.isEmpty()) {
                editInput.setText(initialMessage);
                Toast.makeText(getContext(), modeName + " activated. Reminders scheduled.", Toast.LENGTH_SHORT).show();
                scheduleReminders(modeName);
            }
        });
    }

    private void scheduleReminders(String modeName) {
        Data inputData = new Data.Builder()
                .putString("mode_name", modeName)
                .build();

        // Standard interval is 15 mins (minimum for PeriodicWorkRequest)
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ModeReminderWorker.class, 2, TimeUnit.HOURS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "mode_reminders",
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest
        );
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
            
            btnSend.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.incognito_primary), PorterDuff.Mode.SRC_IN);
            btnSend.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

            if (layoutWelcome != null) {
                TextView title = (TextView) layoutWelcome.getChildAt(1);
                TextView sub = (TextView) layoutWelcome.getChildAt(2);
                title.setText("Incognito Space");
                title.setTextColor(ContextCompat.getColor(getContext(), R.color.incognito_primary));
                sub.setText("Your messages won't be saved in your history. Talk freely.");
                sub.setTextColor(ContextCompat.getColor(getContext(), R.color.incognito_hint));
            }
        } else {
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
                    if (sessionId != -2) db.saveMood(analysis.score, currentUserId);

                    MessageAdapter.Message aiMessage = new MessageAdapter.Message(aiText, "ai");
                    if (analysis.suggestedPractices != null && !analysis.suggestedPractices.isEmpty()) {
                        aiMessage.suggestedPractice = analysis.suggestedPractices.get(0);
                    }
                    
                    messageList.add(aiMessage);
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
                    if (messageList.contains(typingIndicator)) {
                        messageList.remove(typingIndicator);
                        adapter.notifyDataSetChanged();
                    }
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}