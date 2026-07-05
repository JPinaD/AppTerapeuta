package com.example.appterapeuta.data.local.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.appterapeuta.data.local.dao.ActivityResultDao;
import com.example.appterapeuta.data.local.dao.AlumnResultDao;
import com.example.appterapeuta.data.local.dao.IncidentDao;
import com.example.appterapeuta.data.local.dao.RobotConfigDao;
import com.example.appterapeuta.data.local.dao.SessionRecordDao;
import com.example.appterapeuta.data.local.dao.StudentProfileDao;
import com.example.appterapeuta.data.local.dao.TherapistDao;
import com.example.appterapeuta.data.local.dao.TherapyActivityDao;
import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.local.entity.RobotConfigEntity;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.local.entity.TherapistEntity;
import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import com.example.appterapeuta.util.HashUtils;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

@Database(
    entities = {
        StudentProfileEntity.class,
        TherapyActivityEntity.class,
        RobotConfigEntity.class,
        SessionRecordEntity.class,
        ActivityResultEntity.class,
        AlumnResultEntity.class,
        IncidentEntity.class,
        TherapistEntity.class
    },
    version = 5,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract StudentProfileDao studentProfileDao();
    public abstract TherapyActivityDao therapyActivityDao();
    public abstract RobotConfigDao robotConfigDao();
    public abstract SessionRecordDao sessionRecordDao();
    public abstract ActivityResultDao activityResultDao();
    public abstract AlumnResultDao alumnResultDao();
    public abstract IncidentDao incidentDao();
    public abstract TherapistDao therapistDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE student_profiles_new (" +
                    "id TEXT NOT NULL PRIMARY KEY, name TEXT, " +
                    "excludedColors TEXT, backgroundSoundResName TEXT)");
            db.execSQL("INSERT INTO student_profiles_new (id, name) SELECT id, name FROM student_profiles");
            db.execSQL("DROP TABLE student_profiles");
            db.execSQL("ALTER TABLE student_profiles_new RENAME TO student_profiles");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS robot_configs (" +
                    "robotId TEXT NOT NULL PRIMARY KEY, name TEXT, " +
                    "lastKnownHost TEXT, lastKnownPort INTEGER NOT NULL DEFAULT 9000)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS session_records (" +
                    "sessionId TEXT NOT NULL PRIMARY KEY, startTimestamp INTEGER NOT NULL, " +
                    "endTimestamp INTEGER NOT NULL, robotIdsJson TEXT, robotToStudentJson TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS activity_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId TEXT, activityId TEXT, " +
                    "FOREIGN KEY(sessionId) REFERENCES session_records(sessionId) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_results_sessionId ON activity_results(sessionId)");
            db.execSQL("CREATE TABLE IF NOT EXISTS alumn_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, activityResultId INTEGER NOT NULL, " +
                    "studentId TEXT, studentName TEXT, attempts INTEGER NOT NULL, successes INTEGER NOT NULL, " +
                    "avgResponseTimeMs INTEGER NOT NULL, finalPictogramId TEXT, " +
                    "FOREIGN KEY(activityResultId) REFERENCES activity_results(id) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_alumn_results_activityResultId ON alumn_results(activityResultId)");
            db.execSQL("CREATE TABLE IF NOT EXISTS incidents (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId TEXT, robotId TEXT, " +
                    "studentId TEXT, timestamp INTEGER NOT NULL, reason TEXT, " +
                    "FOREIGN KEY(sessionId) REFERENCES session_records(sessionId) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_sessionId ON incidents(sessionId)");
        }
    };

    // v4 -> v5: terapeutas + campos clinicos en student_profiles
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS therapists (" +
                    "username TEXT NOT NULL PRIMARY KEY, passwordHash TEXT, displayName TEXT)");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN communicationLevel TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN communicationLevelOther TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN sensorySensitivity TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN sensorySensitivityOther TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN attentionLevel TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN attentionLevelOther TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN motorSkills TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN motorSkillsOther TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN socioemotionalProfile TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN socioemotionalProfileOther TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN clinicalNotes TEXT");
            db.execSQL("ALTER TABLE student_profiles ADD COLUMN safePlaceUri TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "appterapeuta.db"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            // Asegurar que todos los datos semilla existen siempre.
                            // onOpen se llama tanto tras crear la BD como al abrirla,
                            // por lo que no necesitamos un callback onCreate separado.
                            Executors.newSingleThreadExecutor().execute(() -> {
                                AppDatabase adb = getInstance(context);
                                insertAllSeedData(adb);
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return instance;
    }

    /**
     * Inserta todos los datos semilla: actividades, terapeutas y alumnos de ejemplo.
     * Usa INSERT OR IGNORE para terapeutas (no sobreescribe si ya existen) y
     * comprueba si la tabla de alumnos está vacía antes de insertar los de ejemplo
     * (para no duplicar ni sobreescribir perfiles editados por el usuario).
     */
    private static void insertAllSeedData(AppDatabase adb) {
        // Catálogo de actividades (INSERT OR REPLACE — siempre actualiza)
        insertDefaultActivities(adb);
        // Terapeutas precargados (INSERT OR IGNORE — no sobreescribe)
        insertDefaultTherapists(adb);
        // Alumnos de ejemplo (solo si la tabla está vacía)
        insertSampleStudentsIfEmpty(adb);
    }

    private static void insertDefaultActivities(AppDatabase db) {
        // Remove activity_calm from catalogue — Momento Calma is only accessible via FAB
        db.therapyActivityDao().deleteById("activity_calm");
        TherapyActivityDao dao = db.therapyActivityDao();
        dao.insert(new TherapyActivityEntity(
                "activity_pictogram", "Pictogramas",
                "Comunicación funcional: el alumno selecciona pictogramas para expresar necesidades y emociones.", 1));
        dao.insert(new TherapyActivityEntity(
                "activity_emotion", "Reconocimiento Emocional",
                "Identificación de emociones: el alumno reconoce emociones en caras y avanza por un camino de casillas.", 2));
        dao.insert(new TherapyActivityEntity(
                "activity_social", "Escenarios Sociales",
                "Toma de decisiones sociales: el alumno elige la respuesta adecuada ante situaciones sociales.", 2));
        dao.insert(new TherapyActivityEntity(
                "activity_sequence", "Secuencias Visuales",
                "Memoria secuencial: el alumno memoriza y reproduce secuencias de movimientos.", 3));
        dao.insert(new TherapyActivityEntity(
                "activity_turns", "Turnos Sociales",
                "Habilidades de turno y cooperación: actividad multi-robot para practicar la espera y el respeto de turnos.", 3));
    }

    /**
     * Inserta los 4 terapeutas precargados. Usa INSERT OR IGNORE (del DAO),
     * por lo que no sobreescribe terapeutas existentes ni sus contraseñas modificadas.
     */
    private static void insertDefaultTherapists(AppDatabase db) {
        TherapistDao dao = db.therapistDao();
        dao.insert(new TherapistEntity("rocio",  HashUtils.sha256("terapeuta1"), "Rocío Navarro"));
        dao.insert(new TherapistEntity("carlos", HashUtils.sha256("terapeuta2"), "Carlos Martínez"));
        dao.insert(new TherapistEntity("laura",  HashUtils.sha256("terapeuta3"), "Laura Sánchez"));
        dao.insert(new TherapistEntity("pedro",  HashUtils.sha256("terapeuta4"), "Pedro Romero"));
    }

    /**
     * Inserta los 6 alumnos de ejemplo SOLO si la tabla student_profiles está vacía.
     * Esto evita duplicar perfiles (ya que usan UUID aleatorio) y no sobreescribe
     * perfiles que el terapeuta haya editado o creado.
     */
    private static void insertSampleStudentsIfEmpty(AppDatabase db) {
        // Verificar si ya hay alumnos (query síncrona directa)
        List<StudentProfileEntity> existing = db.studentProfileDao().getAllSync();
        if (existing != null && !existing.isEmpty()) {
            return; // Ya hay datos, no insertar
        }

        StudentProfileEntity marcos = new StudentProfileEntity(UUID.randomUUID().toString(), "Marcos", null, "sound_birds");
        marcos.communicationLevel    = "Comunicación emergente";
        marcos.sensorySensitivity    = "Hipersensibilidad (evita estímulos)";
        marcos.attentionLevel        = "Atención muy reducida (< 5 min)";
        marcos.motorSkills           = "Dificultades en motricidad fina";
        marcos.socioemotionalProfile = "Alta reactividad emocional";
        db.studentProfileDao().insert(marcos);

        StudentProfileEntity sofia = new StudentProfileEntity(UUID.randomUUID().toString(), "Sofía", null, "sound_ocean");
        sofia.communicationLevel    = "Comunicación funcional con apoyo";
        sofia.sensorySensitivity    = "Perfil mixto";
        sofia.attentionLevel        = "Atención reducida (5-10 min)";
        sofia.motorSkills           = "Sin dificultades aparentes";
        sofia.socioemotionalProfile = "Dificultad en reconocimiento emocional";
        db.studentProfileDao().insert(sofia);

        StudentProfileEntity daniel = new StudentProfileEntity(UUID.randomUUID().toString(), "Daniel", null, null);
        daniel.communicationLevel    = "No verbal";
        daniel.sensorySensitivity    = "Hiposensibilidad (busca estímulos)";
        daniel.attentionLevel        = "Atención muy reducida (< 5 min)";
        daniel.motorSkills           = "Dificultades en ambas";
        daniel.socioemotionalProfile = "Tendencia al aislamiento";
        db.studentProfileDao().insert(daniel);

        StudentProfileEntity lucia = new StudentProfileEntity(UUID.randomUUID().toString(), "Lucía", null, "sound_rain");
        lucia.communicationLevel    = "Comunicación verbal funcional";
        lucia.sensorySensitivity    = "Sin alteraciones aparentes";
        lucia.attentionLevel        = "Atención sostenida (> 20 min)";
        lucia.motorSkills           = "Sin dificultades aparentes";
        lucia.socioemotionalProfile = "Conductas de búsqueda de interacción";
        db.studentProfileDao().insert(lucia);

        StudentProfileEntity adrian = new StudentProfileEntity(UUID.randomUUID().toString(), "Adrián", null, null);
        adrian.communicationLevel    = "Comunicación emergente";
        adrian.sensorySensitivity    = "Hipersensibilidad (evita estímulos)";
        adrian.attentionLevel        = "Atención moderada (10-20 min)";
        adrian.motorSkills           = "Dificultades en motricidad gruesa";
        adrian.socioemotionalProfile = "Perfil mixto";
        db.studentProfileDao().insert(adrian);

        StudentProfileEntity elena = new StudentProfileEntity(UUID.randomUUID().toString(), "Elena", null, "sound_birds");
        elena.communicationLevel    = "Comunicación funcional con apoyo";
        elena.sensorySensitivity    = "Perfil mixto";
        elena.attentionLevel        = "Atención reducida (5-10 min)";
        elena.motorSkills           = "Dificultades en motricidad fina";
        elena.socioemotionalProfile = "Alta reactividad emocional";
        db.studentProfileDao().insert(elena);
    }
}
