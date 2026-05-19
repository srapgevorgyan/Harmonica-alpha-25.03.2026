package com.harmonica.application;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class ZenSpaceFragment extends Fragment {

    private String type, title, instruction;
    private View circle, viewBilateralDot;
    private TextView txtAction, txtPrompt, txtZenTitle;
    private View layoutSelection;
    private LinearLayout layoutBreathing, layoutGrounding, layoutGratitude;
    private View layoutBilateral;
    private Button btnEndZen, btnSaveGratitude;
    private EditText editG1, editG2, editG3;
    private RecyclerView rvGroundingList;
    private boolean isRunning = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Procedural Audio for Bilateral Stimulation
    private AudioTrack humTrack;
    private volatile float currentPan = 0.0f; // -1.0 (left) to 1.0 (right)

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
        
        circle = v.findViewById(R.id.viewBreathingCircle);
        viewBilateralDot = v.findViewById(R.id.viewBilateralDot);
        txtAction = v.findViewById(R.id.txtBreathAction);
        txtPrompt = v.findViewById(R.id.txtGroundingPrompt);
        btnEndZen = v.findViewById(R.id.btnEndZen);
        rvGroundingList = v.findViewById(R.id.rvGroundingList);
        
        editG1 = v.findViewById(R.id.editGratitude1);
        editG2 = v.findViewById(R.id.editGratitude2);
        editG3 = v.findViewById(R.id.editGratitude3);
        btnSaveGratitude = v.findViewById(R.id.btnSaveGratitude);

        txtZenTitle.setText(title != null ? title : "Zen Space");

        v.findViewById(R.id.cardBreathing).setOnClickListener(view -> startBreathingExercise());
        v.findViewById(R.id.cardGrounding).setOnClickListener(view -> startGroundingExercise());
        v.findViewById(R.id.cardGratitude).setOnClickListener(view -> startGratitudeExercise());
        v.findViewById(R.id.cardBilateral).setOnClickListener(view -> startBilateralExercise());

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

        if (type != null) {
            if ("Breathing".equalsIgnoreCase(type)) startBreathingExercise();
            else if ("Grounding".equalsIgnoreCase(type)) startGroundingExercise();
            else if ("Gratitude".equalsIgnoreCase(type)) startGratitudeExercise();
            else if ("Bilateral".equalsIgnoreCase(type)) startBilateralExercise();
            else resetToSelection();
        } else {
            resetToSelection();
        }

        return v;
    }

    private void resetToSelection() {
        layoutSelection.setVisibility(View.VISIBLE);
        layoutBreathing.setVisibility(View.GONE);
        layoutGrounding.setVisibility(View.GONE);
        layoutGratitude.setVisibility(View.GONE);
        layoutBilateral.setVisibility(View.GONE);
        txtZenTitle.setText("Zen Space");
        btnEndZen.setText("Back");
    }

    private void stopExercise() {
        isRunning = false;
        stopAudio();
        resetToSelection();
    }

    private void startBilateralExercise() {
        layoutSelection.setVisibility(View.GONE);
        layoutBilateral.setVisibility(View.VISIBLE);
        isRunning = true;
        txtZenTitle.setText("Bilateral Session");
        btnEndZen.setText("Stop Exercise");

        Toast.makeText(getContext(), "Connect headphones for the full EMDR effect.", Toast.LENGTH_SHORT).show();
        startAudioHum();
        
        layoutBilateral.post(() -> {
            float width = layoutBilateral.getWidth() - viewBilateralDot.getWidth();
            runBilateralAnimation(width);
        });
    }

    private void runBilateralAnimation(float pathWidth) {
        if (!isRunning || viewBilateralDot == null) return;

        ObjectAnimator animator = ObjectAnimator.ofFloat(viewBilateralDot, "translationX", 0, pathWidth);
        animator.setDuration(1800); // 1.8 seconds per side
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);

        animator.addUpdateListener(animation -> {
            if (!isRunning) {
                animation.cancel();
                return;
            }
            float translationX = (float) animation.getAnimatedValue();
            // Map 0 -> pathWidth to -1.0 (Left) -> 1.0 (Right)
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
                        phase += 2 * Math.PI * 180.0 / sampleRate; // 180Hz smooth hum
                        short val = (short) (Math.sin(phase) * 7000); // Decent volume
                        
                        float pan = currentPan;
                        // Equal power-ish panning (simplified)
                        float leftVol = (1.0f - pan) / 2.0f;
                        float rightVol = (1.0f + pan) / 2.0f;
                        
                        buffer[i * 2] = (short) (val * leftVol);
                        buffer[i * 2 + 1] = (short) (val * rightVol);
                    }
                    if (humTrack != null) humTrack.write(buffer, 0, buffer.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void stopAudio() {
        if (humTrack != null) {
            try {
                humTrack.stop();
                humTrack.release();
            } catch (Exception ignored) {}
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

    private void startGroundingExercise() {
        layoutSelection.setVisibility(View.GONE);
        layoutGrounding.setVisibility(View.VISIBLE);
        txtZenTitle.setText("Grounding Session");
        btnEndZen.setText("Finish");
        txtPrompt.setText(instruction != null ? instruction : "Focus on your surroundings using the 5-4-3-2-1 method.");
        rvGroundingList.setLayoutManager(new LinearLayoutManager(getContext()));
        List<String> items = Arrays.asList("👀 5 things you SEE", "✋ 4 things you TOUCH", "👂 3 things you HEAR", "👃 2 things you SMELL", "👅 1 thing you TASTE");
        rvGroundingList.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int p) {
                ((TextView)h.itemView.findViewById(android.R.id.text1)).setText(items.get(p));
                ((TextView)h.itemView.findViewById(android.R.id.text1)).setTextColor(Color.parseColor("#2D2D2D"));
            }
            @Override public int getItemCount() { return items.size(); }
        });
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
        handler.removeCallbacksAndMessages(null);
    }
}
