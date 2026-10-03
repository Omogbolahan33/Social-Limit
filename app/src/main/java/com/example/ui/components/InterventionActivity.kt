package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val platform = intent.getStringExtra("blocked_platform") ?: "Social App"
        val reason = intent.getStringExtra("block_reason") ?: "LIMIT_EXCEEDED"

        // Trigger gentle double haptic vibration on entry
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 150, 100, 150)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrandColors.DarkSlateBg
                ) {
                    InterventionContent(
                        platform = platform,
                        reason = reason,
                        onGoHome = {
                            // Minimize/go home
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InterventionContent(
    platform: String,
    reason: String,
    onGoHome: () -> Unit
) {
    val platformColor = BrandColors.getPlatformColor(platform)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Countdown and cooling states
    var secondsLeft by remember { mutableStateOf(0L) }
    var currentEfficiency by remember { mutableStateOf(100f) }

    // Ticking logic recalculates remaining cooling duration
    LaunchedEffect(reason, platform) {
        while (true) {
            val now = System.currentTimeMillis()
            val sharedPrefs = context.getSharedPreferences("screentime_prefs", Context.MODE_PRIVATE)
            
            when (reason) {
                "LIMIT_EXCEEDED" -> {
                    val calendar = Calendar.getInstance()
                    val currentMs = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    secondsLeft = ((calendar.timeInMillis - currentMs) / 1000L).coerceAtLeast(0L)
                }
                "SCHEDULED_BLOCK" -> {
                    try {
                        val db = com.example.data.AppDatabase.getDatabase(context)
                        val schedules = db.blockScheduleDao().getAllSchedules()
                        val activeSchedule = schedules.firstOrNull { schedule ->
                            if (schedule.isEnabled && schedule.blockedPlatforms.split(",").any { it.equals(platform, ignoreCase = true) }) {
                                val curHHmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                isTimeInIntervalLocal(curHHmm, schedule.startTime, schedule.endTime)
                            } else false
                        }
                        if (activeSchedule != null) {
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val currHHmm = sdf.format(Date())
                            val curDate = sdf.parse(currHHmm)
                            val endDate = sdf.parse(activeSchedule.endTime)
                            if (curDate != null && endDate != null) {
                                var diffMs = endDate.time - curDate.time
                                if (diffMs < 0) {
                                    diffMs += 24 * 60 * 60 * 1000L
                                }
                                secondsLeft = (diffMs / 1000L).coerceAtLeast(0L)
                            } else {
                                secondsLeft = 3600L
                            }
                        } else {
                            secondsLeft = 0L
                        }
                    } catch (e: Exception) {
                        secondsLeft = 1800L
                    }
                }
                "POMODORO_LOCK" -> {
                    val endTime = sharedPrefs.getLong("focus_session_end_time", 0L)
                    secondsLeft = ((endTime - now) / 1000L).coerceAtLeast(0L)
                }
                "THROTTLE_CUTOFF" -> {
                    val cooloffUntil = sharedPrefs.getLong("throttle_cooloff_until_$platform", 0L)
                    secondsLeft = ((cooloffUntil - now) / 1000L).coerceAtLeast(0L)
                }
                "EFFICIENCY_DRAIN" -> {
                    try {
                        val db = com.example.data.AppDatabase.getDatabase(context)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val usages = db.dailyUsageDao().getUsageForDate(todayStr)
                        var totalSeconds = usages.sumOf { it.usageSeconds }
                        val totalMins = totalSeconds / 60f

                        val allSessions = db.usageSessionDao().getSessionsForDate(todayStr)
                        val earliestSession = allSessions.minByOrNull { it.startTime }
                        
                        val daytimeSoFarMinutes = if (earliestSession != null) {
                            val elapsedMs = System.currentTimeMillis() - earliestSession.startTime
                            (elapsedMs / 60000f).coerceAtLeast(1f)
                        } else {
                            1f
                        }
                        
                        val curRatio = (totalMins / daytimeSoFarMinutes).coerceIn(0f, 1f)
                        currentEfficiency = 100f - (curRatio * 100f)

                        val threshold = sharedPrefs.getInt("efficiency_lock_threshold", 80).toFloat()
                        val fraction = (100f - threshold) / 100f
                        
                        if (fraction > 0f && earliestSession != null) {
                            val targetDaytimeSoFarMinutes = totalMins / fraction
                            val targetElapsedTimeMs = (targetDaytimeSoFarMinutes * 60000f).toLong()
                            val targetTimeMs = earliestSession.startTime + targetElapsedTimeMs
                            val remainingMs = targetTimeMs - System.currentTimeMillis()
                            secondsLeft = (remainingMs / 1000L).coerceAtLeast(0L)
                        } else {
                            secondsLeft = 300L
                        }
                    } catch (e: Exception) {
                        secondsLeft = 420L
                    }
                }
                else -> {
                    secondsLeft = 0L
                }
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    // Guided breathing loop animation
    val infiniteTransition = rememberInfiniteTransition(label = "respiration")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Breathing text label helper
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_text"
    )

    val breathingStateText = if (breatheScale > 1.05f) "Inhale deeply..." else "Exhale cleanly..."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandColors.DarkSlateBg,
                        BrandColors.TonalSurface,
                        BrandColors.DarkSlateBg
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Header Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            val isThrottled = reason == "THROTTLE_CUTOFF"
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (isThrottled) BrandColors.SolarAmber.copy(alpha = 0.15f) else BrandColors.ErrorRose.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isThrottled) Icons.Default.HourglassBottom else Icons.Default.Block,
                    contentDescription = "Social Limits Active",
                    tint = if (isThrottled) BrandColors.SolarAmber else BrandColors.ErrorRose,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isThrottled) "YOU'VE BEEN CUT OFF • COOL-OFF MANDATORY" else "SOCIAL LEAK SHIELD ENGAGED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isThrottled) BrandColors.SolarAmber else BrandColors.ErrorRose,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        // MIDDLE: Digital Shield Warning and Breathing Coach Bubble
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Block description card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlatformAppIcon(
                    platformName = platform,
                    size = 52.dp,
                    clipShape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$platform is Locked!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                val blockInfo = when (reason) {
                    "LIMIT_EXCEEDED" -> "You have reached your configured daily usage limit for $platform today. Focus on offline wellness!"
                    "SCHEDULED_BLOCK" -> "A scheduled Focus block is currently active, restricting $platform."
                    "POMODORO_LOCK" -> "Pomodoro Focus Sprint session is active! Social networks are locked."
                    "EFFICIENCY_DRAIN" -> "Your Daylight Screen Efficiency has dropped below your set threshold! Take a cool-off break to let it rise back to an optimal level."
                    "THROTTLE_CUTOFF" -> "You've been cut off from $platform! Just like being cut off at a beer parlor, you used the app continuously for an extended stretch. Step away and cool off before returning."
                    else -> "Access to $platform is restricted to keep you focused."
                }

                Text(
                    text = blockInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.SlateGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                )
            }

            // Real-time Countdown Timer Frame - SENSE OF WHEN SOCIAL REOPENS!
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandColors.PrimaryIndigo.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.PrimaryIndigo.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val labelText = when (reason) {
                        "LIMIT_EXCEEDED" -> "Daily limit resets in:"
                        "SCHEDULED_BLOCK" -> "Focus schedule finishes in:"
                        "POMODORO_LOCK" -> "Focus Sprint completed in:"
                        "EFFICIENCY_DRAIN" -> "Cooling off until efficiency reaches target:"
                        else -> "Re-opens in:"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = BrandColors.SecondaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandColors.SlateGray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val hh = secondsLeft / 3600
                    val mm = (secondsLeft % 3600) / 60
                    val ss = secondsLeft % 60
                    val timeString = if (hh > 0) {
                        String.format(Locale.getDefault(), "%02d:%02d:%02d", hh, mm, ss)
                    } else {
                        String.format(Locale.getDefault(), "%02d:%02d", mm, ss)
                    }

                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.headlineLarge,
                        color = BrandColors.SecondaryTeal,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    if (reason == "EFFICIENCY_DRAIN") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "Current: %.1f%% / Needs to rise above threshold", currentEfficiency),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandColors.ErrorRose,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Guided Respiration Circle Frame
            Text(
                text = "TAKE A MINDFUL BREATH",
                style = MaterialTheme.typography.labelSmall,
                color = BrandColors.PrimaryIndigo,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(breatheScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(platformColor.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Middle glowing bubble
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(breatheScale * 0.95f)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(platformColor, BrandColors.PrimaryIndigo)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = breathingStateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = breatheAlpha),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // BOTTOM: Safety Action Row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandColors.PrimaryIndigo
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(52.dp)
            ) {
                Text(
                    text = "Acknowledge & Return to Safety",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Text(
                text = "Resisting scrolling impulses increases focus efficiency by up to 40%.",
                style = MaterialTheme.typography.labelSmall,
                color = BrandColors.SlateGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}

private fun isTimeInIntervalLocal(current: String, start: String, end: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currDate = sdf.parse(current)
        val startDate = sdf.parse(start)
        val endDate = sdf.parse(end)
        if (currDate != null && startDate != null && endDate != null) {
            if (startDate.before(endDate)) {
                currDate in startDate..endDate
            } else {
                currDate >= startDate || currDate <= endDate
            }
        } else false
    } catch (e: Exception) {
        false
    }
}
