package com.example.appterapeuta.data.local.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.appterapeuta.data.local.dao.StudentProfileDao;
import com.example.appterapeuta.data.local.dao.TherapyActivityDao;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import java.util.concurrent.Executors;

@Database(
    entities = {StudentProfileEntity.class, TherapyActivityEntity.class},
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract StudentProfileDao studentProfileDao();
    public abstract TherapyActivityDao therapyActivityDao();

    // v1 → v2: sustituye avatar/educationalNeeds por excludedColors/backgroundSoundResName
    // SQLite no soporta DROP COLUMN en API < 32, se recrea la tabla.
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

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "appterapeuta.db"
                    )
                    .addMigrations(MIGRATION_1_2)
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
