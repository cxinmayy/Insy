package com.example.attendance025;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "attendance.db";
    public static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE subjects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "attended INTEGER," +
                "total INTEGER)");

        db.execSQL("CREATE TABLE attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subjectName TEXT," +
                "date TEXT," +
                "status TEXT," +
                "UNIQUE(subjectName, date))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For future database updates
    }

    // 🔹 Count Present Classes
    public int getPresentCount(String subjectName) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM attendance WHERE subjectName=? AND status='Present'",
                new String[]{subjectName});

        int count = 0;

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    // 🔹 Count Total Classes
    public int getTotalCount(String subjectName) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM attendance WHERE subjectName=?",
                new String[]{subjectName});

        int count = 0;

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }
    public void deleteSubject(String subjectName) {

        SQLiteDatabase db = this.getWritableDatabase();

        // Delete from subjects table
        db.delete("subjects", "name=?", new String[]{subjectName});

        // 🔥 IMPORTANT: Delete related attendance records
        db.delete("attendance", "subjectName=?", new String[]{subjectName});

        db.close();
    }
}