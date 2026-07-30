package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.ui.components.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScreentimeAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var activePlatform: String? = null
    private var sessionStartTime: Long = 0L
    private var tickerJob: Job? = null

    companion object {
        private const val TICKER_CHANNEL_ID = "social_limit_live_ticker"
        private const val TICKER_NOTIFICATION_ID = 8881
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Map packages to platform names used in our database (merge defaults + custom)
            val defaultPackageToPlatform = mapOf(
                "com.instagram.android" to "Instagram",
                "com.zhiliaoapp.musically" to "TikTok",
                "com.ss.android.ugc.aweme" to "TikTok",
                "com.google.android.youtube" to "YouTube",
                "com.twitter.android" to "Twitter/X",
                "com.facebook.katana" to "Facebook",
                "com.linkedin.android" to "LinkedIn",
                "com.whatsapp" to "WhatsApp",
                "com.whatsapp.w4b" to "WhatsApp"
            )

            val customMonitoredSet = getSharedPreferences("screentime_prefs", Context.MODE_PRIVATE)
                .getStringSet("custom_monitored_apps", null) ?: emptySet()

            val packageToPlatform = defaultPackageToPlatform.toMutableMap()
            for (entry in customMonitoredSet) {
                val parts = entry.split("|", limit = 2)
                if (parts.size == 2) {
                    packageToPlatform[parts[0]] = parts[1]
                }
            }

            val platformName = packageToPlatform[packageName]
            val currentActive = activePlatform
            val now = System.currentTimeMillis()

            if (platformName != null) {
                // Entered a tracked app
                if (currentActive == null) {
                    activePlatform = platformName
                    sessionStartTime = now
                    incrementOpenCountInDatabase(platformName)
                    startTicker(platformName)
                } else if (currentActive != platformName) {
                    saveSessionToDatabase(currentActive, sessionStartTime, now)
                    activePlatform = platformName
                    sessionStartTime = now
                    incrementOpenCountInDatabase(platformName)
                    startTicker(platformName)
                }
            } else {
                // Left tracked apps entirely to settings/home launcher
                if (currentActive != null) {
                    saveSessionToDatabase(currentActive, sessionStartTime, now)
                    activePlatform = null
                    sessionStartTime = 0L
                    stopTicker()
                }
            }

            // Check database to see if this platform is blocked right now
            if (platformName != null) {
                serviceScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        
                        var shouldBlock = false
                        var blockReason = ""

                        // 1. Check if Daily Limit is Exceeded
                        val usage = db.dailyUsageDao().getUsageForDateAndPlatform(todayStr, platformName)
                        val goal = db.platformGoalDao().getGoalForPlatform(platformName)

                        if (goal != null && goal.isEnabled) {
                            val limitMinutes = goal.dailyLimitMinutes
                            val currentMinutes = (usage?.usageSeconds ?: 0L) / 60
                            if (currentMinutes >= limitMinutes) {
                                shouldBlock = true
                                blockReason = "LIMIT_EXCEEDED"
                            }
                        }

                        // 2. Check Scheduled Focus Blocks
                        if (!shouldBlock) {
                            val activeSchedules = db.blockScheduleDao().getAllSchedules()
                            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            for (schedule in activeSchedules) {
                                if (schedule.isEnabled && isTimeInInterval(currentTime, schedule.startTime, schedule.endTime)) {
                                    val blockedList = schedule.blockedPlatforms.split(",").filter { it.isNotBlank() }
                                    if (blockedList.any { it.equals(platformName, ignoreCase = true) }) {
                                        shouldBlock = true
                                        blockReason = "SCHEDULED_BLOCK"
                                    }
                                }
                            }
                        }

                        // 3. Check Active Focus Pomodoro (stored in SharedPreferences)
                        if (!shouldBlock) {
                            val sharedPrefs = getSharedPreferences("screentime_prefs", Context.MODE_PRIVATE)
                            val focusEndTime = sharedPrefs.getLong("focus_session_end_time", 0L)
                            val isFocusActive = focusEndTime > System.currentTimeMillis()
                            if (isFocusActive) {
                                val blockedInFocus = sharedPrefs.getString("focus_blocked_platforms", "") ?: ""
                                val isBlockedPf = blockedInFocus.split(",").any { it.equals(platformName, ignoreCase = true) }
                                if (isBlockedPf) {
                                    shouldBlock = true
                                    blockReason = "POMODORO_LOCK"
                                }
                            }
                        }

                        if (shouldBlock) {
                            triggerLimitBlock(platformName, blockReason)
                        }
                    } catch (e: Exception) {
                        Log.e("ScreentimeAccessibility", "Error checking blocking status", e)
                    }
                }
            }
        }
    }

    private fun triggerLimitBlock(platformName: String, reason: String) {
        serviceScope.launch(Dispatchers.Main) {
            try {
                // Perform global action to automatically close the app (press HOME button)
                performGlobalAction(GLOBAL_ACTION_HOME)
                
                // Show the popup for usage exhausted (Intervention screen)
                val intent = Intent(applicationContext, InterventionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("blocked_platform", platformName)
                    putExtra("block_reason", reason)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("ScreentimeAccessibility", "Error triggering limit block", e)
            }
        }
    }

    private fun isTimeInInterval(current: String, start: String, end: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currDate = sdf.parse(current)
            val startDate = sdf.parse(start)
            val endDate = sdf.parse(end)
            if (currDate != null && startDate != null && endDate != null) {
                if (startDate.before(endDate)) {
                    currDate in startDate..endDate
                } else {
                    // Over midnight
                    currDate >= startDate || currDate <= endDate
                }
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override fun onInterrupt() {
        stopTicker()
    }

    override fun onDestroy() {
        stopTicker()
        super.onDestroy()
    }

    private fun startTicker(platformName: String) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (activePlatform == platformName) {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val usage = db.dailyUsageDao().getUsageForDateAndPlatform(todayStr, platformName)
                    val goal = db.platformGoalDao().getGoalForPlatform(platformName)

                    if (goal != null && goal.isEnabled) {
                        val limitSeconds = goal.dailyLimitMinutes * 60L
                        val currentSessionSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000
                        val totalSecondsUsed = (usage?.usageSeconds ?: 0L) + currentSessionSeconds
                        val remainingSeconds = limitSeconds - totalSecondsUsed

                        // Trigger/update persistent ticker notification widget when nearing limit (< 10 minutes, i.e. 600 seconds)
                        val sharedPrefs = getSharedPreferences("screentime_prefs", Context.MODE_PRIVATE)
                        val tickerEnabled = sharedPrefs.getBoolean("ticker_notification_enabled", true)
                        if (remainingSeconds > 0 && remainingSeconds <= 600L && tickerEnabled) {
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            val countdownText = String.format(Locale.getDefault(), "Counter: %02dm %02ds remaining before block!", mins, secs)
                            showTickerNotification(platformName, countdownText)
                        } else if (remainingSeconds <= 0) {
                            cancelTickerNotification()
                            triggerLimitBlock(platformName, "LIMIT_EXCEEDED")
                            break // Stop ticker loop as the app is closed and blocked
                        } else {
                            cancelTickerNotification()
                        }
                    } else {
                        cancelTickerNotification()
                    }
                } catch (e: Exception) {
                    Log.e("ScreentimeAccessibility", "Error checking platform limit countdown in ticker", e)
                }
                delay(1000L) // update the persistent notification counter widget every second
            }
            cancelTickerNotification()
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        cancelTickerNotification()
    }

    private fun createTickerChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Live Limit Countdown Ticker"
            val descriptionText = "Displays a persistent countdown widget when nearing daily usage limits."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(TICKER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showTickerNotification(platformName: String, text: String) {
        createTickerChannel()
        val builder = NotificationCompat.Builder(this, TICKER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$platformName Limit Counter")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // persists and cannot be swiped away while in use
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(TICKER_NOTIFICATION_ID, builder.build())
    }

    private fun cancelTickerNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(TICKER_NOTIFICATION_ID)
    }

    private fun incrementOpenCountInDatabase(platformName: String) {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentUsage = db.dailyUsageDao().getUsageForDateAndPlatform(todayStr, platformName)
                if (currentUsage != null) {
                    db.dailyUsageDao().insertOrUpdate(
                        currentUsage.copy(openCount = currentUsage.openCount + 1)
                    )
                } else {
                    db.dailyUsageDao().insertOrUpdate(
                        com.example.data.DailyUsageEntity(
                            date = todayStr,
                            platform = platformName,
                            usageSeconds = 0,
                            openCount = 1
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("ScreentimeAccessibility", "Error incrementing open count", e)
            }
        }
    }

    private fun saveSessionToDatabase(platform: String, start: Long, end: Long) {
        val durationMs = end - start
        if (durationMs < 1000) return // Skip tiny accidental triggers under 1s
        
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(start))
                
                // Add exact session trace to database
                db.usageSessionDao().insertSession(
                    com.example.data.UsageSessionEntity(
                        date = todayStr,
                        platform = platform,
                        startTime = start,
                        endTime = end
                    )
                )

                // Accumulate usage duration in daily usage as well!
                val durationSecs = durationMs / 1000
                if (durationSecs > 0) {
                    val existing = db.dailyUsageDao().getUsageForDateAndPlatform(todayStr, platform)
                    if (existing != null) {
                        db.dailyUsageDao().insertOrUpdate(
                            existing.copy(usageSeconds = existing.usageSeconds + durationSecs)
                        )
                    } else {
                        db.dailyUsageDao().insertOrUpdate(
                            com.example.data.DailyUsageEntity(
                                date = todayStr,
                                platform = platform,
                                usageSeconds = durationSecs,
                                openCount = 0
                            )
                        )
                    }
                }
                Log.d("ScreentimeAccessibility", "Successfully saved session trace for $platform: $start to $end (${durationSecs}s)")
            } catch (e: Exception) {
                Log.e("ScreentimeAccessibility", "Error logging session trace to DB", e)
            }
        }
    }
}
