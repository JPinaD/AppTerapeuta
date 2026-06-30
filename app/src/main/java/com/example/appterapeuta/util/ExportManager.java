package com.example.appterapeuta.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.appterapeuta.data.local.db.AppDatabase;
import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Utilidad para exportar el historial de sesiones a CSV (todas) o PDF (una sesión concreta).
 * Genera archivos en cacheDir/exports/ y lanza Intent de compartición vía FileProvider.
 */
public class ExportManager {

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private final Context context;
    private final AppDatabase db;

    public ExportManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
    }

    /**
     * Exporta todas las sesiones a un archivo CSV y lanza el Intent de compartición.
     * Se ejecuta en un hilo de fondo.
     */
    public void exportAllSessionsCsv() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SessionRecordEntity> sessions = db.sessionRecordDao().getAllSync();
            if (sessions == null || sessions.isEmpty()) {
                showToast("No hay sesiones para exportar");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("sessionId;fechaInicio;fechaFin;robotIds;alumnosAsignados\n");

            for (SessionRecordEntity s : sessions) {
                sb.append(escape(s.sessionId)).append(';');
                sb.append(SDF.format(new Date(s.startTimestamp))).append(';');
                sb.append(s.endTimestamp > 0 ? SDF.format(new Date(s.endTimestamp)) : "—").append(';');
                sb.append(escape(s.robotIdsJson)).append(';');
                sb.append(escape(s.robotToStudentJson)).append('\n');
            }

            File file = writeToFile("historial_sesiones.csv", sb.toString());
            if (file != null) {
                shareFile(file, "text/csv");
            }
        });
    }

    /**
     * Exporta una sesión concreta a PDF con cabecera, resultados por alumno e incidencias.
     * Se ejecuta en un hilo de fondo.
     */
    public void exportSessionPdf(String sessionId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            SessionRecordEntity session = db.sessionRecordDao().getById(sessionId);
            if (session == null) {
                showToast("Sesión no encontrada");
                return;
            }

            List<ActivityResultEntity> activities = db.activityResultDao().getBySession(sessionId);
            List<IncidentEntity> incidents = db.incidentDao().getBySession(sessionId);

            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            paint.setTextSize(12f);
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(16f);
            titlePaint.setFakeBoldText(true);
            Paint headerPaint = new Paint();
            headerPaint.setTextSize(13f);
            headerPaint.setFakeBoldText(true);

            int pageWidth = 595; // A4 approx
            int pageHeight = 842;
            int margin = 40;
            int maxY = pageHeight - margin;
            int[] pageNum = {1};
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum[0]).create();
            PdfDocument.Page[] currentPage = {pdf.startPage(pageInfo)};
            Canvas[] canvas = {currentPage[0].getCanvas()};
            int[] y = {margin + 20};

            // Helper to check page break
            Runnable checkPage = () -> {
                if (y[0] > maxY) {
                    pdf.finishPage(currentPage[0]);
                    pageNum[0]++;
                    PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum[0]).create();
                    currentPage[0] = pdf.startPage(pi);
                    canvas[0] = currentPage[0].getCanvas();
                    y[0] = margin + 20;
                }
            };

            // Title
            canvas[0].drawText("Informe de Sesión", margin, y[0], titlePaint);
            y[0] += 30;

            // Header
            long durationMin = (session.endTimestamp - session.startTimestamp) / 60000;
            String[] headerLines = {
                    "ID: " + session.sessionId,
                    "Inicio: " + SDF.format(new Date(session.startTimestamp)),
                    "Fin: " + (session.endTimestamp > 0 ? SDF.format(new Date(session.endTimestamp)) : "—"),
                    "Duración: " + durationMin + " min",
                    "Robots: " + (session.robotIdsJson != null ? session.robotIdsJson : "—"),
                    "Alumnos: " + (session.robotToStudentJson != null ? session.robotToStudentJson : "—")
            };
            for (String line : headerLines) {
                canvas[0].drawText(line, margin, y[0], paint);
                y[0] += 18;
            }
            y[0] += 10;

            // Results per student
            canvas[0].drawText("Resultados por alumno", margin, y[0], headerPaint);
            y[0] += 22;

            if (activities != null) {
                for (ActivityResultEntity ar : activities) {
                    checkPage.run();
                    canvas[0].drawText("Actividad: " + ar.activityId, margin, y[0], paint);
                    y[0] += 18;

                    List<AlumnResultEntity> alumnResults = db.alumnResultDao().getByActivityResult(ar.id);
                    if (alumnResults != null) {
                        for (AlumnResultEntity alumn : alumnResults) {
                            checkPage.run();
                            String line = "  " + alumn.studentName +
                                    " — Intentos: " + alumn.attempts +
                                    ", Aciertos: " + alumn.successes +
                                    ", T.medio: " + alumn.avgResponseTimeMs + "ms";
                            canvas[0].drawText(line, margin + 10, y[0], paint);
                            y[0] += 16;
                        }
                    }
                    y[0] += 8;
                }
            }

            // Incidents
            y[0] += 10;
            checkPage.run();
            canvas[0].drawText("Incidencias", margin, y[0], headerPaint);
            y[0] += 22;

            if (incidents != null && !incidents.isEmpty()) {
                for (IncidentEntity inc : incidents) {
                    checkPage.run();
                    String line = SDF.format(new Date(inc.timestamp)) +
                            " | Robot: " + inc.robotId +
                            " | Motivo: " + (inc.reason != null ? inc.reason : "—");
                    canvas[0].drawText(line, margin, y[0], paint);
                    y[0] += 16;
                }
            } else {
                canvas[0].drawText("Sin incidencias registradas.", margin, y[0], paint);
                y[0] += 16;
            }

            pdf.finishPage(currentPage[0]);

            // Write PDF to file
            String filename = "sesion_" + sessionId.substring(0, Math.min(8, sessionId.length())) + ".pdf";
            File file = getExportsDir();
            File pdfFile = new File(file, filename);
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                pdf.writeTo(fos);
            } catch (IOException e) {
                showToast("Error al generar PDF");
                pdf.close();
                return;
            }
            pdf.close();

            shareFile(pdfFile, "application/pdf");
        });
    }

    private File getExportsDir() {
        File dir = new File(context.getCacheDir(), "exports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File writeToFile(String filename, String content) {
        File dir = getExportsDir();
        File file = new File(dir, filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            return file;
        } catch (IOException e) {
            showToast("Error al generar archivo");
            return null;
        }
    }

    private void shareFile(File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent chooser = Intent.createChooser(intent, "Compartir archivo");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    private void showToast(String msg) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    private String escape(String value) {
        if (value == null) return "—";
        return value.replace(";", ",").replace("\n", " ");
    }
}
