package com.example.appterapeuta.ui.profiles;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.StudentProfile;
import com.example.appterapeuta.util.MockDataProvider;

import java.util.List;

public class StudentProfileListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile_list);

        RecyclerView recyclerView = findViewById(R.id.student_list);
        List<StudentProfile> profiles = MockDataProvider.getMockStudentProfiles();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new StudentProfileAdapter(profiles));


        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
