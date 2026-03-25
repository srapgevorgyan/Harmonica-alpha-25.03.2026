package com.harmonica.application;

//chatframgent for

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChatFragment extends Fragment {
    private EditText editInput;
    private TextView txtResponse, txtLabel;
    private GeminiService gemini;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        editInput = v.findViewById(R.id.editMoodInput);
        txtResponse = v.findViewById(R.id.txtResponse);
        txtLabel = v.findViewById(R.id.txtMoodLabel);
        ImageButton btnSend = v.findViewById(R.id.btnSend);

        gemini = new GeminiService();

        btnSend.setOnClickListener(view -> {
            String userInput = editInput.getText().toString().trim();
            if (!userInput.isEmpty()) {
                performAnalysis(userInput);
            }
        });

        return v;
    }

    private void performAnalysis(String text) {
        editInput.setText(""); // Clear input
        txtLabel.setText("Analyzing...");
        txtResponse.setText("Processing your thoughts with care...");

        gemini.analyzeMood(text, new GeminiService.AnalysisCallback() {
            @Override
            public void onResult(GeminiService.MoodAnalysis analysis) {
                txtLabel.setText(analysis.label + " (Intensity: " + analysis.score + "/10)");
                // insight and advice combination for psychologist feel
                String fullResponse = analysis.insight + "\n\n" + analysis.advice;
                txtResponse.setText(fullResponse);

                MoodDatabase db = new MoodDatabase(getContext());
                db.saveMood(analysis.score);
            }

            @Override
            public void onError(String message) {
                txtLabel.setText("Connection Interrupted");
                txtResponse.setText("I'm sorry, I couldn't connect. Please try again.");
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
