package com.example.appterapeuta.ui.communicator;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appterapeuta.AppTerapeutaApp;
import com.example.appterapeuta.R;
import com.example.appterapeuta.data.model.ActivityEvent;
import com.example.appterapeuta.util.AppConstants;
import com.example.appterapeuta.viewmodel.RobotViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del terapeuta para enviar pictogramas al alumno
 * y visualizar los mensajes recibidos del alumno.
 */
public class CommunicatorSenderActivity extends AppCompatActivity {

    private static final String TAG = "CommunicatorSender";
    public static final String EXTRA_ROBOT_ID = "robot_id";

    private static final int RESPONSE_DISPLAY_MS = 5000;

    private RobotViewModel robotViewModel;
    private String targetRobotId;

    private LinearLayout layoutComposedPictos;
    private GridLayout gridPictograms;
    private LinearLayout layoutCategories;
    private LinearLayout layoutReceivedMessage;
    private LinearLayout layoutStudentPictos;
    private Button btnSendToStudent;
    private TextView tvStudentResponse;

    private PictogramCatalog.Category currentCategory = PictogramCatalog.Category.NEEDS;
    private final List<String> composition = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideResponseRunnable = () -> {
        if (tvStudentResponse != null) tvStudentResponse.setVisibility(View.GONE);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicator_sender);

        targetRobotId = getIntent().getStringExtra(EXTRA_ROBOT_ID);
        robotViewModel = ((AppTerapeutaApp) getApplication()).getRobotViewModel();

        // Find views
        layoutComposedPictos = findViewById(R.id.layoutComposedPictos);
        gridPictograms = findViewById(R.id.gridPictograms);
        layoutCategories = findViewById(R.id.layoutCategories);
        layoutReceivedMessage = findViewById(R.id.layoutReceivedMessage);
        layoutStudentPictos = findViewById(R.id.layoutStudentPictos);
        btnSendToStudent = findViewById(R.id.btnSendToStudent);
        tvStudentResponse = findViewById(R.id.tvStudentResponse);

        // Close button
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Send button
        btnSendToStudent.setOnClickListener(v -> sendToStudent());

        // Understood / Not understood (terapeuta responds to student's message)
        findViewById(R.id.btnUnderstood).setOnClickListener(v -> respondToStudent(true));
        findViewById(R.id.btnNotUnderstood).setOnClickListener(v -> respondToStudent(false));

        // Build category tabs
        buildCategoryTabs();

        // Load initial grid
        loadCategoryGrid(currentCategory);

        // Update composition display
        updateCompositionBar();

        // Observe incoming communicator messages from students
        // Uses dedicated communicatorEvents channel to avoid loss from ROBOT_STATUS postValue overwrites
        robotViewModel.getCommunicatorEvents().observe(this, this::handleCommunicatorEvent);
    }

    // --- Handle incoming messages ---

    private void handleCommunicatorEvent(ActivityEvent event) {
        if (event == null) return;
        // Only process if from our target robot (or show all if targetRobotId is null)
        if (targetRobotId != null && !targetRobotId.equals(event.robotId)) return;
        if (event.payload == null) return;

        if (AppConstants.MSG_COMMUNICATOR_SEQUENCE.equals(event.type)) {
            handleCommunicatorSequence(event.payload);
        } else if (AppConstants.MSG_STUDENT_PICTOGRAM_RESPONSE.equals(event.type)) {
            handleStudentPictogramResponse(event.payload);
        }
    }

    /**
     * El alumno ha enviado una secuencia de pictogramas (COMMUNICATOR_SEQUENCE).
     */
    private void handleCommunicatorSequence(String payload) {
        try {
            JSONObject obj = new JSONObject(payload);
            JSONArray arr = obj.optJSONArray("pictogramIds");
            if (arr == null) return;

            List<String> ids = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) ids.add(arr.getString(i));
            showReceivedMessage(ids);
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing COMMUNICATOR_SEQUENCE", e);
        }
    }

    /**
     * El alumno ha respondido SÍ/NO al mensaje del terapeuta (STUDENT_PICTOGRAM_RESPONSE).
     */
    private void handleStudentPictogramResponse(String payload) {
        try {
            JSONObject obj = new JSONObject(payload);
            boolean understood = obj.optBoolean("understood", false);

            if (understood) {
                tvStudentResponse.setText("✓ El alumno ha entendido el mensaje");
                tvStudentResponse.setTextColor(0xFF4CAF50);
                tvStudentResponse.setBackgroundColor(0x1A4CAF50);
            } else {
                tvStudentResponse.setText("✗ El alumno NO ha entendido. Repite el mensaje con otros pictogramas");
                tvStudentResponse.setTextColor(0xFFFF7043);
                tvStudentResponse.setBackgroundColor(0x1AFF7043);
            }
            tvStudentResponse.setVisibility(View.VISIBLE);

            // Auto-hide after RESPONSE_DISPLAY_MS
            handler.removeCallbacks(hideResponseRunnable);
            handler.postDelayed(hideResponseRunnable, RESPONSE_DISPLAY_MS);

            Log.d(TAG, "STUDENT_PICTOGRAM_RESPONSE recibido: understood=" + understood);
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing STUDENT_PICTOGRAM_RESPONSE", e);
        }
    }

    private void showReceivedMessage(List<String> pictogramIds) {
        layoutReceivedMessage.setVisibility(View.VISIBLE);
        layoutStudentPictos.removeAllViews();

        for (String pictoId : pictogramIds) {
            PictogramCatalog.PictogramItem item = PictogramCatalog.findById(pictoId);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

            ImageView img = new ImageView(this);
            int resId = getResources().getIdentifier(pictoId, "drawable", getPackageName());
            if (resId != 0) img.setImageResource(resId);
            else img.setBackgroundColor(0xFFE0E0E0);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int sizePx = dpToPx(48);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(sizePx, sizePx);
            img.setLayoutParams(imgParams);
            cell.addView(img);

            if (item != null) {
                TextView label = new TextView(this);
                label.setText(item.label);
                label.setTextSize(10f);
                label.setTextColor(Color.WHITE);
                label.setGravity(Gravity.CENTER);
                cell.addView(label);
            }

            layoutStudentPictos.addView(cell);
        }
    }

    // --- Respond to student ---

    private void respondToStudent(boolean understood) {
        if (targetRobotId == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("understood", understood);

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_COMMUNICATOR_RESPONSE);
            msg.put("payload", payload.toString());

            robotViewModel.sendMessage(targetRobotId, msg.toString());
            Log.d(TAG, "COMMUNICATOR_RESPONSE sent: understood=" + understood);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending COMMUNICATOR_RESPONSE", e);
        }

        // Hide received message panel
        layoutReceivedMessage.setVisibility(View.GONE);
    }

    // --- Send to student ---

    private void sendToStudent() {
        if (composition.isEmpty() || targetRobotId == null) return;

        try {
            JSONObject payload = new JSONObject();
            JSONArray ids = new JSONArray();
            for (String id : composition) ids.put(id);
            payload.put("pictogramIds", ids);

            JSONObject msg = new JSONObject();
            msg.put("type", AppConstants.MSG_TERAPEUTA_PICTOGRAM_MESSAGE);
            msg.put("payload", payload.toString());

            robotViewModel.sendMessage(targetRobotId, msg.toString());
            Log.d(TAG, "TERAPEUTA_PICTOGRAM_MESSAGE sent: " + ids);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending TERAPEUTA_PICTOGRAM_MESSAGE", e);
        }

        // Clear composition
        composition.clear();
        updateCompositionBar();
    }

    // --- Composition bar ---

    private void updateCompositionBar() {
        layoutComposedPictos.removeAllViews();
        btnSendToStudent.setEnabled(!composition.isEmpty());

        if (composition.isEmpty()) {
            TextView placeholder = new TextView(this);
            placeholder.setText("Selecciona pictogramas...");
            placeholder.setTextSize(12f);
            placeholder.setTextColor(0xFF9E9E9E);
            placeholder.setFontFeatureSettings("monospace");
            layoutComposedPictos.addView(placeholder);
            return;
        }

        for (int i = 0; i < composition.size(); i++) {
            final int index = i;
            String pictoId = composition.get(i);
            PictogramCatalog.PictogramItem item = PictogramCatalog.findById(pictoId);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

            ImageView img = new ImageView(this);
            int resId = getResources().getIdentifier(pictoId, "drawable", getPackageName());
            if (resId != 0) img.setImageResource(resId);
            int sizePx = dpToPx(40);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(sizePx, sizePx);
            img.setLayoutParams(imgParams);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            cell.addView(img);

            TextView removeBtn = new TextView(this);
            removeBtn.setText("✕");
            removeBtn.setTextSize(10f);
            removeBtn.setTextColor(0xFFFF5252);
            removeBtn.setGravity(Gravity.CENTER);
            removeBtn.setOnClickListener(v -> {
                composition.remove(index);
                updateCompositionBar();
            });
            cell.addView(removeBtn);

            layoutComposedPictos.addView(cell);
        }
    }

    // --- Category tabs ---

    private void buildCategoryTabs() {
        layoutCategories.removeAllViews();
        for (PictogramCatalog.Category cat : PictogramCatalog.Category.values()) {
            Button tab = new Button(this);
            tab.setText(cat.label);
            tab.setTextSize(11f);
            tab.setAllCaps(false);
            tab.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            tab.setMinimumWidth(0);
            tab.setMinWidth(0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            tab.setLayoutParams(params);

            updateTabStyle(tab, cat == currentCategory);

            tab.setOnClickListener(v -> {
                currentCategory = cat;
                loadCategoryGrid(cat);
                updateAllTabStyles();
            });

            layoutCategories.addView(tab);
        }
    }

    private void updateAllTabStyles() {
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            Button tab = (Button) layoutCategories.getChildAt(i);
            PictogramCatalog.Category cat = PictogramCatalog.Category.values()[i];
            updateTabStyle(tab, cat == currentCategory);
        }
    }

    private void updateTabStyle(Button tab, boolean selected) {
        if (selected) {
            tab.setBackgroundColor(0xFF00BCD4);
            tab.setTextColor(Color.WHITE);
            tab.setTypeface(null, Typeface.BOLD);
        } else {
            tab.setBackgroundColor(0xFF2D2D2D);
            tab.setTextColor(0xFFAAAAAA);
            tab.setTypeface(null, Typeface.NORMAL);
        }
    }

    // --- Pictogram grid ---

    private void loadCategoryGrid(PictogramCatalog.Category category) {
        gridPictograms.removeAllViews();
        List<PictogramCatalog.PictogramItem> items = PictogramCatalog.getByCategory(category);

        int cols = items.size() <= 4 ? 2 : 4;
        gridPictograms.setColumnCount(cols);

        for (PictogramCatalog.PictogramItem item : items) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setBackgroundColor(0xFF2D2D2D);
            cell.setClickable(true);
            cell.setFocusable(true);
            cell.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
            cell.setLayoutParams(params);

            // Image
            ImageView img = new ImageView(this);
            int resId = getResources().getIdentifier(item.id, "drawable", getPackageName());
            if (resId != 0) img.setImageResource(resId);
            else img.setBackgroundColor(0xFF424242);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setAdjustViewBounds(true);
            int imgSize = dpToPx(52);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(imgSize, imgSize);
            imgParams.setMargins(0, dpToPx(2), 0, dpToPx(2));
            img.setLayoutParams(imgParams);
            cell.addView(img);

            // Label
            TextView label = new TextView(this);
            label.setText(item.label);
            label.setTextSize(11f);
            label.setTextColor(Color.WHITE);
            label.setGravity(Gravity.CENTER);
            cell.addView(label);

            cell.setContentDescription(item.label);
            cell.setOnClickListener(v -> {
                if (composition.size() < 6) {
                    composition.add(item.id);
                    updateCompositionBar();
                }
            });

            gridPictograms.addView(cell);
        }
    }

    // --- Util ---

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
