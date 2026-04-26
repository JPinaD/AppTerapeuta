package com.example.appterapeuta.ui.therapists;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appterapeuta.R;
import com.example.appterapeuta.data.local.entity.TherapistEntity;
import com.example.appterapeuta.data.repository.TherapistRepository;
import com.example.appterapeuta.ui.dashboard.MainDashboardActivity;

public class TherapistManagementActivity extends AppCompatActivity {

    private TherapistRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapist_management);

        repository = new TherapistRepository(this);

        RecyclerView rv = findViewById(R.id.rvTherapists);
        rv.setLayoutManager(new LinearLayoutManager(this));
        TherapistAdapter adapter = new TherapistAdapter(new TherapistAdapter.Listener() {
            @Override
            public void onChangePassword(TherapistEntity t) { showChangePasswordDialog(t); }
            @Override
            public void onDelete(TherapistEntity t) { showDeleteDialog(t); }
        });
        rv.setAdapter(adapter);

        repository.getAll().observe(this, adapter::setItems);

        findViewById(R.id.btnAddTherapist).setOnClickListener(v -> showAddDialog());

        // Root puede entrar al dashboard también
        findViewById(R.id.back_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainDashboardActivity.class);
            intent.putExtra("is_root", true);
            startActivity(intent);
            finish();
        });
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        EditText etName = new EditText(this); etName.setHint("Nombre completo"); layout.addView(etName);
        EditText etUser = new EditText(this); etUser.setHint("Usuario"); layout.addView(etUser);
        EditText etPass = new EditText(this); etPass.setHint("Contraseña");
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPass);

        new AlertDialog.Builder(this)
                .setTitle("Añadir terapeuta")
                .setView(layout)
                .setPositiveButton("Añadir", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String user = etUser.getText().toString().trim();
                    String pass = etPass.getText().toString();
                    if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.insert(user, name, pass);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showChangePasswordDialog(TherapistEntity t) {
        EditText etPass = new EditText(this);
        etPass.setHint("Nueva contraseña");
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etPass.setPadding(pad, pad, pad, 0);

        new AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña — " + t.displayName)
                .setView(etPass)
                .setPositiveButton("Guardar", (d, w) -> {
                    String pass = etPass.getText().toString();
                    if (pass.isEmpty()) {
                        Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.changePassword(t.username, pass);
                    Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteDialog(TherapistEntity t) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar terapeuta")
                .setMessage("¿Eliminar a " + t.displayName + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> repository.delete(t))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
