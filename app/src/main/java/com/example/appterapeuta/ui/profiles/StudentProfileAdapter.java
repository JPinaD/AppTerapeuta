package com.example.appterapeuta.ui.profiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;

import java.util.ArrayList;
import java.util.List;

public class StudentProfileAdapter extends RecyclerView.Adapter<StudentProfileAdapter.StudentViewHolder> {

    private final List<StudentProfileEntity> students = new ArrayList<>();

    public void setStudents(List<StudentProfileEntity> list) {
        students.clear();
        if (list != null) students.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_profile, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentProfileEntity student = students.get(position);
        holder.tvStudentName.setText(student.name);
        holder.tvStudentAvatar.setText(student.avatar);
        holder.tvStudentNeeds.setText(student.educationalNeeds);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentAvatar, tvStudentNeeds;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName  = itemView.findViewById(R.id.student_name);
            tvStudentAvatar = itemView.findViewById(R.id.student_avatar);
            tvStudentNeeds = itemView.findViewById(R.id.student_needs);
        }
    }
}
