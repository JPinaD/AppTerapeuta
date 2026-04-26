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
import com.example.appterapeuta.data.local.dao.TherapyActivityDao;
import com.example.appterapeuta.data.local.entity.ActivityResultEntity;
import com.example.appterapeuta.data.local.entity.AlumnResultEntity;
import com.example.appterapeuta.data.local.entity.IncidentEntity;
import com.example.appterapeuta.data.local.entity.RobotConfigEntity;
import com.example.appterapeuta.data.local.entity.SessionRecordEntity;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import java.util.concurrent.Executors;

@Database(
    entities = {
        StudentProfileEntity.class,
        TherapyActivityEntity.class,
        RobotConfigEntity.class,
        SessionRecordEntity.class,
        ActivityResultEntity.class,
        AlumnResultEntity.class,
        IncidentEntity.class
    },
    version = 4,
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

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE student_profiles_new (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "name TEXT, " +
                    "excludedColors TEXT, " +
                    "backgroundSoundResName TEXT)");
            db.execSQL("INSERT INTO student_profiles_new (id, name) " +
                    "SELECT id, name FROM student_profiles");
            db.execSQL("DROP TABLE student_profiles");
            db.execSQL("ALTER TABLE student_profiles_new RENAME TO student_profiles");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS robot_configs (" +
                    "robotId TEXT NOT NULL PRIMARY KEY, " +
                    "name TEXT, " +
                    "lastKnownHost TEXT, " +
                    "lastKnownPort INTEGER NOT NULL DEFAULT 9000)");
        }
    };

    // v3 → v4: añade tablas de historial (session_records, activity_results, alumn_results, incidents)
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS session_records (" +
                    "sessionId TEXT NOT NULL PRIMARY KEY, " +
                    "startTimestamp INTEGER NOT NULL, " +
                    "endTimestamp INTEGER NOT NULL, " +
                    "robotIdsJson TEXT, " +
                    "robotToStudentJson TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS activity_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sessionId TEXT, " +
                    "activityId TEXT, " +
                    "FOREIGN KEY(sessionId) REFERENCES session_records(sessionId) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS alumn_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "activityResultId INTEGER, " +
                    "studentId TEXT, " +
                    "studentName TEXT, " +
                    "attempts INTEGER NOT NULL, " +
                    "successes INTEGER NOT NULL, " +
                    "avgResponseTimeMs INTEGER NOT NULL, " +
                    "finalPictogramId TEXT, " +
                    "FOREIGN KEY(activityResultId) REFERENCES activity_results(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS incidents (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sessionId TEXT, " +
                    "robotId TEXT, " +
                    "studentId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "reason TEXT, " +
                    "FOREIGN KEY(sessionId) REFERENCES session_records(sessionId) ON DELETE CASCADE)");
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Executors.newSingleThreadExecutor().execute(() ->
                                    getInstance(context).therapyActivityDao().insert(
                                            new TherapyActivityEntity(
                                                    "pictogram_v1",
                                                    "El robot quiere decir algo",
                                                    "El alumno elige un pictograma para comunicarse con el robot.",
                                                    1
                                            )
                                    )
                            );
                        }
                    })
                    .build();
                }
            }
        }
        return instance;
    }
}
