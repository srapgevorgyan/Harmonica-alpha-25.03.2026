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
    private static final int DB_VERSION = 1;

    public MoodDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table to store date and score
        db.execSQL("CREATE TABLE moods (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, score INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}

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
            do {
                scores.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return scores;
    }
}