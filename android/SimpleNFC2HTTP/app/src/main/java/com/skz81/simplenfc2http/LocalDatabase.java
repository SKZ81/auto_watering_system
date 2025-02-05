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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
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

        public LastUpdate() {} // No-argument constructor (required for Room)

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
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
        public long getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Dao
    public interface LastUpdateDao {
        @Query("SELECT COUNT(*) FROM last_updates WHERE table_name = :tableName")
        int countByTableName(String tableName);

        // @Query("SELECT table_name FROM last_updates")
        // LiveData<List<String>> getTableNamesLive();
        //
        // @Query("SELECT * FROM last_updates WHERE table_name = :tableName")
        // LiveData<LastUpdate> getLastUpdateByTableNameLive(String tableName);
        @Query("SELECT table_name FROM last_updates")
        List<String> getTableNames();

        @Query("SELECT * FROM last_updates WHERE table_name = :tableName")
        LastUpdate getLastUpdateByTableName(String tableName);

        @Query("SELECT * FROM last_updates")
        List<LastUpdate> getAll();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertLastUpdate(LastUpdate lastUpdate);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertAll(List<LastUpdate> lastUpdates);
    }

    public abstract LastUpdateDao lastUpdateDao();
    public abstract Varieties.VarietiesDao varietiesDao();

    private static List<String> requiredLocalTables = Arrays.asList("varieties"/*, "products", "orders"*/);
    // Ensure all table names exist in the local database
    public void ensureTableNamesExist() {
        LastUpdateDao lastUpdateDao = lastUpdateDao();
        // Run this in a background thread (Room requires it for DB operations)
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LastUpdate> newEntries = new ArrayList<>();

            for (String tableName : requiredLocalTables) {
                // Check if the table name exists
                if (lastUpdateDao.countByTableName(tableName) == 0) {
                    // Add a new entry for the missing table name with timestamp = 0
                    newEntries.add(new LastUpdate(tableName, 0));
                }
            }

            // Insert all new entries
            if (!newEntries.isEmpty()) {
                lastUpdateDao.insertAll(newEntries);
            }
        });
    }

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
