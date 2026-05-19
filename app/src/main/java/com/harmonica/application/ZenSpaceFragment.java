package com.harmonica.application;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ZenSpaceFragment extends Fragment {

    private String type, title, instruction;
    private View circle, viewBilateralDot;
    private TextView txtAction, txtPrompt, txtZenTitle;
    private View layoutSelection;
    private LinearLayout layoutBreathing, layoutGrounding, layoutGratitude, layoutPMR;
    private View layoutBilateral;
    private Button btnEndZen, btnSaveGratitude;
    private EditText editG1, editG2, editG3;
    private RecyclerView rvGroundingList;
    private TextView txtLiveTranscription;
    private FloatingActionButton fabMic;
    
    private TextView txtPMRStep, txtPMRAction;
    private ProgressBar progressPMR;
    
    private boolean isRunning = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Voice Grounding
    private SpeechRecognizer speechRecognizer;
    private int groundingStepCount = 5; // 5 see, 4 touch, etc.
    private List<String> identifiedItems = new ArrayList<>();
    private GroundingAdapter groundingAdapter;

    // Procedural Audio for Bilateral Stimulation
    private AudioTrack humTrack;
    private volatile float currentPan = 0.0f;

    // Haptic PMR
    private Vibrator vibrator;
    private int pmrCurrentStep = 0;
    private final String[] pmrSteps = {"Shoulders", "Arms & Hands", "Stomach", "Thighs", "Feet"};

    public static ZenSpaceFragment newInstance(String type, String title, String instruction) {
        ZenSpaceFragment fragment = new ZenSpaceFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("title", title);
        args.putString("instruction", instruction);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_zen_space, container, false);

        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

        if (getArguments() != null) {
            type = getArguments().getString("type");
            title = getArguments().getString("title");
            instruction = getArguments().getString("instruction");
        }

        txtZenTitle = v.findViewById(R.id.txtZenTitle);
        layoutSelection = v.findViewById(R.id.layoutSelection);
        layoutBreathing = v.findViewById(R.id.layoutBreathing);
        layoutGrounding = v.findViewById(R.id.layoutGrounding);
        layoutGratitude = v.findViewById(R.id.layoutGratitude);
        layoutBilateral = v.findViewById(R.id.layoutBilateral);
        layoutPMR = v.findViewById(R.id.layoutPMR);
        
        circle = v.findViewById(R.id.viewBreathingCircle);
        viewBilateralDot = v.findViewById(R.id.viewBilateralDot);
        txtAction = v.findViewById(R.id.txtBreathAction);
        txtPrompt = v.findViewById(R.id.txtGroundingPrompt);
        btnEndZen = v.findViewById(R.id.btnEndZen);
        rvGroundingList = v.findViewById(R.id.rvGroundingList);
        txtLiveTranscription = v.findViewById(R.id.txtLiveTranscription);
        fabMic = v.findViewById(R.id.fabMic);
        
        editG1 = v.findViewById(R.id.editGratitude1);
        editG2 = v.findViewById(R.id.editGratitude2);
        editG3 = v.findViewById(R.id.editGratitude3);
        btnSaveGratitude = v.findViewById(R.id.btnSaveGratitude);

        txtPMRStep = v.findViewById(R.id.txtPMRStep);
        txtPMRAction = v.findViewById(R.id.txtPMRAction);
        progressPMR = v.findViewById(R.id.progressPMR);

        txtZenTitle.setText(title != null ? title : "Zen Space");

        v.findViewById(R.id.cardBreathing).setOnClickListener(view -> startBreathingExercise());
        v.findViewById(R.id.cardGrounding).setOnClickListener(view -> startGroundingExercise());
        v.findViewById(R.id.cardGratitude).setOnClickListener(view -> startGratitudeExercise());
        v.findViewById(R.id.cardBilateral).setOnClickListener(view -> startBilateralExercise());
        v.findViewById(R.id.cardPMR).setOnClickListener(view -> startPMRExercise());

        btnEndZen.setOnClickListener(view -> {
            if (layoutSelection.getVisibility() == View.VISIBLE) {
                if (getActivity() != null) getParentFragmentManager().popBackStack();
            } else {
                stopExercise();
            }
        });
        
        btnSaveGratitude.setOnClickListener(view -> {
            Toast.makeText(getContext(), "Thoughts saved to your heart.", Toast.LENGTH_SHORT).show();
            stopExercise();
        });

        initSpeechRecognizer();

        if (type != null) {
            if ("Breathing".equalsIgnoreCase(type)) startBreathingExercise();
            else if ("Grounding".equalsIgnoreCase(type)) startGroundingExercise();
            else if ("Gratitude".equalsIgnoreCase(type)) startGratitudeExercise();
            else if ("Bilateral".equalsIgnoreCase(type)) startBilateralExercise();
            else if ("PMR".equalsIgnoreCase(type) || "Muscle Relaxation".equalsIgnoreCase(type)) startPMRExercise();
            else resetToSelection();
        } else {
            resetToSelection();
        }

        return v;
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { txtLiveTranscription.setText("Listening..."); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) { txtLiveTranscription.setText("Try again..."); }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (data != null && !data.isEmpty()) {
                        processGroundingInput(data.get(0));
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (data != null && !data.isEmpty()) txtLiveTranscription.setText(data.get(0));
                }
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void processGroundingInput(String text) {
        txtLiveTranscription.setText("\"" + text + "\"");
        identifiedItems.add(text);
        groundingAdapter.notifyItemInserted(identifiedItems.size() - 1);
        rvGroundingList.scrollToPosition(identifiedItems.size() - 1);

        if (identifiedItems.size() >= groundingStepCount) {
            moveToNextGroundingStep();
        }
    }

    private void moveToNextGroundingStep() {
        groundingStepCount--;
        identifiedItems.clear();
        groundingAdapter.notifyDataSetChanged();
        
        if (groundingStepCount == 0) {
            Toast.makeText(getContext(), "Grounding Complete. You are here, you are safe.", Toast.LENGTH_LONG).show();
            stopExercise();
            return;
        }

        String[] prompts = {
            "Voice Grounding",
            "Name 5 things you can SEE.",
            "Name 4 things you can TOUCH.",
            "Name 3 things you can HEAR.",
            "Name 2 things you can SMELL.",
            "Name 1 thing you can TASTE."
        };
        txtPrompt.setText(prompts[6 - groundingStepCount]);
    }

    private void startGroundingExercise() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        layoutSelection.setVisibility(View.GONE);
        layoutGrounding.setVisibility(View.VISIBLE);
        isRunning = true;
        groundingStepCount = 5;
        identifiedItems.clear();
        txtZenTitle.setText("Voice Grounding");
        txtPrompt.setText("Hold the mic and name 5 things you can SEE.");
        btnEndZen.setText("Stop Exercise");

        groundingAdapter = new GroundingAdapter(identifiedItems);
        rvGroundingList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGroundingList.setAdapter(groundingAdapter);

        fabMic.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                speechRecognizer.startListening(intent);
                fabMic.setAlpha(0.5f);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                speechRecognizer.stopListening();
                fabMic.setAlpha(1.0f);
            }
            return true;
        });
    }

    private void resetToSelection() {
        layoutSelection.setVisibility(View.VISIBLE);
        layoutBreathing.setVisibility(View.GONE);
        layoutGrounding.setVisibility(View.GONE);
        layoutGratitude.setVisibility(View.GONE);
        layoutBilateral.setVisibility(View.GONE);
        layoutPMR.setVisibility(View.GONE);
        txtZenTitle.setText("Zen Space");
        btnEndZen.setText("Back");
    }

    private void stopExercise() {
        isRunning = false;
        stopAudio();
        if (vibrator != null) vibrator.cancel();
        if (speechRecognizer != null) speechRecognizer.stopListening();
        resetToSelection();
    }

    // PMR Logic
    private void startPMRExercise() {
        layoutSelection.setVisibility(View.GONE);
        layoutPMR.setVisibility(View.VISIBLE);
        isRunning = true;
        txtZenTitle.setText("Muscle Relaxation");
        btnEndZen.setText("Stop Exercise");
        pmrCurrentStep = 0;
        runPMRCycle();
    }

    private void runPMRCycle() {
        if (!isRunning || pmrCurrentStep >= pmrSteps.length) {
            if (pmrCurrentStep >= pmrSteps.length) {
                Toast.makeText(getContext(), "Exercise complete. Feel the relaxation.", Toast.LENGTH_LONG).show();
                stopExercise();
            }
            return;
        }
        txtPMRStep.setText(pmrSteps[pmrCurrentStep]);
        txtPMRAction.setText("TENSE UP!");
        txtPMRAction.setTextColor(Color.RED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else { vibrator.vibrate(5000); }

        ValueAnimator progressAnim = ValueAnimator.ofInt(0, 100);
        progressAnim.setDuration(5000);
        progressAnim.addUpdateListener(animation -> progressPMR.setProgress((int) animation.getAnimatedValue()));
        progressAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isRunning) return;
                txtPMRAction.setText("RELEASE...");
                txtPMRAction.setTextColor(Color.parseColor("#4CAF50"));
                vibrator.cancel();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 200, 500}, new int[]{0, 50, 0}, 0));
                } else { vibrator.vibrate(new long[]{0, 200, 500}, 0); }
                handler.postDelayed(() -> {
                    if (!isRunning) return;
                    vibrator.cancel();
                    pmrCurrentStep++;
                    runPMRCycle();
                }, 5000);
            }
        });
        progressAnim.start();
    }

    // Bilateral Logic
    private void startBilateralExercise() {
        layoutSelection.setVisibility(View.GONE);
        layoutBilateral.setVisibility(View.VISIBLE);
        isRunning = true;
        txtZenTitle.setText("Bilateral Session");
        btnEndZen.setText("Stop Exercise");
        startAudioHum();
        layoutBilateral.post(() -> {
            float width = layoutBilateral.getWidth() - viewBilateralDot.getWidth();
            runBilateralAnimation(width);
        });
    }

    private void runBilateralAnimation(float pathWidth) {
        if (!isRunning || viewBilateralDot == null) return;
        ObjectAnimator animator = ObjectAnimator.ofFloat(viewBilateralDot, "translationX", 0, pathWidth);
        animator.setDuration(1800);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(animation -> {
            if (!isRunning) { animation.cancel(); return; }
            float translationX = (float) animation.getAnimatedValue();
            currentPan = (translationX / pathWidth) * 2.0f - 1.0f;
        });
        animator.start();
    }

    private void startAudioHum() {
        stopAudio();
        isRunning = true;
        new Thread(() -> {
            int sampleRate = 44100;
            int minSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            try {
                humTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
                        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 
                        minSize, AudioTrack.MODE_STREAM);
                short[] buffer = new short[minSize];
                double phase = 0;
                humTrack.play();
                while (isRunning && humTrack != null) {
                    for (int i = 0; i < buffer.length / 2; i++) {
                        phase += 2 * Math.PI * 220.0 / sampleRate;
                        short val = (short) (Math.sin(phase) * 12000);
                        float pan = currentPan;
                        float leftVol = (1.0f - pan) / 2.0f;
                        float rightVol = (1.0f + pan) / 2.0f;
                        buffer[i * 2] = (short) (val * leftVol);
                        buffer[i * 2 + 1] = (short) (val * rightVol);
                    }
                    if (humTrack != null) humTrack.write(buffer, 0, buffer.length);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void stopAudio() {
        if (humTrack != null) {
            try { humTrack.stop(); humTrack.release(); } catch (Exception ignored) {}
            humTrack = null;
        }
    }

    private void startBreathingExercise() {
        layoutSelection.setVisibility(View.GONE);
        layoutBreathing.setVisibility(View.VISIBLE);
        isRunning = true;
        txtZenTitle.setText("Breathing Session");
        btnEndZen.setText("Stop Exercise");
        runBreathingCycle();
    }

    private void runBreathingCycle() {
        if (!isRunning || circle == null) return;
        txtAction.setText("Inhale...");
        ObjectAnimator inhale = ObjectAnimator.ofPropertyValuesHolder(circle,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.5f));
        inhale.setDuration(4000);
        inhale.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isRunning) return;
                txtAction.setText("Hold...");
                handler.postDelayed(() -> {
                    if (!isRunning || circle == null) return;
                    txtAction.setText("Exhale...");
                    ObjectAnimator exhale = ObjectAnimator.ofPropertyValuesHolder(circle,
                            PropertyValuesHolder.ofFloat("scaleX", 1.5f, 1f),
                            PropertyValuesHolder.ofFloat("scaleY", 1.5f, 1f));
                    exhale.setDuration(4000);
                    exhale.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) { runBreathingCycle(); }
                    });
                    exhale.start();
                }, 4000);
            }
        });
        inhale.start();
    }

    private void startGratitudeExercise() {
        layoutSelection.setVisibility(View.GONE);
        layoutGratitude.setVisibility(View.VISIBLE);
        txtZenTitle.setText("Gratitude Practice");
        btnEndZen.setText("Stop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isRunning = false;
        stopAudio();
        if (vibrator != null) vibrator.cancel();
        if (speechRecognizer != null) speechRecognizer.destroy();
        handler.removeCallbacksAndMessages(null);
    }

    private static class GroundingAdapter extends RecyclerView.Adapter<GroundingAdapter.VH> {
        private List<String> items;
        GroundingAdapter(List<String> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            h.tv.setText("✓ " + items.get(p));
            h.tv.setTextColor(Color.parseColor("#4CAF50"));
            h.tv.setPadding(32, 16, 32, 16);
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(View v) { super(v); tv = v.findViewById(android.R.id.text1); }
        }
    }
}
