package com.harmonica.application;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject; // Standard Android JSON library

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class GeminiService {
    private final GenerativeModelFutures model;

    public static class MoodAnalysis {
        public int score = 5;
        public String label = "Neutral";
        public String insight = "";
        public String advice = "";
        public String chatTitle = "New Conversation";
    }

    public interface AnalysisCallback {
        void onResult(MoodAnalysis analysis);
        void onError(String message);
    }

    public GeminiService() {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.75f;
        // Force JSON response if the model supports it
        configBuilder.responseMimeType = "application/json";
        GenerationConfig config = configBuilder.build();

        RequestOptions requestOptions = new RequestOptions();

        GenerativeModel baseModel = new GenerativeModel(
                "gemini-2.5-flash",
                "AIzaSyDJiL7RQaHokuewdHRqNrMrxMxGxLzyBH4",
                config,
                new ArrayList<>(),
                requestOptions
        );

        this.model = GenerativeModelFutures.from(baseModel);
    }

    public void analyzeMood(String userText, AnalysisCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        String systemPrompt = "You are Dr. Harmonica, a world-class psychologist. " +
                "Analyze the user's mood and hormonal triggers. " +
                "IMPORTANT: You MUST respond ONLY with a valid JSON object. " +
                "JSON Schema: " +
                "{ \"score\": number, \"label\": string, \"insight\": string, \"advice\": string, \"chatTitle\": string }";

        Content content = new Content.Builder()
                .addText(systemPrompt + "\n\nUser: " + userText)
                .build();

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
        }, Executors.newSingleThreadExecutor());
    }

    private MoodAnalysis parseResponse(String raw) {
        MoodAnalysis m = new MoodAnalysis();
        if (raw == null || raw.isEmpty()) return m;

        try {
            // Clean the string (sometimes Gemini wraps JSON in markdown ```json blocks)
            String jsonStr = raw.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7, jsonStr.length() - 3).trim();
            } else if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3, jsonStr.length() - 3).trim();
            }

            JSONObject json = new JSONObject(jsonStr);

            m.score = json.optInt("score", 5);
            m.label = json.optString("label", "Neutral");
            m.insight = json.optString("insight", "");
            m.advice = json.optString("advice", "");
            m.chatTitle = json.optString("chatTitle", "New Conversation");

        } catch (Exception e) {
            // Fallback: If JSON parsing fails, put the raw text in the insight
            m.insight = raw;
            m.advice = "I'm processing your thoughts. Tell me more.";
        }
        return m;
    }
}