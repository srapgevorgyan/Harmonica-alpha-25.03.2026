package com.harmonica.application;


//working gemini assistant class

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class GeminiService {
    private final GenerativeModelFutures model;

    public static class MoodAnalysis {
        public int score = 5;
        public String label = "Reflective";
        public String insight = "";
        public String advice = "";
    }

    public interface AnalysisCallback {
        void onResult(MoodAnalysis analysis);
        void onError(String message);
    }

    public GeminiService() {
        // 1. Personality Config
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.75f;
        GenerationConfig config = configBuilder.build();

        // 2. Request Options
        RequestOptions requestOptions = new RequestOptions();

        // 3. Initialize the base model
        GenerativeModel baseModel = new GenerativeModel(
                "gemini-2.5-flash",
                "AIzaSyDJiL7RQaHokuewdHRqNrMrxMxGxLzyBH4",
                config,
                new ArrayList<>(),
                requestOptions
        );

        // 4. Wrap it for JAVA (This fixes the red lines!)
        this.model = GenerativeModelFutures.from(baseModel);
    }

    public void analyzeMood(String userText, AnalysisCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());


        String systemPrompt = "You are Dr. Harmonica, a world-class psychologist and endocrinology expert. " +
                "Tone: Warm, deeply empathetic, scientifically grounded. " +
                "Explain the psychological & hormonal triggers (e.g. Cortisol, Serotonin). " +
                "Format: SCORE: [1-10]|LABEL: [Mood]|INSIGHT: [Explanation]|ADVICE: [Tip]";

        Content content = new Content.Builder()
                .addText(systemPrompt + "\n\nUser: " + userText)
                .build();

        // Use the Java-friendly Future call
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String rawText = result.getText();
                MoodAnalysis analysis = parseResponse(rawText);
                mainHandler.post(() -> callback.onResult(analysis));
            }

            @Override
            public void onFailure(Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        }, Executors.newSingleThreadExecutor()); // Run in background
    }

    private MoodAnalysis parseResponse(String raw) {
        MoodAnalysis m = new MoodAnalysis();
        if (raw == null) return m;
        try {
            String[] parts = raw.split("\\|");
            for (String p : parts) {
                if (p.contains("SCORE:")) m.score = Integer.parseInt(p.replace("SCORE:", "").trim());
                if (p.contains("LABEL:")) m.label = p.replace("LABEL:", "").trim();
                if (p.contains("INSIGHT:")) m.insight = p.replace("INSIGHT:", "").trim();
                if (p.contains("ADVICE:")) m.advice = p.replace("ADVICE:", "").trim();
            }
        } catch (Exception e) {
            m.insight = raw; // Fallback
        }
        return m;
    }
}