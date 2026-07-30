package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage ORDER BY date DESC, usageSeconds DESC")
    fun getAllUsageFlow(): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun getUsageForDate(date: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE date = :date AND platform = :platform LIMIT 1")
    suspend fun getUsageForDateAndPlatform(date: String, platform: String): DailyUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyUsageEntity): Long

    @Query("DELETE FROM daily_usage")
    suspend fun clearAll()

    @Query("UPDATE daily_usage SET usageSeconds = usageSeconds + :incrementSeconds WHERE date = :date AND platform = :platform")
    suspend fun incrementUsageSeconds(date: String, platform: String, incrementSeconds: Long): Int
}

@Dao
interface PlatformGoalDao {
    @Query("SELECT * FROM platform_goals")
    fun getAllGoalsFlow(): Flow<List<PlatformGoalEntity>>

    @Query("SELECT * FROM platform_goals")
    suspend fun getAllGoals(): List<PlatformGoalEntity>

    @Query("SELECT * FROM platform_goals WHERE platform = :platform LIMIT 1")
    suspend fun getGoalForPlatform(platform: String): PlatformGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: PlatformGoalEntity)

    @Delete
    suspend fun delete(goal: PlatformGoalEntity)
}

@Dao
interface BlockScheduleDao {
    @Query("SELECT * FROM block_schedules ORDER BY id DESC")
    fun getAllSchedulesFlow(): Flow<List<BlockScheduleEntity>>

    @Query("SELECT * FROM block_schedules")
    suspend fun getAllSchedules(): List<BlockScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: BlockScheduleEntity)

    @Delete
    suspend fun delete(schedule: BlockScheduleEntity)
}

@Dao
interface UsageSessionDao {
    @Query("SELECT * FROM usage_sessions WHERE date = :date AND platform = :platform ORDER BY startTime ASC")
    suspend fun getSessionsForDateAndPlatform(date: String, platform: String): List<UsageSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UsageSessionEntity): Long

    @Query("DELETE FROM usage_sessions")
    suspend fun clearAll()
}
