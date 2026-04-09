package com.example.attendance025;

import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.google.android.material.button.MaterialButton;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.HashSet;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

public class SubjectDetailActivity extends AppCompatActivity {

    TextView tvTitle;
    TextView tvPresentCount, tvAbsentCount, tvTotalCount;
    TextView tvAttendancePct;
    android.widget.ProgressBar progressBar;

    MaterialCalendarView calendarView;

    HashSet<CalendarDay> presentDates = new HashSet<>();
    HashSet<CalendarDay> absentDates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detail);

        tvTitle = findViewById(R.id.tvSubjectTitle);
        calendarView = findViewById(R.id.calendarView);

        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvAttendancePct = findViewById(R.id.tvAttendancePct);
        progressBar = findViewById(R.id.progressBar);

        String subjectName = getIntent().getStringExtra("subjectName");
        tvTitle.setText(subjectName);

        refreshCalendar();
        updateStats();

        calendarView.setOnDateChangedListener((widget, date, selected) -> {

            // 🔥 HAPTIC FEEDBACK
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(30);

            // 🔥 SCALE + FADE ANIMATION
            calendarView.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .alpha(0.85f)
                    .setDuration(80)
                    .withEndAction(() -> {

                        calendarView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f)
                                .setDuration(100)
                                .start();

                        // Open dialog AFTER animation
                        showAttendanceDialog(date);
                    })
                    .start();
        });


    }

    private void showAttendanceDialog(CalendarDay date) {

        String[] options = {"Present", "Absent", "Clear"};

        new AlertDialog.Builder(this)
                .setTitle("Mark Attendance")
                .setItems(options, (dialog, which) -> {

                    DatabaseHelper dbHelper = new DatabaseHelper(this);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    int year = date.getYear();
                    int month = date.getMonth() + 1;
                    int day = date.getDay();

                    String dateStr = year + "-"
                            + (month < 10 ? "0" + month : month) + "-"
                            + (day < 10 ? "0" + day : day);

                    String subjectName = tvTitle.getText().toString().trim(); // 🔥 FIX

                    if (which == 0) { // Present

                        db.execSQL(
                                "INSERT OR REPLACE INTO attendance (subjectName, date, status) VALUES (?, ?, ?)",
                                new Object[]{subjectName, dateStr, "Present"}
                        );

                    } else if (which == 1) { // Absent

                        db.execSQL(
                                "INSERT OR REPLACE INTO attendance (subjectName, date, status) VALUES (?, ?, ?)",
                                new Object[]{subjectName, dateStr, "Absent"}
                        );

                    } else if (which == 2) { // Clear

                        db.execSQL(
                                "DELETE FROM attendance WHERE LOWER(TRIM(subjectName)) = LOWER(TRIM(?)) AND date=?",
                                new Object[]{subjectName, dateStr}
                        );
                    }

                    // 🔥 DEBUG (you can remove later)
                    Cursor check = db.rawQuery("SELECT subjectName, date, status FROM attendance", null);
                    while (check.moveToNext()) {
                        System.out.println("DB -> "
                                + check.getString(0) + " | "
                                + check.getString(1) + " | "
                                + check.getString(2));
                    }
                    check.close();

                    refreshCalendar();
                    updateStats();

// 🔥 SMOOTH REFRESH ANIMATION
                    calendarView.animate()
                            .alpha(0.6f)
                            .setDuration(100)
                            .withEndAction(() ->
                                    calendarView.animate()
                                            .alpha(1f)
                                            .setDuration(150)
                                            .start()
                            ).start();
                })
                .show();
    }


    private void refreshCalendar() {

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        presentDates.clear();
        absentDates.clear();

        String subjectName = tvTitle.getText().toString();

        Cursor cursor = db.rawQuery(
                "SELECT date, status FROM attendance WHERE subjectName=?",
                new String[]{subjectName}
        );

        while (cursor.moveToNext()) {

            String dateStr = cursor.getString(0);
            String status = cursor.getString(1);

            String[] parts = dateStr.split("-");

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);

            CalendarDay calendarDay = CalendarDay.from(year, month, day);

            if ("Present".equals(status)) {
                presentDates.add(calendarDay);
            } else if ("Absent".equals(status)) {
                absentDates.add(calendarDay);
            }
        }

        cursor.close();

        calendarView.removeDecorators();

        calendarView.addDecorator(new EventDecorator(Color.parseColor("#FDCBF2"), presentDates)); // soft purple/pink scholarly
        calendarView.addDecorator(new EventDecorator(Color.parseColor("#F97386"), absentDates));  // soft red scholarly

        calendarView.addDecorator(new TodayDecorator());
    }


    private void updateStats() {

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String subjectName = tvTitle.getText().toString();

        int initialPresent = 0;
        int initialTotal = 0;

        // Get initial data from subjects table
        Cursor subjectCursor = db.rawQuery(
                "SELECT attended, total FROM subjects WHERE name=?",
                new String[]{subjectName}
        );

        if (subjectCursor.moveToFirst()) {
            initialPresent = subjectCursor.getInt(0);
            initialTotal = subjectCursor.getInt(1);
        }
        subjectCursor.close();

        int calendarPresent = 0;
        int calendarAbsent = 0;

        // Get calendar attendance
        Cursor attendanceCursor = db.rawQuery(
                "SELECT status FROM attendance WHERE subjectName=?",
                new String[]{subjectName}
        );

        while (attendanceCursor.moveToNext()) {

            String status = attendanceCursor.getString(0);

            if ("Present".equals(status)) {
                calendarPresent++;
            } else if ("Absent".equals(status)) {
                calendarAbsent++;
            }
        }

        attendanceCursor.close();

        int finalPresent = initialPresent + calendarPresent;
        int finalTotal = initialTotal + calendarPresent + calendarAbsent;
        int finalAbsent = finalTotal - finalPresent;

        tvPresentCount.setText(String.valueOf(finalPresent));
        tvAbsentCount.setText(String.valueOf(finalAbsent));
        tvTotalCount.setText(String.valueOf(finalTotal));

        // Update Percentage Layout
        int percentage = 0;
        if (finalTotal > 0) {
            percentage = (int) (((double) finalPresent / finalTotal) * 100);
        }
        
        if (tvAttendancePct != null) {
            tvAttendancePct.setText(percentage + "%");
        }
        if (progressBar != null) {
            progressBar.setProgress(percentage);
        }
    }


}