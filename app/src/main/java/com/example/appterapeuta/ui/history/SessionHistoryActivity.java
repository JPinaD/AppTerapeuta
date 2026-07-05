package com.example.appterapeuta.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.util.ExportManager;
import com.example.appterapeuta.viewmodel.SessionHistoryViewModel;

public class SessionHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_history);

        RecyclerView rv = findViewById(R.id.rvSessions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SessionHistoryAdapter adapter = new SessionHistoryAdapter(sessionId -> {
            Intent intent = new Intent(this, SessionResultDetailActivity.class);
            intent.putExtra("session_id", sessionId);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        SessionHistoryViewModel vm = new ViewModelProvider(this).get(SessionHistoryViewModel.class);
        vm.sessions.observe(this, adapter::setSessions);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // CSV export button
        Button btnExportCsv = findViewById(R.id.btnExportCsv);
        btnExportCsv.setOnClickListener(v -> {
            btnExportCsv.setEnabled(false);
            btnExportCsv.setText("...");
            ExportManager.exportCSV(this, new ExportManager.ExportCallback() {
                @Override
                public void onSuccess(java.io.File file) {
                    runOnUiThread(() -> {
                        btnExportCsv.setEnabled(true);
                        btnExportCsv.setText("CSV");
                        Toast.makeText(SessionHistoryActivity.this,
                                "CSV exportado correctamente", Toast.LENGTH_SHORT).show();
                        Intent shareIntent = ExportManager.createShareIntent(
                                SessionHistoryActivity.this, file, "text/csv");
                        startActivity(shareIntent);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        btnExportCsv.setEnabled(true);
                        btnExportCsv.setText("CSV");
                        Toast.makeText(SessionHistoryActivity.this,
                                message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
