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
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
     * Includes: sessionId, start, end, duration (min), robots, activity, students, attempts, successes, incidents.
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

                    // Header
                    pw.println("ID Sesión;Inicio;Fin;Duración (min);Robots;Actividad;Alumno;Intentos;Aciertos;Tasa (%);T.Medio (ms);Incidencias");

                    for (SessionRecordEntity session : sessions) {
                        long durationMin = (session.endTimestamp - session.startTimestamp) / 60000;
                        String start = SDF.format(new Date(session.startTimestamp));
                        String end = SDF.format(new Date(session.endTimestamp));
                        String robots = session.robotIdsJson != null ? session.robotIdsJson.replace("\"", "") : "—";

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
                                        pw.println(String.format("%s;%s;%s;%d;%s;%s;%s;%d;%d;%d;%d;%d",
                                                session.sessionId, start, end, durationMin, robots,
                                                ar.activityId, alumn.studentName,
                                                alumn.attempts, alumn.successes, rate,
                                                alumn.avgResponseTimeMs, incidentCount));
                                    }
                                } else {
                                    // Activity without alumn results
                                    pw.println(String.format("%s;%s;%s;%d;%s;%s;—;0;0;0;0;%d",
                                            session.sessionId, start, end, durationMin, robots,
                                            ar.activityId, incidentCount));
                                }
                            }
                        } else {
                            // Session without activity results
                            pw.println(String.format("%s;%s;%s;%d;%s;—;—;0;0;0;0;%d",
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
     * Includes session info, activity results per student, and incidents.
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

                Paint linePaint = new Paint();
                linePaint.setColor(0xFFCCCCCC);
                linePaint.setStrokeWidth(1f);

                float margin = 40f;
                float y = margin;

                // Title
                canvas.drawText("INFORME DE SESIÓN — EMOTIKO", margin, y + 18, titlePaint);
                y += 36;
                canvas.drawLine(margin, y, 555, y, linePaint);
                y += 20;

                // Session info
                long durationMin = (session.endTimestamp - session.startTimestamp) / 60000;
                canvas.drawText("ID: " + session.sessionId, margin, y, textPaint);
                y += 16;
                canvas.drawText("Inicio: " + SDF.format(new Date(session.startTimestamp)), margin, y, textPaint);
                y += 16;
                canvas.drawText("Fin: " + SDF.format(new Date(session.endTimestamp)), margin, y, textPaint);
                y += 16;
                canvas.drawText("Duración: " + durationMin + " minutos", margin, y, textPaint);
                y += 16;
                String robots = session.robotIdsJson != null ? session.robotIdsJson.replace("\"", "").replace("[", "").replace("]", "") : "—";
                canvas.drawText("Robots: " + robots, margin, y, textPaint);
                y += 24;

                canvas.drawLine(margin, y, 555, y, linePaint);
                y += 20;

                // Activities & results
                canvas.drawText("RESULTADOS POR ACTIVIDAD", margin, y, headerPaint);
                y += 20;

                if (activities != null && !activities.isEmpty()) {
                    for (ActivityResultEntity ar : activities) {
                        if (y > 780) {
                            document.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = margin;
                        }

                        canvas.drawText("▸ " + ar.activityId, margin, y, headerPaint);
                        y += 18;

                        List<AlumnResultEntity> alumns = db.alumnResultDao().getByActivityResult(ar.id);
                        if (alumns != null && !alumns.isEmpty()) {
                            for (AlumnResultEntity alumn : alumns) {
                                int rate = alumn.attempts > 0 ? (alumn.successes * 100 / alumn.attempts) : 0;
                                canvas.drawText("    Alumno: " + alumn.studentName, margin, y, textPaint);
                                y += 14;
                                canvas.drawText("    Intentos: " + alumn.attempts +
                                        " | Aciertos: " + alumn.successes +
                                        " | Tasa: " + rate + "%" +
                                        " | T.Medio: " + alumn.avgResponseTimeMs + "ms", margin + 10, y, textPaint);
                                y += 18;
                            }
                        } else {
                            canvas.drawText("    Sin resultados registrados", margin, y, textPaint);
                            y += 18;
                        }
                        y += 6;
                    }
                } else {
                    canvas.drawText("Sin actividades registradas en esta sesión.", margin, y, textPaint);
                    y += 20;
                }

                // Incidents
                y += 10;
                canvas.drawLine(margin, y, 555, y, linePaint);
                y += 20;
                canvas.drawText("INCIDENCIAS", margin, y, headerPaint);
                y += 20;

                if (incidents != null && !incidents.isEmpty()) {
                    for (IncidentEntity inc : incidents) {
                        if (y > 780) {
                            document.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = margin;
                        }
                        canvas.drawText("• Robot: " + inc.robotId + " — " +
                                SDF.format(new Date(inc.timestamp)) + " — " +
                                (inc.reason != null ? inc.reason : "Sin motivo"), margin, y, textPaint);
                        y += 16;
                    }
                } else {
                    canvas.drawText("No se registraron incidencias.", margin, y, textPaint);
                }

                document.finishPage(page);

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
}
