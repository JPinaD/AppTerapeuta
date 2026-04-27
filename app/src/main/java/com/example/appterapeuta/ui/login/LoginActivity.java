package com.example.appterapeuta.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.appterapeuta.R;
import com.example.appterapeuta.SessionManager;
import com.example.appterapeuta.ui.dashboard.MainDashboardActivity;
import com.example.appterapeuta.ui.therapists.TherapistManagementActivity;
import com.example.appterapeuta.viewmodel.LoginViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextInputEditText etUsername = findViewById(R.id.username);
        TextInputEditText etPassword = findViewById(R.id.password);
        TextView tvError = findViewById(R.id.tvLoginError);

        LoginViewModel vm = new ViewModelProvider(this).get(LoginViewModel.class);

        vm.getLoginResult().observe(this, result -> {
            if (result == LoginViewModel.LoginResult.SUCCESS) {
                SessionManager.getInstance().login(
                        etUsername.getText().toString().trim(), false);
                startActivity(new Intent(this, MainDashboardActivity.class));
                finish();
            } else if (result == LoginViewModel.LoginResult.SUCCESS_ROOT) {
                SessionManager.getInstance().login(
                        etUsername.getText().toString().trim(), true);
                startActivity(new Intent(this, TherapistManagementActivity.class));
                finish();
            } else {
                tvError.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.login_button).setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            String user = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString() : "";
            if (user.isEmpty() || pass.isEmpty()) {
                tvError.setText("Completa todos los campos");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            vm.login(user, pass);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
