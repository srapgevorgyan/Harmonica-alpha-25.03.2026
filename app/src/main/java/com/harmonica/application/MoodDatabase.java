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
    private static final int DB_VERSION = 2;

    public MoodDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static class MoodEntry {
        public int score;
        public long timestamp;
        public MoodEntry(int score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE moods (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, score INTEGER)");
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

    public long createSession(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("timestamp", System.currentTimeMillis());
        return db.insert("sessions", null, v);
    }

    public void saveMessage(long sessionId, String sender, String text) {
        if (sessionId == -2) return; // -2 is reserved for Incognito Mode
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("sessionId", sessionId);
        v.put("sender", sender);
        v.put("text", text);
        v.put("timestamp", System.currentTimeMillis());
        db.insert("messages", null, v);
    }

    public Cursor getMessages(long sessionId) {
        return this.getReadableDatabase().rawQuery("SELECT * FROM messages WHERE sessionId = ? ORDER BY timestamp ASC", new String[]{String.valueOf(sessionId)});
    }

    public void saveMood(int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", String.valueOf(System.currentTimeMillis()));
        values.put("score", score);
        db.insert("moods", null, values);
    }

    public List<MoodEntry> getRecentMoodEntries() {
        return getMoodEntries(7);
    }

    public List<MoodEntry> getMonthMoodEntries() {
        return getMoodEntries(30);
    }

    private List<MoodEntry> getMoodEntries(int limit) {
        List<MoodEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT score, date FROM moods ORDER BY id DESC LIMIT ?", new String[]{String.valueOf(limit)});

        if (cursor.moveToFirst()) {
            do {
                entries.add(new MoodEntry(
                        cursor.getInt(0),
                        Long.parseLong(cursor.getString(1))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

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
        
        String query = "SELECT s.id, s.title, s.timestamp, " +
                "CASE " +
                "WHEN date(s.timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime') THEN 'Today' " +
                "WHEN date(s.timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime', '-1 day') THEN 'Yesterday' " +
                "ELSE 'Previous' END as category " +
                "FROM sessions s " +
                "WHERE EXISTS (SELECT 1 FROM messages m WHERE m.sessionId = s.id) " +
                "ORDER BY s.timestamp DESC";

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

    public void updateSessionTitle(long id, String newTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", newTitle);
        db.update("sessions", v, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteSession(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("messages", "sessionId = ?", new String[]{String.valueOf(id)});
        db.delete("sessions", "id = ?", new String[]{String.valueOf(id)});
    }
}