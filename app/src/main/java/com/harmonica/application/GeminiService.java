package com.harmonica.application;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class GeminiService {
    private final String API_KEY_DEFAULT = "AIzaSyAOgWzU3wAfLWgW6ENRV1O30wiy68GzlDU";
    private GenerativeModelFutures model;

    public static class MoodAnalysis {
        public int score = 5;
        public String label = "Neutral";
        public String insight = "";
        public String advice = "";
        public String chatTitle = "New Conversation";
        public String suggestedMode = "None";
        public List<Practice> suggestedPractices = new ArrayList<>();
    }

    public static class Practice {
        public String title;
        public String type;
        public String instruction;
        
        public Practice(String title, String type, String instruction) {
            this.title = title;
            this.type = type;
            this.instruction = instruction;
        }
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

    public GeminiService(Context context) {
        initModel(context);
    }

    private void initModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("HarmonicaPrefs", Context.MODE_PRIVATE);
        String customKey = prefs.getString("gemini_api_key", null);
        String activeKey = (customKey != null && !customKey.isEmpty()) ? customKey : API_KEY_DEFAULT;

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.7f; 
        configBuilder.responseMimeType = "application/json";
        GenerationConfig config = configBuilder.build();

        GenerativeModel baseModel = new GenerativeModel(
                "gemini-2.5-flash",
                activeKey,
                config,
                new ArrayList<>(),
                new RequestOptions()
        );

        this.model = GenerativeModelFutures.from(baseModel);
    }

    public void analyzeMood(List<MessageAdapter.Message> history, String userText, AnalysisCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are Dr. Harmonica, a compassionate, professional psychologist. ")
                .append("RULES OF CONDUCT:\n")
                .append("- Speak DIRECTLY to the user in the 1st person ('I' and 'you').\n")
                .append("- NEVER use 3rd person (e.g., 'The user is feeling...'). This is a direct therapy session.\n")
                .append("- Be warm and empathetic. Validate their feelings.\n")
                .append("- CRITICAL: Keep responses EXTREMELY concise. Users in distress cannot read long paragraphs.\n")
                .append("- Limit your empathetic response to 1 to 2 short sentences maximum\n")
                .append("- In case user asks for advise, give brief actionable list\n")
                .append("- And ALWAYS after giving a message, give a contiuation or follow-up question\n")
                .append("- Format actionable advice as a short bulleted list using Markdown (-).\n")
                .append("- Integrate therapeutic techniques naturally into your response.\n")
                .append("- If you detect stress, anxiety, panic, or low mood, explicitly suggest they visit the 'Zen Space' in the sidebar menu for guided breathing or grounding.\n\n")
                .append("THERAPEUTIC FRAMEWORKS TO UTILIZE:\n")
                .append("- Grounding (Anxiety/Panic): Guide them through the 5-4-3-2-1 method or box breathing.\n")
                .append("- CBT (Depression/Anxiety): Gently point out cognitive distortions (like catastrophizing) and encourage reframing negative thoughts.\n")
                .append("- DBT (Severe Distress): Suggest T.I.P.P. skills (like splashing cold water on their face) to reset their nervous system.\n")
                .append("- Behavioral Activation (Burnout/Depression): Suggest micro-goals. Break tasks down into ridiculously small, manageable steps.\n\n")
                .append("JSON SCHEMA (MANDATORY):\n")
                .append("{ \n")
                .append("  \"score\": number, \n")
                .append("  \"label\": string, \n")
                .append("  \"insight\": \"Your direct, empathetic response to the user...\", \n")
                .append("  \"advice\": \"Practical steps they can take right now...\", \n")
                .append("  \"chatTitle\": \"Short session title\", \n")
                .append("  \"suggestedMode\": \"None\" | \"Calm\" | \"Focus\" | \"Elevate\" | \"Crisis\", \n")
                .append("  \"suggestedPractices\": [ { \"title\": \"Friendly Name\", \"type\": \"Breathing\"|\"Grounding\", \"instruction\": \"Steps\" } ] \n")
                .append("}\n\n")
                .append("SESSION HISTORY:\n");

        for (MessageAdapter.Message msg : history) {
            if (msg.isTyping) continue;
            String role = msg.sender.equals("user") ? "User" : "Dr. Harmonica";
            promptBuilder.append(role).append(": ").append(msg.text).append("\n");
        }
        promptBuilder.append("User: ").append(userText).append("\n\n")
                .append("Dr. Harmonica (Respond in JSON):");

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
        promptBuilder.append("Pick a unique hormone. ")
                .append("Exclude: ").append(String.join(", ", excludeList)).append(". ")
                .append("Provide a search link in the 'url' field. ")
                .append("Respond in JSON: { \"name\", \"type\", \"description\", \"url\" }");

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
            m.suggestedMode = json.optString("suggestedMode", "None");
            
            JSONArray practices = json.optJSONArray("suggestedPractices");
            if (practices != null) {
                for (int i = 0; i < practices.length(); i++) {
                    JSONObject p = practices.getJSONObject(i);
                    m.suggestedPractices.add(new Practice(
                        p.optString("title"),
                        p.optString("type"),
                        p.optString("instruction")
                    ));
                }
            }
        } catch (Exception e) {
            m.insight = (raw != null) ? raw : "I'm listening.";
        }
        return m;
    }

    private HormoneEducation parseHormoneResponse(String raw) {
        HormoneEducation h = new HormoneEducation();
        try {
            JSONObject json = new JSONObject(cleanJson(raw));
            h.name = json.optString("name", "Unknown Hormone");
            h.type = json.optString("type", "General Health");
            h.description = json.optString("description", "A key chemical.");
            h.url = json.optString("url", "https://www.healthline.com");
        } catch (Exception e) {
            h.name = "Error";
            h.description = "Could not parse response.";
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
