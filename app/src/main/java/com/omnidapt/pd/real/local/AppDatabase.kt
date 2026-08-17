package com.omnidapt.pd.real.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey val eventId: String,
    val type: String,
    val payloadJson: String,
    val createdAtMs: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)

@Entity(tableName = "cached_patients")
data class CachedPatientEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val gender: String,
    val age: Int?,
    val implantDate: String?,
    val summary: String,
    val emergencyContact: String?,
    val emergencyPhone: String?,
    val updatedAtMs: Long,
)

@Entity(tableName = "cached_models")
data class CachedModelEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val version: Int,
    val payloadJson: String,
    val approved: Boolean,
    val updatedAtMs: Long,
)

@Entity(tableName = "cached_chat_messages")
data class CachedChatMessageEntity(
    @PrimaryKey val eventId: String,
    val sessionId: String,
    val senderUserId: String,
    val content: String,
    val createdAt: String,
    val pending: Boolean,
)

@Dao
interface PendingEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PendingEventEntity): Long

    @Query("SELECT * FROM pending_events ORDER BY createdAtMs LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<PendingEventEntity>

    @Query("DELETE FROM pending_events WHERE eventId = :eventId")
    suspend fun delete(eventId: String)

    @Query(
        """
        UPDATE pending_events
        SET retryCount = retryCount + 1, lastError = :error
        WHERE eventId = :eventId
        """,
    )
    suspend fun markFailure(eventId: String, error: String)

    @Query("SELECT COUNT(*) FROM pending_events")
    suspend fun count(): Int
}

@Dao
interface PatientCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(patients: List<CachedPatientEntity>)

    @Query("SELECT * FROM cached_patients ORDER BY code")
    suspend fun all(): List<CachedPatientEntity>

    @Query("DELETE FROM cached_patients")
    suspend fun clear()
}

@Dao
interface ModelCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: CachedModelEntity)

    @Query(
        """
        SELECT * FROM cached_models
        WHERE patientId = :patientId AND approved = 1
        ORDER BY version DESC LIMIT 1
        """,
    )
    suspend fun latestApproved(patientId: String): CachedModelEntity?
}

@Dao
interface ChatCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: CachedChatMessageEntity)

    @Query("SELECT * FROM cached_chat_messages WHERE sessionId = :sessionId ORDER BY createdAt")
    suspend fun messages(sessionId: String): List<CachedChatMessageEntity>
}

@Database(
    entities = [
        PendingEventEntity::class,
        CachedPatientEntity::class,
        CachedModelEntity::class,
        CachedChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OminidaptDatabase : RoomDatabase() {
    abstract fun pendingEvents(): PendingEventDao
    abstract fun patients(): PatientCacheDao
    abstract fun models(): ModelCacheDao
    abstract fun chat(): ChatCacheDao

    companion object {
        @Volatile private var instance: OminidaptDatabase? = null

        fun get(context: Context): OminidaptDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OminidaptDatabase::class.java,
                    "omnidapt.db",
                ).build().also { instance = it }
            }
    }
}
