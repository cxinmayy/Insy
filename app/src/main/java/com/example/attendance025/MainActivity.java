package com.example.attendance025;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;

import android.animation.Animator;
import android.view.ViewAnimationUtils;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    SubjectAdapter adapter;
    List<Subject> subjectList;
    BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerSubjects);
        MaterialButton fab = findViewById(R.id.btnAddSubject);
        ImageView btnDarkMode = findViewById(R.id.btnDarkMode);
        View overlay = findViewById(R.id.themeOverlay);
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.mainScrollView);

        if (scrollView != null) {
            scrollView.setOnScrollChangeListener(new androidx.core.widget.NestedScrollView.OnScrollChangeListener() {
                boolean isFabVisible = true;
                @Override
                public void onScrollChange(androidx.core.widget.NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollY > oldScrollY && isFabVisible) {
                        // Scrolling down -> hide
                        fab.animate().translationY(fab.getHeight() + 150).setDuration(250).start();
                        isFabVisible = false;
                    } else if (scrollY < oldScrollY && !isFabVisible) {
                        // Scrolling up -> show
                        fab.animate().translationY(0).setDuration(250).start();
                        isFabVisible = true;
                    }
                }
            });
        }

        if (isDark) {
            btnDarkMode.setImageResource(R.drawable.ic_sun);
        } else {
            btnDarkMode.setImageResource(R.drawable.ic_moon);
        }

        btnDarkMode.setOnClickListener(v -> {
            boolean newMode = !isDark;

            // Animate Icon 
            btnDarkMode.animate()
                    .rotationBy(180)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(210)
                    .withEndAction(() -> {
                        btnDarkMode.setImageResource(newMode ? R.drawable.ic_sun : R.drawable.ic_moon);
                        btnDarkMode.animate()
                                .rotationBy(180)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(210)
                                .start();
                    })
                    .start();

            overlay.setBackgroundColor(newMode ? Color.BLACK : Color.WHITE);

            int cx = (int) (btnDarkMode.getX() + btnDarkMode.getWidth() / 2);
            int cy = (int) (btnDarkMode.getY() + btnDarkMode.getHeight() / 2);

            int finalRadius = (int) Math.hypot(
                    findViewById(android.R.id.content).getWidth(),
                    findViewById(android.R.id.content).getHeight()
            );

            overlay.setVisibility(View.VISIBLE);

            Animator reveal = ViewAnimationUtils.createCircularReveal(
                    overlay, cx, cy, 0, finalRadius
            );

            reveal.setDuration(420);

            reveal.addListener(new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animation) {}
                @Override public void onAnimationCancel(Animator animation) {}
                @Override public void onAnimationRepeat(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("dark_mode", newMode);
                    editor.apply();

                    AppCompatDelegate.setDefaultNightMode(
                            newMode ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO
                    );
                }
            });

            reveal.start();
        });

        subjectList = new ArrayList<>();

        loadSubjects(); // 🔥 load data

        // 🔥 UPDATED ADAPTER (IMPORTANT FIX)
        adapter = new SubjectAdapter(
                subjectList,
                subject -> {
                    Intent intent = new Intent(MainActivity.this, SubjectDetailActivity.class);
                    intent.putExtra("subjectName", subject.getName());
                    startActivity(intent);
                },
                () -> {
                    // 🔥 THIS FIXES YOUR ISSUE
                    loadSubjects();
                    setupChart();
                }
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        fab.setOnClickListener(v -> showAddSubjectDialog());

        setupChart();
    }

    // 🔥 NEW METHOD
    private void loadSubjects() {

        subjectList.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM subjects", null);

        while (cursor.moveToNext()) {

            String name = cursor.getString(1);

            int initialAttended = cursor.getInt(2);
            int initialTotal = cursor.getInt(3);

            int calendarPresent = dbHelper.getPresentCount(name);
            int calendarTotal = dbHelper.getTotalCount(name);

            int finalAttended = initialAttended + calendarPresent;
            int finalTotal = initialTotal + calendarTotal;

            subjectList.add(new Subject(name, finalAttended, finalTotal));
        }

        cursor.close();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddSubjectDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_subject, null);

        EditText etName = view.findViewById(R.id.etSubjectName);
        EditText etAttended = view.findViewById(R.id.etAttended);
        EditText etTotal = view.findViewById(R.id.etTotal);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(btn -> dialog.dismiss());
        view.findViewById(R.id.btnAdd).setOnClickListener(btn -> {
            String name = etName.getText().toString();
            int attended = Integer.parseInt(etAttended.getText().toString());
            int total = Integer.parseInt(etTotal.getText().toString());

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            db.execSQL("INSERT INTO subjects (name, attended, total) VALUES (?, ?, ?)",
                    new Object[]{name, attended, total});

            loadSubjects();   // 🔥 refresh list
            setupChart();     // 🔥 refresh graph
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadSubjects();
        setupChart();
    }

    private void setupChart() {
        android.widget.LinearLayout layoutChartPills = findViewById(R.id.layoutChartPills);
        if (layoutChartPills == null) return;
        
        layoutChartPills.removeAllViews();

        for (int i = 0; i < subjectList.size(); i++) {
            Subject subject = subjectList.get(i);
            int percentage = subject.getPercentage();

            View pillView = LayoutInflater.from(this).inflate(R.layout.item_performance, layoutChartPills, false);

            android.widget.TextView tvPerfPercentage = pillView.findViewById(R.id.tvPerfPercentage);
            android.widget.TextView tvPerfSubject = pillView.findViewById(R.id.tvPerfSubject);
            View viewChartEmpty = pillView.findViewById(R.id.viewChartEmpty);
            View viewChartFill = pillView.findViewById(R.id.viewChartFill);

            tvPerfPercentage.setText(percentage + ".0");
            tvPerfSubject.setText(subject.getName());

            int emptyWeight = 100 - percentage;
            if (emptyWeight < 0) emptyWeight = 0;
            if (percentage > 100) percentage = 100;

            android.widget.LinearLayout.LayoutParams emptyParams = (android.widget.LinearLayout.LayoutParams) viewChartEmpty.getLayoutParams();
            emptyParams.weight = emptyWeight;
            viewChartEmpty.setLayoutParams(emptyParams);

            android.widget.LinearLayout.LayoutParams fillParams = (android.widget.LinearLayout.LayoutParams) viewChartFill.getLayoutParams();
            fillParams.weight = percentage;
            viewChartFill.setLayoutParams(fillParams);

            layoutChartPills.addView(pillView);
        }
    }
}