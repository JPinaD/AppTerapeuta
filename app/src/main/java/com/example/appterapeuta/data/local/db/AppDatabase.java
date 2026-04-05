package com.example.appterapeuta.data.local.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.appterapeuta.data.local.dao.StudentProfileDao;
import com.example.appterapeuta.data.local.dao.TherapyActivityDao;
import com.example.appterapeuta.data.local.entity.StudentProfileEntity;
import com.example.appterapeuta.data.local.entity.TherapyActivityEntity;

import java.util.concurrent.Executors;

@Database(
    entities = {StudentProfileEntity.class, TherapyActivityEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract StudentProfileDao studentProfileDao();
    public abstract TherapyActivityDao therapyActivityDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "appterapeuta.db"
                    )
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
