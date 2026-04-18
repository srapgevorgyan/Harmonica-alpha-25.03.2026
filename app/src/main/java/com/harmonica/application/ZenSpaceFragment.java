package com.harmonica.application;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class ZenSpaceFragment extends Fragment {

    private String type, title, instruction;
    private View circle;
    private TextView txtAction, txtPrompt, txtZenTitle;
    private LinearLayout layoutSelection, layoutBreathing, layoutGrounding;
    private Button btnEndZen;
    private RecyclerView rvGroundingList;
    private boolean isRunning = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
        txtZenTitle.setText(title != null ? title : "Zen Space");
        
        layoutSelection = v.findViewById(R.id.layoutSelection);
        layoutBreathing = v.findViewById(R.id.layoutBreathing);
        layoutGrounding = v.findViewById(R.id.layoutGrounding);
        circle = v.findViewById(R.id.viewBreathingCircle);
        txtAction = v.findViewById(R.id.txtBreathAction);
        txtPrompt = v.findViewById(R.id.txtGroundingPrompt);
        btnEndZen = v.findViewById(R.id.btnEndZen);
        rvGroundingList = v.findViewById(R.id.rvGroundingList);

        // Selection Cards
        v.findViewById(R.id.cardBreathing).setOnClickListener(view -> {
            type = "Breathing";
            startBreathingExercise();
        });

        v.findViewById(R.id.cardGrounding).setOnClickListener(view -> {
            type = "Grounding";
            startGroundingExercise();
        });

        btnEndZen.setOnClickListener(view -> {
            if (layoutSelection.getVisibility() == View.VISIBLE) {
                if (getActivity() != null) {
                    getParentFragmentManager().popBackStack();
                }
            } else {
                isRunning = false;
                resetToSelection();
            }
        });

        if (type != null) {
            if ("Breathing".equalsIgnoreCase(type)) {
                startBreathingExercise();
            } else if ("Grounding".equalsIgnoreCase(type)) {
                startGroundingExercise();
            }
        } else {
            resetToSelection();
        }

        return v;
    }

    private void resetToSelection() {
        layoutSelection.setVisibility(View.VISIBLE);
        layoutBreathing.setVisibility(View.GONE);
        layoutGrounding.setVisibility(View.GONE);
        txtZenTitle.setText("Zen Space");
        btnEndZen.setText("Back");
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
                        public void onAnimationEnd(Animator animation) {
                            runBreathingCycle();
                        }
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
        
        List<String> items = Arrays.asList(
            "👀 5 things you can SEE",
            "✋ 4 things you can TOUCH",
            "👂 3 things you can HEAR",
            "👃 2 things you can SMELL",
            "👅 1 thing you can TASTE"
        );
        
        rvGroundingList.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView tv = holder.itemView.findViewById(android.R.id.text1);
                tv.setText(items.get(position));
                tv.setTextColor(Color.parseColor("#2D2D2D"));
            }

            @Override
            public int getItemCount() { return items.size(); }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }
}
