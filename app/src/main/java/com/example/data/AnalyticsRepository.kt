package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsRepository(
    private val dailyUsageDao: DailyUsageDao,
    private val platformGoalDao: PlatformGoalDao,
    private val blockScheduleDao: BlockScheduleDao,
    private val usageSessionDao: UsageSessionDao
) {
    val allUsageFlow: Flow<List<DailyUsageEntity>> = dailyUsageDao.getAllUsageFlow()
    val allGoalsFlow: Flow<List<PlatformGoalEntity>> = platformGoalDao.getAllGoalsFlow()
    val allSchedulesFlow: Flow<List<BlockScheduleEntity>> = blockScheduleDao.getAllSchedulesFlow()

    suspend fun getUsageForDate(date: String): List<DailyUsageEntity> = dailyUsageDao.getUsageForDate(date)

    suspend fun saveSession(platform: String, startTime: Long, endTime: Long) = withContext(Dispatchers.IO) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startTime))
        usageSessionDao.insertSession(
            UsageSessionEntity(
                date = date,
                platform = platform,
                startTime = startTime,
                endTime = endTime
            )
        )
    }

    suspend fun getSessionsForDateAndPlatform(date: String, platform: String): List<UsageSessionEntity> = withContext(Dispatchers.IO) {
        usageSessionDao.getSessionsForDateAndPlatform(date, platform)
    }

    suspend fun incrementUsage(platform: String, seconds: Long) = withContext(Dispatchers.IO) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existing = dailyUsageDao.getUsageForDateAndPlatform(date, platform)
        if (existing == null) {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    date = date,
                    platform = platform,
                    usageSeconds = seconds
                )
            )
        } else {
            dailyUsageDao.incrementUsageSeconds(date, platform, seconds)
        }
    }

    suspend fun setUsageForDate(date: String, platform: String, minutes: Long) = withContext(Dispatchers.IO) {
        val existing = dailyUsageDao.getUsageForDateAndPlatform(date, platform)
        val entity = if (existing == null) {
            DailyUsageEntity(date = date, platform = platform, usageSeconds = minutes * 60)
        } else {
            existing.copy(usageSeconds = minutes * 60)
        }
        dailyUsageDao.insertOrUpdate(entity)
    }

    suspend fun setUsageSecondsForDate(date: String, platform: String, seconds: Long, openCount: Int = 0) = withContext(Dispatchers.IO) {
        val existing = dailyUsageDao.getUsageForDateAndPlatform(date, platform)
        val entity = if (existing == null) {
            DailyUsageEntity(date = date, platform = platform, usageSeconds = seconds, openCount = openCount)
        } else {
            existing.copy(usageSeconds = seconds, openCount = openCount)
        }
        dailyUsageDao.insertOrUpdate(entity)
    }

    suspend fun saveGoal(platform: String, limitMinutes: Long, isEnabled: Boolean = true) = withContext(Dispatchers.IO) {
        platformGoalDao.insertOrUpdate(PlatformGoalEntity(platform, limitMinutes, isEnabled))
    }

    suspend fun deleteGoal(goal: PlatformGoalEntity) = withContext(Dispatchers.IO) {
        platformGoalDao.delete(goal)
    }

    suspend fun saveSchedule(schedule: BlockScheduleEntity) = withContext(Dispatchers.IO) {
        blockScheduleDao.insertOrUpdate(schedule)
    }

    suspend fun deleteSchedule(schedule: BlockScheduleEntity) = withContext(Dispatchers.IO) {
        blockScheduleDao.delete(schedule)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dailyUsageDao.clearAll()
        usageSessionDao.clearAll()
    }

    // Ensure database has rich seed data for immediate playability
    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        try {
            val existingGoals = platformGoalDao.getAllGoals()
            if (existingGoals.isEmpty()) {
                Log.d("AnalyticsRepository", "Database empty. Seeding dashboard stats...")

                // 1. Seed Limit Goals
                val seedGoals = listOf(
                    PlatformGoalEntity("Instagram", 60, true),
                    PlatformGoalEntity("TikTok", 45, true),
                    PlatformGoalEntity("YouTube", 90, true),
                    PlatformGoalEntity("Twitter/X", 40, true),
                    PlatformGoalEntity("Facebook", 30, false),
                    PlatformGoalEntity("LinkedIn", 30, true),
                    PlatformGoalEntity("WhatsApp", 60, true)
                )
                for (g in seedGoals) {
                    platformGoalDao.insertOrUpdate(g)
                }

                // 2. Seed Default Blocks
                val seedSchedules = listOf(
                    BlockScheduleEntity(
                        label = "9-to-5 Work Hours",
                        startTime = "09:00",
                        endTime = "17:00",
                        blockedPlatforms = "TikTok,Instagram,Facebook",
                        isEnabled = true
                    ),
                    BlockScheduleEntity(
                        label = "Evening Digital Detox",
                        startTime = "21:30",
                        endTime = "23:59",
                        blockedPlatforms = "TikTok,YouTube,Twitter/X",
                        isEnabled = false
                    )
                )
                for (s in seedSchedules) {
                    blockScheduleDao.insertOrUpdate(s)
                }

                Log.d("AnalyticsRepository", "Seeding completed successfully!")
            }
        } catch (e: Exception) {
            Log.e("AnalyticsRepository", "Error seeding database: ${e.message}", e)
        }
    }
}
