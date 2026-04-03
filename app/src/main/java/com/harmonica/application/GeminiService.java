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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class GeminiService {
    private final GenerativeModelFutures model;
    private final String API_KEY = "AIzaSyBSI1rHTjJC50kzNtE245lcYbmpinPpYmY";

    public static class MoodAnalysis {
        public int score = 5;
        public String label = "Neutral";
        public String insight = "";
        public String advice = "";
        public String chatTitle = "New Conversation";
    }

    public static class HormoneEducation {
        public String name;
        public String type;
        public String description;
        public String url;
    }

    public interface AnalysisCallback {
        void onResult(MoodAnalysis analysis);
        void onError(String message);
    }

    public interface HormoneCallback {
        void onResult(HormoneEducation hormone);
        void onError(String message);
    }

    public GeminiService() {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 1.0f;
        configBuilder.responseMimeType = "application/json";
        GenerationConfig config = configBuilder.build();

        RequestOptions requestOptions = new RequestOptions();

        GenerativeModel baseModel = new GenerativeModel(
                "gemini-2.5-flash",
                API_KEY,
                config,
                new ArrayList<>(),
                requestOptions
        );

        this.model = GenerativeModelFutures.from(baseModel);
    }

    public void analyzeMood(List<MessageAdapter.Message> history, String userText, AnalysisCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are Dr. Harmonica, a psychological assistant and hormonal health expert. ")
                .append("Analyze the conversation history and the latest message. ")
                .append("Provide empathetic insights and actionable advice focused on hormonal balance.\n\n")
                .append("RESPOND ONLY IN JSON matching this schema:\n")
                .append("{ \"score\": number, \"label\": string, \"insight\": string, \"advice\": string, \"chatTitle\": string }\n\n")
                .append("HISTORY:\n");

        for (MessageAdapter.Message msg : history) {
            if (msg.isTyping) continue;
            String role = msg.sender.equals("user") ? "User" : "Assistant";
            promptBuilder.append(role).append(": ").append(msg.text).append("\n");
        }
        promptBuilder.append("User (Latest): ").append(userText);

        Content content = new Content.Builder().addText(promptBuilder.toString()).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                mainHandler.post(() -> callback.onResult(parseMoodResponse(result.getText())));
            }

            @Override
            public void onFailure(Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        }, Executors.newSingleThreadExecutor());
    }

    public void getRandomHormone(List<String> excludeList, HormoneCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Pick a random hormone or neurochemical. Be creative and pick something unique. ")
                .append("CRITICAL: Do NOT pick any of the following recently shown: ")
                .append(String.join(", ", excludeList)).append(". ")
                .append("\n\nFor the 'url' field, provide a RELIABLE search link to Mayo Clinic or Healthline. ")
                .append("Example format: https://www.mayoclinic.org/search/search-results?q=hormone_name ")
                .append("or https://www.healthline.com/search?q1=hormone_name. ")
                .append("\n\nRESPOND ONLY IN JSON: { \"name\": string, \"type\": string, \"description\": string, \"url\": string }");

        Content content = new Content.Builder().addText(promptBuilder.toString()).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                mainHandler.post(() -> callback.onResult(parseHormoneResponse(result.getText())));
            }

            @Override
            public void onFailure(Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        }, Executors.newSingleThreadExecutor());
    }

    private MoodAnalysis parseMoodResponse(String raw) {
        MoodAnalysis m = new MoodAnalysis();
        try {
            JSONObject json = new JSONObject(cleanJson(raw));
            m.score = json.optInt("score", 5);
            m.label = json.optString("label", "Neutral");
            m.insight = json.optString("insight", "");
            m.advice = json.optString("advice", "");
            m.chatTitle = json.optString("chatTitle", "New Topic");
        } catch (Exception e) {
            m.insight = (raw != null) ? raw : "Error parsing response.";
        }
        return m;
    }

    private HormoneEducation parseHormoneResponse(String raw) {
        HormoneEducation h = new HormoneEducation();
        try {
            JSONObject json = new JSONObject(cleanJson(raw));
            h.name = json.optString("name", "Unknown Hormone");
            h.type = json.optString("type", "General Health");
            h.description = json.optString("description", "A key chemical in the body.");
            h.url = json.optString("url", "https://www.healthline.com");
        } catch (Exception e) {
            h.name = "Error loading";
            h.description = "Could not parse AI response.";
        }
        return h;
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        String clean = raw.trim();
        int start = clean.indexOf("{");
        int end = clean.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return clean.substring(start, end + 1);
        }
        return clean;
    }
}
