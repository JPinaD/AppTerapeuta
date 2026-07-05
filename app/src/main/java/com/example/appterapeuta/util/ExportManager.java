package com.example.appterapeuta.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.local.entity.ItemResultEntity;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Utility class for exporting session history data to CSV and PDF formats.
 * Uses Android's built-in PdfDocument API (no external dependencies).
 */
public class ExportManager {

    private static final String TAG = "ExportManager";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FILE_SDF = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    public interface ExportCallback {
        void onSuccess(File file);
        void onError(String message);
    }

    /**
     * Exports all session records to a CSV file.
     * Includes: session info, activity, student name, results, and student clinical profile data.
     */
    public static void exportCSV(Context context, ExportCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<SessionRecordEntity> sessions = db.sessionRecordDao().getAllSync();

                if (sessions == null || sessions.isEmpty()) {
                    callback.onError("No hay sesiones para exportar");
                    return;
                }

                File exportDir = new File(context.getExternalFilesDir(null), "exports");
                if (!exportDir.exists()) exportDir.mkdirs();

                String filename = "sesiones_" + FILE_SDF.format(new Date()) + ".csv";
                File csvFile = new File(exportDir, filename);

                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(csvFile), "UTF-8"))) {

                    // BOM for Excel UTF-8 compatibility
                    pw.write('\uFEFF');

                    // Header — includes profile columns
                    pw.println("ID Sesión;Inicio;Fin;Duración (min);Robots;Actividad;Alumno;" +
                            "Nivel Comunicativo;Sensibilidad Sensorial;Nivel Atencional;" +
                            "Motricidad;Perfil Socioemocional;" +
                            "Intentos;Aciertos;Tasa (%);T.Medio (ms);Incidencias");

                    for (SessionRecordEntity session : sessions) {
                        long durationMin = (session.endTimestamp - session.startTimestamp) / 60000;
                        String start = SDF.format(new Date(session.startTimestamp));
                        String end = SDF.format(new Date(session.endTimestamp));
                        String robots = session.robotIdsJson != null ? session.robotIdsJson.replace("\"", "") : "—";

                        // Parse robotToStudentJson to get studentIds
                        Map<String, String> robotToStudent = parseRobotToStudentMap(session.robotToStudentJson);

                        // Get activity results for this session
                        List<ActivityResultEntity> activities = db.activityResultDao().getBySession(session.sessionId);
                        List<IncidentEntity> incidents = db.incidentDao().getBySession(session.sessionId);
                        int incidentCount = incidents != null ? incidents.size() : 0;

                        if (activities != null && !activities.isEmpty()) {
                            for (ActivityResultEntity ar : activities) {
                                List<AlumnResultEntity> alumns = db.alumnResultDao().getByActivityResult(ar.id);
                                if (alumns != null && !alumns.isEmpty()) {
                                    for (AlumnResultEntity alumn : alumns) {
                                        int rate = alumn.attempts > 0 ? (alumn.successes * 100 / alumn.attempts) : 0;
                                        // Load student profile
                                        StudentProfileEntity profile = loadProfileForStudent(db, alumn.studentId, robotToStudent);
                                        pw.println(String.format("%s;%s;%s;%d;%s;%s;%s;%s;%s;%s;%s;%s;%d;%d;%d;%d;%d",
                                                session.sessionId, start, end, durationMin, robots,
                                                ar.activityId, alumn.studentName,
                                                safeStr(profile != null ? profile.communicationLevel : null),
                                                safeStr(profile != null ? profile.sensorySensitivity : null),
                                                safeStr(profile != null ? profile.attentionLevel : null),
                                                safeStr(profile != null ? profile.motorSkills : null),
                                                safeStr(profile != null ? profile.socioemotionalProfile : null),
                                                alumn.attempts, alumn.successes, rate,
                                                alumn.avgResponseTimeMs, incidentCount));
                                    }
                                } else {
                                    // Activity without alumn results
                                    pw.println(String.format("%s;%s;%s;%d;%s;%s;—;—;—;—;—;—;0;0;0;0;%d",
                                            session.sessionId, start, end, durationMin, robots,
                                            ar.activityId, incidentCount));
                                }
                            }
                        } else {
                            // Session without activity results
                            pw.println(String.format("%s;%s;%s;%d;%s;—;—;—;—;—;—;—;0;0;0;0;%d",
                                    session.sessionId, start, end, durationMin, robots, incidentCount));
                        }
                    }
                }

                callback.onSuccess(csvFile);

            } catch (Exception e) {
                Log.e(TAG, "Error exporting CSV", e);
                callback.onError("Error al exportar: " + e.getMessage());
            }
        });
    }

    /**
     * Exports a single session to a PDF document with detail.
     * Includes session info, student profile, activity results per student,
     * emotion grouping (if applicable), and incidents.
     */
    public static void exportPDF(Context context, String sessionId, ExportCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                SessionRecordEntity session = db.sessionRecordDao().getById(sessionId);

                if (session == null) {
                    callback.onError("Sesión no encontrada");
                    return;
                }

                List<ActivityResultEntity> activities = db.activityResultDao().getBySession(sessionId);
                List<IncidentEntity> incidents = db.incidentDao().getBySession(sessionId);
                List<ItemResultEntity> itemResults = db.itemResultDao().getBySession(sessionId);

                // Parse robotToStudentJson
                Map<String, String> robotToStudent = parseRobotToStudentMap(session.robotToStudentJson);

                File exportDir = new File(context.getExternalFilesDir(null), "exports");
                if (!exportDir.exists()) exportDir.mkdirs();

                String filename = "sesion_" + sessionId.substring(0, Math.min(8, sessionId.length()))
                        + "_" + FILE_SDF.format(new Date()) + ".pdf";
                File pdfFile = new File(exportDir, filename);

                PdfDocument document = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                Paint titlePaint = new Paint();
                titlePaint.setTextSize(18f);
                titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                titlePaint.setColor(0xFF1A237E); // Dark blue

                Paint headerPaint = new Paint();
                headerPaint.setTextSize(13f);
                headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                headerPaint.setColor(0xFF333333);

                Paint textPaint = new Paint();
                textPaint.setTextSize(11f);
                textPaint.setColor(0xFF444444);

                Paint smallPaint = new Paint();
                smallPaint.setTextSize(10f);
                smallPaint.setColor(0xFF666666);

                Paint linePaint = new Paint();
                linePaint.setColor(0xFFCCCCCC);
                linePaint.setStrokeWidth(1f);

                float margin = 40f;
                float y = margin;
                float pageWidth = 555f;

                // --- Helper array to allow page breaks (mutable reference) ---
                final PdfDocument.Page[] currentPage = {page};
                final Canvas[] currentCanvas = {canvas};
                final float[] currentY = {y};

                // Title
                currentCanvas[0].drawText("INFORME DE SESIÓN — EMOTIKO", margin, currentY[0] + 18, titlePaint);
                currentY[0] += 36;
                currentCanvas[0].drawLine(margin, currentY[0], pageWidth, currentY[0], linePaint);
                currentY[0] += 20;

                // Session info
                long durationMin = (session.endTimestamp - session.startTimestamp) / 60000;
                drawText(currentCanvas[0], "ID: " + session.sessionId, margin, currentY, textPaint);
                drawText(currentCanvas[0], "Inicio: " + SDF.format(new Date(session.startTimestamp)), margin, currentY, textPaint);
                drawText(currentCanvas[0], "Fin: " + SDF.format(new Date(session.endTimestamp)), margin, currentY, textPaint);
                drawText(currentCanvas[0], "Duración: " + durationMin + " minutos", margin, currentY, textPaint);
                String robots = session.robotIdsJson != null ? session.robotIdsJson.replace("\"", "").replace("[", "").replace("]", "") : "—";
                drawText(currentCanvas[0], "Robots: " + robots, margin, currentY, textPaint);
                currentY[0] += 8;

                // --- Student Profile Section ---
                currentCanvas[0].drawLine(margin, currentY[0], pageWidth, currentY[0], linePaint);
                currentY[0] += 16;
                currentCanvas[0].drawText("PERFIL DEL ALUMNO", margin, currentY[0], headerPaint);
                currentY[0] += 18;

                // Collect unique student IDs from the mapping
                boolean profileWritten = false;
                for (String studentId : robotToStudent.values()) {
                    if (studentId == null) continue;
                    StudentProfileEntity profile = db.studentProfileDao().getById(studentId);
                    if (profile == null) continue;

                    if (currentY[0] > 780) {
                        document.finishPage(currentPage[0]);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        currentPage[0] = document.startPage(pageInfo);
                        currentCanvas[0] = currentPage[0].getCanvas();
                        currentY[0] = margin;
                    }

                    drawText(currentCanvas[0], "▸ " + (profile.name != null ? profile.name : studentId), margin, currentY, headerPaint);
                    drawText(currentCanvas[0], "    Nivel comunicativo: " + safeStr(profile.communicationLevel), margin, currentY, textPaint);
                    drawText(currentCanvas[0], "    Sensibilidad sensorial: " + safeStr(profile.sensorySensitivity), margin, currentY, textPaint);
                    drawText(currentCanvas[0], "    Nivel atencional: " + safeStr(profile.attentionLevel), margin, currentY, textPaint);
                    drawText(currentCanvas[0], "    Motricidad: " + safeStr(profile.motorSkills), margin, currentY, textPaint);
                    drawText(currentCanvas[0], "    Perfil socioemocional: " + safeStr(profile.socioemotionalProfile), margin, currentY, textPaint);
                    if (profile.clinicalNotes != null && !profile.clinicalNotes.isEmpty()) {
                        drawText(currentCanvas[0], "    Notas clínicas: " + profile.clinicalNotes, margin, currentY, smallPaint);
                    }
                    currentY[0] += 6;
                    profileWritten = true;
                }
                if (!profileWritten) {
                    drawText(currentCanvas[0], "No se encontró perfil del alumno asociado.", margin, currentY, textPaint);
                }

                currentY[0] += 8;
                currentCanvas[0].drawLine(margin, currentY[0], pageWidth, currentY[0], linePaint);
                currentY[0] += 16;

                // Activities & results
                currentCanvas[0].drawText("RESULTADOS POR ACTIVIDAD", margin, currentY[0], headerPaint);
                currentY[0] += 20;

                if (activities != null && !activities.isEmpty()) {
                    for (ActivityResultEntity ar : activities) {
                        if (currentY[0] > 780) {
                            document.finishPage(currentPage[0]);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            currentPage[0] = document.startPage(pageInfo);
                            currentCanvas[0] = currentPage[0].getCanvas();
                            currentY[0] = margin;
                        }

                        currentCanvas[0].drawText("▸ " + ar.activityId, margin, currentY[0], headerPaint);
                        currentY[0] += 18;

                        List<AlumnResultEntity> alumns = db.alumnResultDao().getByActivityResult(ar.id);
                        if (alumns != null && !alumns.isEmpty()) {
                            for (AlumnResultEntity alumn : alumns) {
                                int rate = alumn.attempts > 0 ? (alumn.successes * 100 / alumn.attempts) : 0;
                                drawText(currentCanvas[0], "    Alumno: " + alumn.studentName, margin, currentY, textPaint);
                                drawText(currentCanvas[0], "    Intentos: " + alumn.attempts +
                                        " | Aciertos: " + alumn.successes +
                                        " | Tasa: " + rate + "%" +
                                        " | T.Medio: " + alumn.avgResponseTimeMs + "ms", margin + 10, currentY, textPaint);
                                currentY[0] += 4;
                            }
                        } else {
                            drawText(currentCanvas[0], "    Sin resultados registrados", margin, currentY, textPaint);
                        }
                        currentY[0] += 6;
                    }
                } else {
                    drawText(currentCanvas[0], "Sin actividades registradas en esta sesión.", margin, currentY, textPaint);
                }

                // --- Emotion Grouping Section (if activity_emotion was used) ---
                Map<String, int[]> emotionStats = buildEmotionStats(itemResults, "activity_emotion");
                if (!emotionStats.isEmpty()) {
                    currentY[0] += 10;
                    if (currentY[0] > 740) {
                        document.finishPage(currentPage[0]);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        currentPage[0] = document.startPage(pageInfo);
                        currentCanvas[0] = currentPage[0].getCanvas();
                        currentY[0] = margin;
                    }
                    currentCanvas[0].drawLine(margin, currentY[0], pageWidth, currentY[0], linePaint);
                    currentY[0] += 16;
                    currentCanvas[0].drawText("RESULTADOS POR EMOCIÓN", margin, currentY[0], headerPaint);
                    currentY[0] += 18;

                    for (Map.Entry<String, int[]> entry : emotionStats.entrySet()) {
                        if (currentY[0] > 780) {
                            document.finishPage(currentPage[0]);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            currentPage[0] = document.startPage(pageInfo);
                            currentCanvas[0] = currentPage[0].getCanvas();
                            currentY[0] = margin;
                        }
                        String emotionName = formatEmotionName(entry.getKey());
                        int[] stats = entry.getValue(); // [successes, total]
                        int total = stats[1];
                        int successes = stats[0];
                        int pct = total > 0 ? (successes * 100 / total) : 0;
                        drawText(currentCanvas[0], "    " + emotionName + ": " +
                                successes + "/" + total + " (" + pct + "%)", margin, currentY, textPaint);
                    }
                }

                // Incidents
                currentY[0] += 10;
                if (currentY[0] > 760) {
                    document.finishPage(currentPage[0]);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    currentPage[0] = document.startPage(pageInfo);
                    currentCanvas[0] = currentPage[0].getCanvas();
                    currentY[0] = margin;
                }
                currentCanvas[0].drawLine(margin, currentY[0], pageWidth, currentY[0], linePaint);
                currentY[0] += 16;
                currentCanvas[0].drawText("INCIDENCIAS", margin, currentY[0], headerPaint);
                currentY[0] += 18;

                if (incidents != null && !incidents.isEmpty()) {
                    for (IncidentEntity inc : incidents) {
                        if (currentY[0] > 780) {
                            document.finishPage(currentPage[0]);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            currentPage[0] = document.startPage(pageInfo);
                            currentCanvas[0] = currentPage[0].getCanvas();
                            currentY[0] = margin;
                        }
                        drawText(currentCanvas[0], "• Robot: " + inc.robotId + " — " +
                                SDF.format(new Date(inc.timestamp)) + " — " +
                                (inc.reason != null ? inc.reason : "Sin motivo"), margin, currentY, textPaint);
                    }
                } else {
                    drawText(currentCanvas[0], "No se registraron incidencias.", margin, currentY, textPaint);
                }

                document.finishPage(currentPage[0]);

                try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                    document.writeTo(fos);
                }
                document.close();

                callback.onSuccess(pdfFile);

            } catch (Exception e) {
                Log.e(TAG, "Error exporting PDF", e);
                callback.onError("Error al exportar PDF: " + e.getMessage());
            }
        });
    }

    /**
     * Creates a share intent for the exported file.
     */
    public static Intent createShareIntent(Context context, File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(intent, "Compartir exportación");
    }

    // --- Private helpers ---

    /**
     * Draws text and advances Y position.
     */
    private static void drawText(Canvas canvas, String text, float x, float[] y, Paint paint) {
        canvas.drawText(text, x, y[0], paint);
        y[0] += 15;
    }

    /**
     * Parses the robotToStudentJson field into a Map<robotId, studentId>.
     */
    private static Map<String, String> parseRobotToStudentMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isEmpty()) return map;
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, obj.optString(key, null));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing robotToStudentJson: " + json);
        }
        return map;
    }

    /**
     * Loads the student profile entity. First tries studentId directly, then looks
     * through the robot-to-student mapping to find the profile.
     */
    private static StudentProfileEntity loadProfileForStudent(AppDatabase db, String studentId,
                                                              Map<String, String> robotToStudent) {
        // Try direct lookup by studentId (from AlumnResult)
        if (studentId != null && !studentId.isEmpty()) {
            StudentProfileEntity profile = db.studentProfileDao().getById(studentId);
            if (profile != null) return profile;
        }
        // Fallback: try all IDs in the mapping
        for (String id : robotToStudent.values()) {
            if (id != null) {
                StudentProfileEntity profile = db.studentProfileDao().getById(id);
                if (profile != null) return profile;
            }
        }
        return null;
    }

    /**
     * Builds emotion statistics from per-item results.
     * Returns a map of itemId -> [successes, totalAttempts].
     */
    private static Map<String, int[]> buildEmotionStats(List<ItemResultEntity> itemResults, String activityId) {
        Map<String, int[]> stats = new LinkedHashMap<>();
        if (itemResults == null) return stats;
        for (ItemResultEntity item : itemResults) {
            if (activityId.equals(item.activityId) && item.itemId != null) {
                int[] counts = stats.get(item.itemId);
                if (counts == null) {
                    counts = new int[]{0, 0};
                    stats.put(item.itemId, counts);
                }
                counts[1]++; // total
                if (item.correct) counts[0]++; // successes
            }
        }
        return stats;
    }

    /**
     * Formats an emotion ID into a human-readable name.
     * E.g., "emotion_happy" -> "Alegría", "emotion_sad" -> "Tristeza".
     */
    private static String formatEmotionName(String emotionId) {
        if (emotionId == null) return "Desconocida";
        switch (emotionId) {
            case "emotion_happy":      return "Alegría";
            case "emotion_sad":        return "Tristeza";
            case "emotion_angry":      return "Enfado";
            case "emotion_surprised":  return "Sorpresa";
            case "emotion_scared":     return "Miedo";
            case "emotion_disgusted":  return "Asco";
            case "emotion_calm":       return "Calma";
            case "emotion_shy":        return "Vergüenza";
            case "emotion_bored":      return "Aburrimiento";
            case "emotion_tired":      return "Cansancio";
            case "emotion_excited":    return "Emoción";
            case "emotion_terror":     return "Terror";
            default:                   return emotionId;
        }
    }

    /**
     * Returns the string or "—" if null/empty.
     */
    private static String safeStr(String value) {
        return (value != null && !value.isEmpty()) ? value : "—";
    }
}
