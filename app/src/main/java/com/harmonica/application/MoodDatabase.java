package com.harmonica.application;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class MoodDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "HarmonicaDB";
    private static final int DB_VERSION = 2; // Incremented version

    public MoodDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE moods (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, score INTEGER)");

        // New Tables for Gemini-style history
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, timestamp LONG)");
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sessionId INTEGER, sender TEXT, text TEXT, timestamp LONG)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, timestamp LONG)");
            db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sessionId INTEGER, sender TEXT, text TEXT, timestamp LONG)");
        }
    }

    // --- Session Methods ---
    public long createSession(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("timestamp", System.currentTimeMillis());
        return db.insert("sessions", null, v);
    }

    public Cursor getSessions() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM sessions ORDER BY timestamp DESC", null);
    }

    // --- Message Methods ---
    public void saveMessage(long sessionId, String sender, String text) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("sessionId", sessionId);
        v.put("sender", sender); // "user" or "ai"
        v.put("text", text);
        v.put("timestamp", System.currentTimeMillis());
        db.insert("messages", null, v);
    }

    public Cursor getMessages(long sessionId) {
        return this.getReadableDatabase().rawQuery("SELECT * FROM messages WHERE sessionId = ? ORDER BY timestamp ASC", new String[]{String.valueOf(sessionId)});
    }

    // Keep your old mood methods for the graph...
    public void saveMood(int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", String.valueOf(System.currentTimeMillis()));
        values.put("score", score);
        db.insert("moods", null, values);
    }

    public List<Integer> getRecentScores() {
        List<Integer> scores = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT score FROM moods ORDER BY id DESC LIMIT 7", null);
        if (cursor.moveToFirst()) {
            do { scores.add(cursor.getInt(0)); } while (cursor.moveToNext());
        }
        cursor.close();
        return scores;
    }


    // saved chats logic

    public static class SessionHeader {
        public long id;
        public String title, category;
        public SessionHeader(long id, String title, String category) {
            this.id = id; this.title = title; this.category = category;
        }
    }

    public List<SessionHeader> getCategorizedSessions() {
        List<SessionHeader> headers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // This query calculates the category (Today, Yesterday, Previous) based on the timestamp
        String query = "SELECT id, title, timestamp, " +
                "CASE " +
                "WHEN date(timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime') THEN 'Today' " +
                "WHEN date(timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime', '-1 day') THEN 'Yesterday' " +
                "ELSE 'Previous' END as category " +
                "FROM sessions ORDER BY timestamp DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                headers.add(new SessionHeader(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(3)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return headers;
    }

    // Add this to allow updating the title after the first message
    public void updateSessionTitle(long id, String newTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", newTitle);
        db.update("sessions", v, "id = ?", new String[]{String.valueOf(id)});
    }
}