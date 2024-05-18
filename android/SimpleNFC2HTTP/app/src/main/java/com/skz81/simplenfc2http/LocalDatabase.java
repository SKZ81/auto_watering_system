package com.skz81.simplenfc2http;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;
import java.sql.Timestamp;

@Database(entities = {Varieties.Variety.class, LocalDatabase.LastUpdate.class}, version = 1, exportSchema = false)
public abstract class LocalDatabase extends RoomDatabase {
    @Entity(tableName = "last_updates")
    public static class LastUpdate {
        @PrimaryKey(autoGenerate = true)
        private int id;
        @ColumnInfo(name = "table_name")
        private String tableName;
        private long timestamp;

        public LastUpdate(String tableName, long timestamp) {
            this.tableName = tableName;
            this.timestamp = timestamp;
        }

        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public String getTableName() {
            return tableName;
        }
        public long getTimestamp() {
            return timestamp;
        }
    }
    @Dao
    public interface LastUpdateDao {
        @Query("SELECT table_name FROM last_updates")
        LiveData<List<String>> getTableNames();

        @Query("SELECT * FROM last_updates WHERE table_name = :tableName")
        LiveData<LastUpdate> getLastUpdateByDataName(String tableName);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertLastUpdate(LastUpdate lastUpdate);
    }

    public abstract LastUpdateDao lastUpdateDao();
    public abstract Varieties.VarietyDao varietyDao();

    private static volatile LocalDatabase INSTANCE;

    public static LocalDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocalDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LocalDatabase.class, "autowats_local_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
