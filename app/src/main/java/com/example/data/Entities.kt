package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // ISO Format, yyyy-MM-dd
    val platform: String,        // Instagram, TikTok, YouTube, Twitter/X, Facebook, etc.
    val usageSeconds: Long,      // Active tracking seconds
    val openCount: Int = 0       // Number of times launched/opened
)

@Entity(tableName = "platform_goals")
data class PlatformGoalEntity(
    @PrimaryKey val platform: String,
    val dailyLimitMinutes: Long,
    val isEnabled: Boolean = true
)

@Entity(tableName = "block_schedules")
data class BlockScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,           // e.g. "Work Hours", "Deep Focus"
    val startTime: String,       // HH:mm format, e.g. "09:00"
    val endTime: String,         // HH:mm format, e.g. "17:00"
    val blockedPlatforms: String, // Comma-separated list details, e.g. "Instagram,TikTok"
    val isEnabled: Boolean = true
)

@Entity(tableName = "usage_sessions")
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // ISO Format, yyyy-MM-dd
    val platform: String,        // Instagram, TikTok, YouTube, Twitter/X, Facebook, WhatsApp, etc.
    val startTime: Long,         // Timestamp in ms
    val endTime: Long            // Timestamp in ms
)
