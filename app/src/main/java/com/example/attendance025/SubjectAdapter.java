package com.example.attendance025;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    List<Subject> subjectList;
    OnSubjectClickListener listener;
    OnDataChangedListener dataChangedListener;

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    // 🔥 NEW INTERFACE
    public interface OnDataChangedListener {
        void onDataChanged();
    }

    // 🔥 UPDATED CONSTRUCTOR
    public SubjectAdapter(List<Subject> subjectList,
                          OnSubjectClickListener listener,
                          OnDataChangedListener dataChangedListener) {
        this.subjectList = subjectList;
        this.listener = listener;
        this.dataChangedListener = dataChangedListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvAttendance, tvPercentage, tvPrediction;
        ProgressBar progressAttendance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvAttendance = itemView.findViewById(R.id.tvAttendance);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            tvPrediction = itemView.findViewById(R.id.tvPrediction);
            progressAttendance = itemView.findViewById(R.id.progressAttendance);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Subject subject = subjectList.get(position);

        int attended = subject.getAttended();
        int total = subject.getTotal();
        int percentage = subject.getPercentage();

        holder.tvName.setText(subject.getName());
        holder.tvAttendance.setText(attended + " / " + total);
        holder.tvPercentage.setText(percentage + "%");

        holder.progressAttendance.setProgress(percentage);

        int progressColor;
        if (percentage < 75) {
            progressColor = holder.itemView.getContext().getResources().getColor(R.color.progress_red, null);
        } else {
            progressColor = holder.itemView.getContext().getResources().getColor(R.color.progress_green, null);
        }
        
        holder.progressAttendance.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));

        String prediction;

        if (percentage >= 75) {
            int bunk = (int) Math.floor((attended / 0.75) - total);
            if (bunk > 0) {
                prediction = "You can miss " + bunk + " classes";
            } else {
                prediction = "Attendance safe";
            }
        } else {
            int need = (int) Math.ceil((0.75 * total - attended) / (1 - 0.75));
            prediction = "Attend " + need + " classes to reach 75%";
        }

        holder.tvPrediction.setText(prediction);

        holder.itemView.setOnClickListener(v -> {
            listener.onSubjectClick(subject);
        });

        holder.itemView.setOnLongClickListener(v -> {

            String[] options = {"Edit Subject", "Delete Subject"};

            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle(subject.getName())
                    .setItems(options, (dialog, which) -> {

                        DatabaseHelper dbHelper = new DatabaseHelper(v.getContext());
                        SQLiteDatabase db = dbHelper.getWritableDatabase();

                        int pos = holder.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;

                        if (which == 1) { // DELETE

                            db.execSQL("DELETE FROM subjects WHERE name=?",
                                    new Object[]{subject.getName()});

                            db.execSQL("DELETE FROM attendance WHERE subjectName=?",
                                    new Object[]{subject.getName()});

                            subjectList.remove(pos);
                            notifyItemRemoved(pos);

                            // 🔥 IMPORTANT FIX
                            if (dataChangedListener != null) {
                                dataChangedListener.onDataChanged();
                            }
                        }

                        if (which == 0) { // EDIT

                            View view = LayoutInflater.from(v.getContext())
                                    .inflate(R.layout.dialog_edit_subject, null);

                            android.widget.EditText etName = view.findViewById(R.id.etEditName);
                            android.widget.EditText etAttended = view.findViewById(R.id.etEditAttended);
                            android.widget.EditText etTotal = view.findViewById(R.id.etEditTotal);

                            etName.setText(subject.getName());
                            etAttended.setText(String.valueOf(subject.getAttended()));
                            etTotal.setText(String.valueOf(subject.getTotal()));

                            androidx.appcompat.app.AlertDialog editDialog = new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                                    .setView(view)
                                    .create();

                            if (editDialog.getWindow() != null) {
                                editDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                            }

                            view.findViewById(R.id.btnCancelEdit).setOnClickListener(btn -> editDialog.dismiss());
                            view.findViewById(R.id.btnSaveEdit).setOnClickListener(btn -> {
                                String oldName = subject.getName();
                                String newName = etName.getText().toString().trim();

                                int newAttended = Integer.parseInt(etAttended.getText().toString());
                                int newTotal = Integer.parseInt(etTotal.getText().toString());

                                db.execSQL(
                                        "UPDATE subjects SET name=?, attended=?, total=? WHERE name=?",
                                        new Object[]{newName, newAttended, newTotal, oldName}
                                );

                                db.execSQL(
                                        "UPDATE attendance SET subjectName=? WHERE subjectName=?",
                                        new Object[]{newName, oldName}
                                );

                                subjectList.set(pos, new Subject(newName, newAttended, newTotal));
                                notifyItemChanged(pos);

                                // 🔥 ALSO UPDATE CHART AFTER EDIT
                                if (dataChangedListener != null) {
                                    dataChangedListener.onDataChanged();
                                }
                                editDialog.dismiss();
                            });
                            
                            editDialog.show();
                        }

                    })
                    .show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return subjectList.size();
    }
}