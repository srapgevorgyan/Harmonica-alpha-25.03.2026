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
    private static final int DB_VERSION = 3;

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
        db.execSQL("CREATE TABLE moods (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, score INTEGER, userId TEXT)");
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, timestamp LONG, userId TEXT)");
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sessionId INTEGER, sender TEXT, text TEXT, timestamp LONG)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, timestamp LONG)");
            db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sessionId INTEGER, sender TEXT, text TEXT, timestamp LONG)");
        }
        if (oldV < 3) {
            addColumnIfNotExists(db, "moods", "userId", "TEXT");
            addColumnIfNotExists(db, "sessions", "userId", "TEXT");
        }
    }

    private void addColumnIfNotExists(SQLiteDatabase db, String table, String column, String type) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            boolean exists = false;
            while (cursor.moveToNext()) {
                if (cursor.getString(1).equals(column)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        } catch (Exception ignored) {}
    }

    public long createSession(String title, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("timestamp", System.currentTimeMillis());
        v.put("userId", userId);
        return db.insert("sessions", null, v);
    }

    public void saveMessage(long sessionId, String sender, String text) {
        if (sessionId == -2) return;
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

    public void saveMood(int score, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", String.valueOf(System.currentTimeMillis()));
        values.put("score", score);
        values.put("userId", userId);
        db.insert("moods", null, values);
    }

    public List<MoodEntry> getRecentMoodEntries(String userId) {
        return getMoodEntries(7, userId);
    }

    public List<MoodEntry> getMonthMoodEntries(String userId) {
        return getMoodEntries(30, userId);
    }

    private List<MoodEntry> getMoodEntries(int limit, String userId) {
        List<MoodEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query;
        String[] args;
        if (userId == null) {
            query = "SELECT score, date FROM moods WHERE userId IS NULL ORDER BY id DESC LIMIT ?";
            args = new String[]{String.valueOf(limit)};
        } else {
            query = "SELECT score, date FROM moods WHERE userId = ? ORDER BY id DESC LIMIT ?";
            args = new String[]{userId, String.valueOf(limit)};
        }
        
        try (Cursor cursor = db.rawQuery(query, args)) {
            if (cursor.moveToFirst()) {
                do {
                    entries.add(new MoodEntry(
                            cursor.getInt(0),
                            Long.parseLong(cursor.getString(1))
                    ));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return entries;
    }

    public static class SessionHeader {
        public long id;
        public String title, category;
        public SessionHeader(long id, String title, String category) {
            this.id = id; this.title = title; this.category = category;
        }
    }

    public List<SessionHeader> getCategorizedSessions(String userId) {
        List<SessionHeader> headers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String userCondition = (userId == null) ? "s.userId IS NULL" : "s.userId = ?";
        String query = "SELECT s.id, s.title, s.timestamp, " +
                "CASE " +
                "WHEN date(s.timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime') THEN 'Today' " +
                "WHEN date(s.timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime', '-1 day') THEN 'Yesterday' " +
                "ELSE 'Previous' END as category " +
                "FROM sessions s " +
                "WHERE " + userCondition + " " +
                "AND EXISTS (SELECT 1 FROM messages m WHERE m.sessionId = s.id) " +
                "ORDER BY s.timestamp DESC";

        String[] args = (userId == null) ? null : new String[]{userId};
        
        try (Cursor cursor = db.rawQuery(query, args)) {
            if (cursor.moveToFirst()) {
                do {
                    headers.add(new SessionHeader(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(3)
                    ));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
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