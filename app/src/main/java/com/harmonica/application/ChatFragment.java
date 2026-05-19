package com.harmonica.application;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ChatFragment extends Fragment implements TextToSpeech.OnInitListener {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<MessageAdapter.Message> messageList = new ArrayList<>();
    private EditText editInput;
    private MoodDatabase db;
    private long sessionId = -1;
    private GeminiService gemini;
    private LinearLayout layoutWelcome;
    private ImageButton btnSend, btnMic;
    private View root;
    private String currentUserId;
    private ChipGroup chipGroupModes;

    // Voice Chat components
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean isTtsEnabled = true;
    private ValueAnimator micPulseAnimator;
    private boolean isListening = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_chat, container, false);

        db = new MoodDatabase(getContext());
        gemini = new GeminiService(getContext());
        tts = new TextToSpeech(getContext(), this);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        if (getArguments() != null) sessionId = getArguments().getLong("sessionId", -1);
        if (sessionId == -1) sessionId = db.createSession("New Conversation...", currentUserId);

        recyclerView = root.findViewById(R.id.chatRecyclerView);
        editInput = root.findViewById(R.id.editMoodInput);
        btnSend = root.findViewById(R.id.btnSend);
        btnMic = root.findViewById(R.id.btnMic);
        layoutWelcome = root.findViewById(R.id.layoutWelcome);
        chipGroupModes = root.findViewById(R.id.chipGroupModes);

        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        setupModes();
        applyIncognitoStyling();
        loadHistory();
        
        btnSend.setOnClickListener(view -> sendMessage());
        setupVoiceChat();

        return root;
    }

    private void setupVoiceChat() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { editInput.setHint("Listening..."); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {
                    // Reactive scaling based on voice volume
                    if (isListening) {
                        float scale = 1.3f + (Math.max(0, rmsdB) / 15f);
                        if (scale > 1.8f) scale = 1.8f;
                        btnMic.setScaleX(scale);
                        btnMic.setScaleY(scale);
                    }
                }
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) { 
                    editInput.setHint("Message Dr. Harmonica..."); 
                    stopMicAnimation();
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (data != null && !data.isEmpty()) {
                        editInput.setText(data.get(0));
                        sendMessage();
                    }
                    editInput.setHint("Message Dr. Harmonica...");
                    stopMicAnimation();
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        btnMic.setOnTouchListener((v, event) -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return false;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (tts.isSpeaking()) tts.stop();
                startMicAnimation();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                speechRecognizer.startListening(intent);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                speechRecognizer.stopListening();
                return true;
            }
            return false;
        });
    }

    private void startMicAnimation() {
        isListening = true;
        
        // Haptic feedback
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(40);
            }
        }

        // Animate color to Red
        int themeColor = (sessionId == -2) ? 
                ContextCompat.getColor(getContext(), R.color.incognito_primary) : 
                ContextCompat.getColor(getContext(), R.color.harmonica_primary);
        
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), themeColor, Color.RED);
        colorAnim.setDuration(300);
        colorAnim.addUpdateListener(anim -> btnMic.setColorFilter((int) anim.getAnimatedValue(), PorterDuff.Mode.SRC_IN));
        colorAnim.start();

        // Base Pulse animation
        if (micPulseAnimator == null) {
            micPulseAnimator = ValueAnimator.ofFloat(1.2f, 1.4f);
            micPulseAnimator.setDuration(600);
            micPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            micPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            micPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            micPulseAnimator.addUpdateListener(animation -> {
                if (isListening) {
                    float s = (float) animation.getAnimatedValue();
                    // Note: onRmsChanged might override this momentarily, which is fine
                    btnMic.setScaleX(s);
                    btnMic.setScaleY(s);
                }
            });
        }
        micPulseAnimator.start();
    }

    private void stopMicAnimation() {
        isListening = false;
        if (micPulseAnimator != null) micPulseAnimator.cancel();

        // Animate back to theme color
        int themeColor = (sessionId == -2) ? 
                ContextCompat.getColor(getContext(), R.color.incognito_primary) : 
                ContextCompat.getColor(getContext(), R.color.harmonica_primary);
        
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), Color.RED, themeColor);
        colorAnim.setDuration(300);
        colorAnim.addUpdateListener(anim -> btnMic.setColorFilter((int) anim.getAnimatedValue(), PorterDuff.Mode.SRC_IN));
        colorAnim.start();

        // Bouncy scale return
        btnMic.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
            tts.setPitch(1.0f);
            tts.setSpeechRate(0.9f);
        }
    }

    private void setupModes() {
        chipGroupModes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                WorkManager.getInstance(requireContext()).cancelUniqueWork("mode_reminders");
                return;
            }
            int id = checkedIds.get(0);
            String initialMessage = "";
            if (id == R.id.chipPanic) initialMessage = "I think I'm having a panic attack. Please help me stay calm.";
            else if (id == R.id.chipDepression) initialMessage = "I'm feeling very low and unmotivated. Can we talk about it?";
            else if (id == R.id.chipAnxiety) initialMessage = "I feel very anxious and my heart is racing. What should I do?";
            else if (id == R.id.chipStress) initialMessage = "My stress levels are through the roof. I need some peace.";

            if (!initialMessage.isEmpty()) {
                editInput.setText(initialMessage);
                sendMessage();
            }
        });
    }

    private void scheduleReminders(String modeName) {
        Data inputData = new Data.Builder().putString("mode_name", modeName).build();
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(ModeReminderWorker.class, 2, TimeUnit.HOURS).setInputData(inputData).build();
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork("mode_reminders", ExistingPeriodicWorkPolicy.REPLACE, reminderRequest);
    }

    private void applyIncognitoStyling() {
        boolean isIncognito = (sessionId == -2);
        adapter.setIncognito(isIncognito);
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setIncognitoMode(isIncognito);
        if (isIncognito) {
            root.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.incognito_bg));
            View inputContainer = root.findViewById(R.id.layout_input_container);
            if (inputContainer != null) inputContainer.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_input_bar_incognito));
            btnSend.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.incognito_primary), PorterDuff.Mode.SRC_IN);
            btnMic.setColorFilter(ContextCompat.getColor(getContext(), R.color.incognito_primary), PorterDuff.Mode.SRC_IN);
        } else {
            btnSend.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.harmonica_primary), PorterDuff.Mode.SRC_IN);
            btnMic.setColorFilter(ContextCompat.getColor(getContext(), R.color.harmonica_primary), PorterDuff.Mode.SRC_IN);
        }
    }

    private void updateWelcomeVisibility() {
        if (layoutWelcome != null) layoutWelcome.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
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
            do { messageList.add(new MessageAdapter.Message(cursor.getString(3), cursor.getString(2))); } while (cursor.moveToNext());
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
                    if (index != -1) { messageList.remove(index); adapter.notifyItemRemoved(index); }
                    String aiText = analysis.insight + "\n\n" + analysis.advice;
                    db.saveMessage(sessionId, "ai", aiText);
                    if (sessionId != -2) db.saveMood(analysis.score, currentUserId);
                    MessageAdapter.Message aiMessage = new MessageAdapter.Message(aiText, "ai");
                    if (analysis.suggestedPractices != null && !analysis.suggestedPractices.isEmpty()) aiMessage.suggestedPractice = analysis.suggestedPractices.get(0);
                    messageList.add(aiMessage);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    if (isTtsEnabled) tts.speak(analysis.insight, TextToSpeech.QUEUE_FLUSH, null, null);
                });
            }
            @Override public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (messageList.contains(typingIndicator)) { messageList.remove(typingIndicator); adapter.notifyDataSetChanged(); }
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
        super.onDestroy();
    }
}
