package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyUsageEntity
import com.example.data.PlatformGoalEntity
import com.example.viewmodel.ScreentimeViewModel
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: ScreentimeViewModel,
    modifier: Modifier = Modifier
) {
    val usageList by viewModel.allUsage.collectAsState()
    val goalList by viewModel.allGoals.collectAsState()
    val modelAlerts by viewModel.activeAlerts.collectAsState()
    val customMonitoredApps by viewModel.customMonitoredApps.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasUsageStatsPermission by remember { mutableStateOf(false) }
    var selectedPlatformForDetails by remember { mutableStateOf<String?>(null) }
    var selectedChartTab by remember { mutableStateOf(0) }
    var showDaylightRatioDetails by remember { mutableStateOf(false) }
    var showTodayBreakdownDialog by remember { mutableStateOf(false) }
    var showWeeklyBreakdownDialog by remember { mutableStateOf(false) }

    // Determine current Today Date String dynamically from ViewModel StateFlow
    val todayStr by viewModel.currentDate.collectAsState()

    // Today's platform break down helper
    val todaysUsages = remember(usageList, todayStr) {
        usageList.filter { it.date == todayStr }
    }

    // Dynamic calculations for circular indicator
    val todaysTotalMinutes = remember(todaysUsages) {
        todaysUsages.sumOf { it.usageSeconds } / 60
    }

    val allSessions by viewModel.allPlatformsTodaySessions.collectAsState()
    val dayStartMinutes by viewModel.dayStartMinutes.collectAsState()
    val dayStartTimestamp by viewModel.dayStartTimestamp.collectAsState()

    val dayStartStr = remember(dayStartMinutes) {
        val dm = dayStartMinutes
        if (dm != null) {
            val h = dm / 60
            val m = dm % 60
            val ampm = if (h >= 12) "PM" else "AM"
            val displayHour = if (h % 12 == 0) 12 else h % 12
            String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, ampm)
        } else {
            "--:--"
        }
    }

    val daytimeSoFarMinutes = remember(dayStartMinutes) {
        val dm = dayStartMinutes
        if (dm != null) {
            val currentCal = Calendar.getInstance()
            val currentMinutes = currentCal.get(Calendar.HOUR_OF_DAY) * 60 + currentCal.get(Calendar.MINUTE)
            (currentMinutes - dm).coerceAtLeast(1).toFloat()
        } else {
            1f
        }
    }

    val daylightSecondsSpent = remember(allSessions, dayStartTimestamp, todaysUsages, daytimeSoFarMinutes) {
        val epochMs = dayStartTimestamp
        if (epochMs != null) {
            if (allSessions.isNotEmpty()) {
                allSessions.sumOf { sess ->
                    if (sess.endTime <= epochMs) {
                        0L
                    } else {
                        val effectiveStart = maxOf(sess.startTime, epochMs)
                        ((sess.endTime - effectiveStart) / 1000L).coerceAtLeast(0L)
                    }
                }
            } else {
                val elapsedSecs = (daytimeSoFarMinutes * 60f).toLong()
                (todaysUsages.sumOf { it.usageSeconds }).coerceAtMost(elapsedSecs)
            }
        } else {
            0L
        }
    }

    val maxUsagePlatform = remember(todaysUsages) {
        if (todaysUsages.isNotEmpty()) todaysUsages.maxByOrNull { it.usageSeconds } else null
    }
    val combinedRatio = remember(daytimeSoFarMinutes, daylightSecondsSpent) {
        val elapsedSecs = daytimeSoFarMinutes * 60f
        if (elapsedSecs > 0f) (daylightSecondsSpent.toFloat() / elapsedSecs).coerceIn(0f, 1f) else 0f
    }
    val combinedPercent = (combinedRatio * 100).toInt()
    val singleRatioPercent = remember(maxUsagePlatform, daytimeSoFarMinutes) {
        if (maxUsagePlatform != null && daytimeSoFarMinutes > 0) {
            val singlePlatformMins = maxUsagePlatform.usageSeconds / 60f
            ((singlePlatformMins / daytimeSoFarMinutes) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    LaunchedEffect(todaysUsages) {
        viewModel.loadAllPlatformsTodaySessions(todaysUsages)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = viewModel.hasUsageStatsPermission()
                hasUsageStatsPermission = granted
                if (granted) {
                    viewModel.syncActualUsage()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val todaysGoalTotal = remember(goalList) {
        val active = goalList.filter { it.isEnabled }
        if (active.isEmpty()) 150L else active.sumOf { it.dailyLimitMinutes }
    }

    val overallPercentage = remember(todaysTotalMinutes, todaysGoalTotal) {
        if (todaysGoalTotal > 0) (todaysTotalMinutes.toFloat() / todaysGoalTotal.toFloat()) else 0f
    }

    // Weekly trend processor (Last 7 days calculation)
    val weeklyDayTimePairs = remember(usageList, todayStr) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val parsedToday = try {
            sdf.parse(todayStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val list = mutableListOf<Pair<String, Long>>()

        for (dayAgo in (0..6).reversed()) {
            calendar.time = parsedToday
            calendar.add(Calendar.DAY_OF_YEAR, -dayAgo)
            val loopDate = sdf.format(calendar.time)
            val lbl = dayFormat.format(calendar.time)
            
            val secondsForDay = usageList.filter { it.date == loopDate }.sumOf { it.usageSeconds }
            list.add(Pair(lbl, secondsForDay / 60))
        }
        list
    }



    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        // System Usage permission request banner
        if (!hasUsageStatsPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Active Analytics Tracking",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REAL-TIME TRACKING INACTIVE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To track daily active minutes on other apps (Instagram, TikTok, YouTube etc.) on this phone automatically, please grant Usage Access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (e: Exception) {
                                    // Fallback if settings page fails to launch
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant Usage Permission", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Main Dashboard Gauge Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { selectedChartTab = 0 },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (selectedChartTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedChartTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Today, contentDescription = "Today", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Today's Progress", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        TextButton(
                            onClick = { selectedChartTab = 1 },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (selectedChartTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedChartTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BarChart, contentDescription = "Weekly Trends", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Weekly Trends", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedChartTab == 0) {
                        val todaysTotalMinutesStr = if (todaysTotalMinutes >= 60) {
                            val h = todaysTotalMinutes / 60
                            val m = todaysTotalMinutes % 60
                            if (m > 0) "${h}h ${m}m" else "${h}h"
                        } else {
                            "${todaysTotalMinutes}m"
                        }
                        val radialGoalMinutesStr = if (todaysGoalTotal >= 60) {
                            val h = todaysGoalTotal / 60
                            val m = todaysGoalTotal % 60
                            if (m > 0) "${h}h ${m}m" else "${h}h"
                        } else {
                            "${todaysGoalTotal}m"
                        }

                        // Full-width Card Header: TODAY'S USAGE + Sync on left, Day Start on right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "TODAY'S USAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SecondaryTeal,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                // Tiny unobtrusive sync icon
                                IconButton(
                                    onClick = {
                                        viewModel.syncActualUsage()
                                        Toast.makeText(context, "Usage synced", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = BrandColors.SlateGray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            // Compact day start indicator (Clickable to view detailed day start & sunlight timeline)
                            Surface(
                                onClick = { showDaylightRatioDetails = true },
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = "Day start",
                                        tint = BrandColors.NeonAmber,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = dayStartStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandColors.SoftWhite,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Content Row: Metrics on Left, Gauge on Right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = todaysTotalMinutesStr,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = BrandColors.SoftWhite,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.clickable { showTodayBreakdownDialog = true }
                                )

                                // Smart Compact Daylight Efficiency Pill (Clickable to open breakdown & chronological timeline)
                                Surface(
                                    onClick = { showDaylightRatioDetails = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        combinedPercent < 15 -> Color(0xFF2E7D32).copy(alpha = 0.10f)
                                        combinedPercent < 35 -> Color(0xFFEF6C00).copy(alpha = 0.12f)
                                        else -> BrandColors.ErrorRose.copy(alpha = 0.12f)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when {
                                            combinedPercent < 15 -> Color(0xFF2E7D32).copy(alpha = 0.30f)
                                            combinedPercent < 35 -> Color(0xFFEF6C00).copy(alpha = 0.35f)
                                            else -> BrandColors.ErrorRose.copy(alpha = 0.35f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("daylight_efficiency_pill")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when {
                                                        combinedPercent < 15 -> Icons.Default.CheckCircle
                                                        combinedPercent < 35 -> Icons.Default.WarningAmber
                                                        else -> Icons.Default.ErrorOutline
                                                    },
                                                    contentDescription = "Daylight rating",
                                                    tint = when {
                                                        combinedPercent < 15 -> Color(0xFF2E7D32)
                                                        combinedPercent < 35 -> Color(0xFFEF6C00)
                                                        else -> BrandColors.ErrorRose
                                                    },
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$combinedPercent% Sun Spent",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        combinedPercent < 15 -> Color(0xFF2E7D32)
                                                        combinedPercent < 35 -> Color(0xFFEF6C00)
                                                        else -> BrandColors.ErrorRose
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "View timeline and breakdown",
                                                tint = BrandColors.SlateGray,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Mini Daylight Allocation progress meter
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .background(BrandColors.SlateGray.copy(alpha = 0.15f), shape = RoundedCornerShape(2.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(combinedRatio)
                                                    .height(4.dp)
                                                    .background(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(BrandColors.NeonAmber, BrandColors.ErrorRose)
                                                        ),
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }

                                        // Top leak platform alert if any
                                        if (maxUsagePlatform != null && maxUsagePlatform.usageSeconds > 0) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                PlatformAppIcon(
                                                    platformName = maxUsagePlatform.platform,
                                                    size = 13.dp,
                                                    clipShape = RoundedCornerShape(3.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${maxUsagePlatform.platform}: $singleRatioPercent% daylight",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = BrandColors.SoftWhite,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Right column: Dual Concentric Radial Chart
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(0.95f)
                            ) {
                                ScreentimeRadialChart(
                                    percentage = overallPercentage,
                                    totalMinutesStr = todaysTotalMinutesStr,
                                    goalMinutesStr = radialGoalMinutesStr,
                                    daylightPercentage = combinedRatio,
                                    modifier = Modifier.clickable { showTodayBreakdownDialog = true }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showWeeklyBreakdownDialog = true }
                                .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                        ) {
                            WeeklyTrendsBarChart(
                                days = weeklyDayTimePairs,
                                peakLimitMins = todaysGoalTotal,
                                containerColor = Color.Transparent,
                                border = null
                            )
                        }
                    }
                }
            }
        }

        // Platform Break down List
        item {
            Text(
                text = "Platform breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.SoftWhite,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        val defaultPlatforms = listOf("Instagram", "TikTok", "YouTube", "Twitter/X", "Facebook", "LinkedIn", "WhatsApp")
        val allMonitoredPlatforms = (defaultPlatforms + customMonitoredApps.values).distinct()
        val renderedUsages = allMonitoredPlatforms.map { platform ->
            val match = todaysUsages.firstOrNull { it.platform.equals(platform, ignoreCase = true) }
            match ?: DailyUsageEntity(date = todayStr, platform = platform, usageSeconds = 0, openCount = 0)
        }

        items(renderedUsages) { usage ->
            val platformGoal = goalList.firstOrNull { it.platform.equals(usage.platform, ignoreCase = true) }
            val limitMinutes = platformGoal?.dailyLimitMinutes ?: 0L
            val isLimitEnabled = platformGoal?.isEnabled == true
            
            val platformMinutes = usage.usageSeconds / 60
            val secondsRemaining = usage.usageSeconds % 60
            
            val limitRatio = if (isLimitEnabled && limitMinutes > 0) {
                (platformMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val platformColor = BrandColors.getPlatformColor(usage.platform)

            Card(
                colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { selectedPlatformForDetails = usage.platform }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformAppIcon(
                        platformName = usage.platform,
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = usage.platform,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite
                                )
                                if (usage.openCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(BrandColors.PrimaryIndigo.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${usage.openCount} opens",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandColors.PrimaryIndigo,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            val totalMinutes = usage.usageSeconds / 60
                            val displayTimeStr = if (totalMinutes > 60) {
                                String.format(Locale.getDefault(), "%02d:%02d", totalMinutes / 60, totalMinutes % 60)
                            } else {
                                if (platformMinutes > 0 || secondsRemaining > 0) "${platformMinutes}m ${secondsRemaining}s" else "0m"
                            }
                            Text(
                                text = displayTimeStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Progress Bar comparing with limit
                        if (isLimitEnabled && limitMinutes > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(BrandColors.SlateGray.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(limitRatio)
                                        .height(6.dp)
                                        .background(
                                            if (limitRatio >= 1f) BrandColors.ErrorRose else platformColor,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val limitMinutesStr = if (limitMinutes > 60) {
                                String.format(Locale.getDefault(), "%02d:%02d", limitMinutes / 60, limitMinutes % 60)
                            } else {
                                "${limitMinutes}m"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Goal Limit: $limitMinutesStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandColors.SlateGray
                                )
                                Text(
                                    text = if (platformMinutes >= limitMinutes) "Exceeded!" else "${(limitRatio * 100).toInt()}% Used",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (platformMinutes >= limitMinutes) BrandColors.ErrorRose else BrandColors.SecondaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "No daily limit active",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandColors.SlateGray
                            )
                        }
                    }
                }
            }
        }

    }

    // Modal detailed popover for clicked platform
    selectedPlatformForDetails?.let { platform ->
        PlatformGranularDetailsDialog(
            platform = platform,
            usageList = usageList,
            viewModel = viewModel,
            onDismiss = { selectedPlatformForDetails = null }
        )
    }

    if (showDaylightRatioDetails) {
        DaylightEfficiencyDetailsDialog(
            dayStartMinutes = dayStartMinutes,
            dayStartStr = dayStartStr,
            daytimeSoFarMinutes = daytimeSoFarMinutes,
            todaysTotalMinutes = todaysTotalMinutes,
            daylightSecondsSpent = daylightSecondsSpent,
            todaysUsages = todaysUsages,
            allSessions = allSessions,
            onDismiss = { showDaylightRatioDetails = false }
        )
    }

    if (showTodayBreakdownDialog) {
        TodayBreakdownDialog(
            todaysTotalMinutes = todaysTotalMinutes,
            todaysGoalTotal = todaysGoalTotal,
            combinedPercent = combinedPercent,
            todaysUsages = todaysUsages,
            goalList = goalList,
            onDismiss = { showTodayBreakdownDialog = false }
        )
    }

    if (showWeeklyBreakdownDialog) {
        WeeklyBreakdownDialog(
            weeklyDayTimePairs = weeklyDayTimePairs,
            usageList = usageList,
            goalTotal = todaysGoalTotal,
            onDismiss = { showWeeklyBreakdownDialog = false }
        )
    }
}

@Composable
fun TodayBreakdownDialog(
    todaysTotalMinutes: Long,
    todaysGoalTotal: Long,
    combinedPercent: Int,
    todaysUsages: List<DailyUsageEntity>,
    goalList: List<PlatformGoalEntity>,
    onDismiss: () -> Unit
) {
    val totalHours = todaysTotalMinutes / 60
    val totalMinsRem = todaysTotalMinutes % 60
    val totalTimeStr = if (totalHours > 0) "${totalHours}h ${totalMinsRem}m" else "${todaysTotalMinutes}m"
    val isOverGoal = todaysTotalMinutes > todaysGoalTotal && todaysGoalTotal > 0
    val sortedUsages = remember(todaysUsages) {
        todaysUsages.sortedByDescending { it.usageSeconds }
    }
    val totalOpens = remember(todaysUsages) {
        todaysUsages.sumOf { it.openCount }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY'S USAGE BREAKDOWN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.SoftWhite
                        )
                        Text(
                            text = "Comprehensive daily activity report",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandColors.SlateGray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BrandColors.SlateGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Time", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = totalTimeStr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverGoal) BrandColors.ErrorRose else BrandColors.SoftWhite
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Daily Goal", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${todaysGoalTotal}m",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("App Opens", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalOpens",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.PrimaryIndigo
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "App-by-App Breakdown",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.PrimaryIndigo
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sortedUsages.isEmpty() || sortedUsages.all { it.usageSeconds == 0L }) {
                            Text(
                                text = "No social media usage logged yet today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            sortedUsages.filter { it.usageSeconds > 0 }.forEach { usage ->
                                val platformColor = BrandColors.getPlatformColor(usage.platform)
                                val pMins = usage.usageSeconds / 60
                                val pSecs = usage.usageSeconds % 60
                                val timeText = if (pMins > 0) "${pMins}m ${pSecs}s" else "${pSecs}s"
                                val pctOfTotal = if (todaysTotalMinutes > 0) {
                                    ((pMins.toFloat() / todaysTotalMinutes) * 100).toInt()
                                } else 0

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            PlatformAppIcon(
                                                platformName = usage.platform,
                                                size = 32.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = usage.platform,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandColors.SoftWhite
                                                )
                                                Text(
                                                    text = "${usage.openCount} opens • $pctOfTotal% of today",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrandColors.SlateGray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandColors.SoftWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = BrandColors.PrimaryIndigo)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun WeeklyBreakdownDialog(
    weeklyDayTimePairs: List<Pair<String, Long>>,
    usageList: List<DailyUsageEntity>,
    goalTotal: Long,
    onDismiss: () -> Unit
) {
    val totalWeeklyMinutes = remember(weeklyDayTimePairs) {
        weeklyDayTimePairs.sumOf { it.second }
    }
    val avgDailyMinutes = if (weeklyDayTimePairs.isNotEmpty()) totalWeeklyMinutes / weeklyDayTimePairs.size else 0L
    val peakDay = remember(weeklyDayTimePairs) {
        weeklyDayTimePairs.maxByOrNull { it.second }
    }
    val maxVal = maxOf(weeklyDayTimePairs.maxOfOrNull { it.second } ?: 1L, goalTotal, 1L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WEEKLY TRENDS BREAKDOWN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.SoftWhite
                        )
                        Text(
                            text = "Past 7 days screentime breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandColors.SlateGray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BrandColors.SlateGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("7-Day Total", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            val h = totalWeeklyMinutes / 60
                            val m = totalWeeklyMinutes % 60
                            val tStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                            Text(tStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandColors.SoftWhite)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Daily Average", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            val h = avgDailyMinutes / 60
                            val m = avgDailyMinutes % 60
                            val tStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                            Text(tStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandColors.PrimaryIndigo)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Peak Day", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            val peakStr = if (peakDay != null && peakDay.second > 0) "${peakDay.first} (${peakDay.second}m)" else "None"
                            Text(peakStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandColors.ErrorRose)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Day-by-Day Activity",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.PrimaryIndigo
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weeklyDayTimePairs.forEach { (dayName, mins) ->
                            val isOverGoal = mins > goalTotal && goalTotal > 0
                            val isPeak = peakDay != null && dayName == peakDay.first && mins > 0
                            val ratio = (mins.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = dayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandColors.SoftWhite
                                            )
                                            if (isPeak) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(BrandColors.ErrorRose.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Peak", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandColors.ErrorRose)
                                                }
                                            } else if (isOverGoal) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(BrandColors.NeonAmber.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Over Limit", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandColors.NeonAmber)
                                                }
                                            }
                                        }

                                        val h = mins / 60
                                        val m = mins % 60
                                        val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverGoal) BrandColors.ErrorRose else BrandColors.SoftWhite
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .background(BrandColors.SlateGray.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(ratio)
                                                .height(5.dp)
                                                .background(
                                                    if (isOverGoal) BrandColors.ErrorRose else BrandColors.PrimaryIndigo,
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = BrandColors.PrimaryIndigo)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformGranularDetailsDialog(
    platform: String,
    usageList: List<DailyUsageEntity>,
    viewModel: ScreentimeViewModel,
    onDismiss: () -> Unit
) {
    val platformColor = BrandColors.getPlatformColor(platform)
    var selectedTab by remember { mutableStateOf(0) } // 0 = DAY, 1 = WEEK, 2 = MONTH
    
    val today by viewModel.currentDate.collectAsState()

    // Retrieve today's usage seconds
    val todayUsageSecs = remember(usageList, platform, today) {
        usageList.firstOrNull { it.date == today && it.platform.equals(platform, ignoreCase = true) }?.usageSeconds ?: 0L
    }
    val todayMins = todayUsageSecs / 60
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Title Header with details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformAppIcon(
                        platformName = platform,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = platform.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = BrandColors.SoftWhite
                        )
                        val todayOpens = remember(usageList, platform, today) {
                            usageList.firstOrNull { it.date == today && it.platform.equals(platform, ignoreCase = true) }?.openCount ?: 0
                        }
                        val todayTimeStr = if (todayMins > 60) {
                            String.format(Locale.getDefault(), "%02d:%02d", todayMins / 60, todayMins % 60)
                        } else {
                            "${todayMins}m"
                        }
                        Text(
                            text = "Today: $todayTimeStr ($todayOpens opens)",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandColors.SlateGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = BrandColors.SlateGray)
                    }
                }

                // Scrollable tab row selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BrandColors.PrimaryIndigo,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = platformColor
                            )
                        }
                    },
                    divider = {},
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Day Pattern", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) platformColor else BrandColors.SlateGray) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Week trends", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) platformColor else BrandColors.SlateGray) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("30-Day Map", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selectedTab == 2) platformColor else BrandColors.SlateGray) }
                    )
                }

                // Tab Content Renderers
                when (selectedTab) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "24-Hour Activity Timeline:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )

                            val sessionsState by viewModel.selectedPlatformSessions.collectAsState()
                            LaunchedEffect(platform) {
                                viewModel.loadSessionsForPlatform(platform)
                            }

                            if (sessionsState.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandColors.TonalSurface.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timeline,
                                            contentDescription = "Timeline",
                                            tint = BrandColors.SlateGray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "No recorded sessions for today yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = BrandColors.SlateGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Launch and use $platform foreground tasks to generate logs.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandColors.SlateGray.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sessionsState.forEach { sess ->
                                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                                        val startStr = timeFormat.format(Date(sess.startTime))
                                        val endStr = timeFormat.format(Date(sess.endTime))
                                        
                                        val durationMs = sess.endTime - sess.startTime
                                        val durationSecs = durationMs / 1000
                                        val durationMins = durationSecs / 60
                                        val durationSecsLeft = durationSecs % 60
                                        val durationStr = if (durationMins > 60) String.format(Locale.getDefault(), "%02d:%02d", durationMins / 60, durationMins % 60) else if (durationMins > 0) {
                                            "${durationMins}m ${durationSecsLeft}s"
                                        } else {
                                            "${durationSecsLeft}s"
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = BrandColors.TonalSurface,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(platformColor, shape = CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "$startStr - $endStr",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandColors.SoftWhite
                                                )
                                                Text(
                                                    text = "Session duration: $durationStr",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrandColors.SlateGray
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Session",
                                                tint = platformColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Estimated segment breakdown:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SlateGray
                            )

                            val morningRatio = 0.15f
                            val afternoonRatio = 0.35f
                            val eveningRatio = 0.40f
                            val nightRatio = 0.10f

                            val morningMins = (todayMins * morningRatio).toInt()
                            val afternoonMins = (todayMins * afternoonRatio).toInt()
                            val eveningMins = (todayMins * eveningRatio).toInt()
                            val nightMins = (todayMins * nightRatio).toInt()

                            DayPartProgressRow("Morning (06:00 - 12:00)", morningMins, morningRatio, platformColor)
                            DayPartProgressRow("Afternoon (12:00 - 17:00)", afternoonMins, afternoonRatio, platformColor)
                            DayPartProgressRow("Evening (17:00 - 21:00)", eveningMins, eveningRatio, platformColor)
                            DayPartProgressRow("Night (21:00 - 06:00)", nightMins, nightRatio, platformColor)

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 Tip: Scroll locking in evening hours secures 40% daytime recovery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    1 -> {
                        // WEEK TRENDS - COMPARATIVE SECONDS READ
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Last 7 calendar days history logs:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )

                            val weekDays = remember(usageList, platform, today) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val daySdf = SimpleDateFormat("EEE", Locale.getDefault())
                                val calendar = Calendar.getInstance()
                                val parsedToday = try {
                                    sdf.parse(today) ?: Date()
                                } catch (e: Exception) {
                                    Date()
                                }
                                val list = mutableListOf<Triple<String, Long, Int>>()
                                for (dayAgo in (0..6).reversed()) {
                                    calendar.time = parsedToday
                                    calendar.add(Calendar.DAY_OF_YEAR, -dayAgo)
                                    val dateKey = sdf.format(calendar.time)
                                    val label = daySdf.format(calendar.time)
                                    val match = usageList.firstOrNull { it.date == dateKey && it.platform.equals(platform, ignoreCase = true) }
                                    val secondsForDay = match?.usageSeconds ?: 0L
                                    val opensForDay = match?.openCount ?: 0
                                    list.add(Triple(label, secondsForDay / 60, opensForDay))
                                }
                                list
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                weekDays.forEach { (lbl, mins, opens) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(lbl, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandColors.SlateGray, modifier = Modifier.width(48.dp))
                                        
                                        val barMax = weekDays.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 100L
                                        val animatedWidth = mins.toFloat() / barMax.toFloat()
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .background(BrandColors.SlateGray.copy(alpha = 0.08f), shape = RoundedCornerShape(7.dp))
                                                .padding(horizontal = 2.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedWidth.coerceIn(0.04f, 1f))
                                                    .height(10.dp)
                                                    .background(platformColor, shape = RoundedCornerShape(5.dp))
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (opens > 0) "${mins}m (${opens}x)" else "${mins}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandColors.SoftWhite,
                                            modifier = Modifier.width(64.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // 30-DAY HEATMAP GRID MAP
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "30-Day Activity Heatmap Grid:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )

                            val dates30 = remember(usageList, platform, today) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val calendar = Calendar.getInstance()
                                val parsedToday = try {
                                    sdf.parse(today) ?: Date()
                                } catch (e: Exception) {
                                    Date()
                                }
                                val list = mutableListOf<String>()
                                for (i in 0..29) {
                                    calendar.time = parsedToday
                                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                                    list.add(sdf.format(calendar.time))
                                }
                                list.reversed()
                            }

                            Text(
                                "Daily timeline footprint. Dark squares signify heavy screen active foreground periods.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            FlowRow(
                                maxItemsInEachRow = 6,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
                            ) {
                                dates30.forEach { dateKey ->
                                    val dayNum = dateKey.substringAfterLast("-")
                                    val usageForDate = usageList.firstOrNull { it.date == dateKey && it.platform.equals(platform, ignoreCase = true) }
                                    val mins = (usageForDate?.usageSeconds ?: 0L) / 60
                                    
                                    val levelOpacity = when {
                                        mins == 0L -> 0.06f
                                        mins < 10 -> 0.25f
                                        mins < 30 -> 0.50f
                                        mins < 60 -> 0.75f
                                        else -> 1f
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                color = platformColor.copy(alpha = levelOpacity),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (levelOpacity > 0.6f) Color.White else BrandColors.SoftWhite
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Less", fontSize = 10.sp, color = BrandColors.SlateGray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(10.dp).background(platformColor.copy(alpha = 0.06f), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(modifier = Modifier.size(10.dp).background(platformColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(modifier = Modifier.size(10.dp).background(platformColor, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("More", fontSize = 10.sp, color = BrandColors.SlateGray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = platformColor)
            ) {
                Text("Dismiss stats")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun DayPartProgressRow(
    label: String,
    minutes: Int,
    ratio: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = BrandColors.SlateGray)
            val formattedMinutes = if (minutes >= 60) {
                val h = minutes / 60
                val m = minutes % 60
                if (m > 0) "${h}h ${m}m" else "${h}h"
            } else {
                "${minutes}m"
            }
            Text("$formattedMinutes spent", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandColors.SoftWhite)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(BrandColors.SlateGray.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (minutes > 0) ratio else 0f)
                    .height(8.dp)
                    .background(barColor, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

// Simple Row helper
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    label: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor.copy(alpha = 0.2f) else Color.Transparent,
        contentColor = if (selected) selectedColor else unselectedColor,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            label()
        }
    }
}

@Composable
fun DaylightEfficiencyDetailsDialog(
    dayStartMinutes: Int?,
    dayStartStr: String,
    daytimeSoFarMinutes: Float,
    todaysTotalMinutes: Long,
    daylightSecondsSpent: Long,
    todaysUsages: List<DailyUsageEntity>,
    allSessions: List<com.example.viewmodel.UsageSession>,
    onDismiss: () -> Unit
) {
    val totalElapsedSeconds = if (dayStartMinutes != null) (daytimeSoFarMinutes * 60f).coerceAtLeast(60f) else 1f
    val totalSecondsSpent = remember(todaysUsages) { todaysUsages.sumOf { it.usageSeconds }.toFloat() }
    val overallRatio = if (dayStartMinutes != null) (daylightSecondsSpent.toFloat() / totalElapsedSeconds).coerceIn(0f, 1f) else 0f
    val overallPercent = (overallRatio * 100).toInt()

    // Calculate Active Day Hours Elapsed directly from real daytime elapsed:
    val elapsedMinutes = if (dayStartMinutes != null) daytimeSoFarMinutes.toInt() else 0
    val elapsedHoursStr = if (dayStartMinutes != null) {
        if (elapsedMinutes >= 60) {
            val h = elapsedMinutes / 60
            val m = elapsedMinutes % 60
            if (m > 0) "${h}h ${m}m" else "${h}h"
        } else {
            "${elapsedMinutes}m"
        }
    } else {
        "0m"
    }

    var sortNewestFirst by remember { mutableStateOf(false) }
    val displaySessions = remember(allSessions, sortNewestFirst) {
        if (sortNewestFirst) {
            allSessions.sortedWith(compareByDescending<com.example.viewmodel.UsageSession> { it.startTime }.thenByDescending { it.endTime })
        } else {
            allSessions.sortedWith(compareBy<com.example.viewmodel.UsageSession> { it.startTime }.thenBy { it.endTime })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAYLIGHT EFFICIENCY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.SoftWhite
                        )
                        Text(
                            text = "Sun Hours Allocation Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandColors.SlateGray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BrandColors.SlateGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable details content container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Stats Card Block
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Day Start Time:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandColors.SlateGray
                                    )
                                    Text(
                                        text = dayStartStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SecondaryTeal
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Daylight Elapsed:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandColors.SlateGray
                                    )
                                    Text(
                                        text = elapsedHoursStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Daylight Distraction:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandColors.SlateGray
                                    )
                                    val daylightMins = daylightSecondsSpent / 60
                                    val daylightSecsRem = daylightSecondsSpent % 60
                                    val formattedDistraction = if (daylightMins >= 60) {
                                        val h = daylightMins / 60
                                        val m = daylightMins % 60
                                        if (m > 0) "${h}h ${m}m" else "${h}h"
                                    } else {
                                        if (daylightMins > 0 || daylightSecsRem > 0) "${daylightMins}m ${daylightSecsRem}s" else "0m"
                                    }
                                    Text(
                                        text = "$formattedDistraction spent",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.ErrorRose
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BrandColors.SlateGray.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Daylight Allocation Ratio: $overallPercent%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(BrandColors.SlateGray.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(overallRatio)
                                            .height(8.dp)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(BrandColors.PrimaryIndigo, BrandColors.ErrorRose)
                                                ),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        overallPercent < 15 -> "Optimal Sun Allocation! Your screen leaks are currently under control, maximizing the active hours of your day."
                                        overallPercent < 35 -> "Moderate Daylight Leak. Social distractions are taking up a sizeable portion of daylight activities. Try focusing blocks."
                                        else -> "Extreme Active Drain! You are losing a severe percentage of daylight hours to screen distractions. Immediate custom limits are advised."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandColors.SlateGray,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // App Contributions Breakdown Section
                        Column {
                            Text(
                                text = "APP CONTRIBUTION TO RATIO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val nonZeroUsages = todaysUsages.filter { it.usageSeconds > 0 }
                            if (nonZeroUsages.isEmpty()) {
                                Text(
                                    text = "No app usages registered today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandColors.SlateGray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        nonZeroUsages.sortedByDescending { it.usageSeconds }.forEachIndexed { index, usage ->
                                            val appMinutes = usage.usageSeconds / 60
                                            val appSeconds = usage.usageSeconds % 60
                                            val appRatioPercent = if (totalElapsedSeconds > 0f) {
                                                 val ratio = (usage.usageSeconds.toFloat() / totalElapsedSeconds) * 100f
                                                 if (ratio < 1f && ratio > 0f) {
                                                     String.format(Locale.getDefault(), "%.2f", ratio)
                                                 } else {
                                                     String.format(Locale.getDefault(), "%.1f", ratio)
                                                 }
                                             } else "0.0"
                                            val appDistractionPercent = if (totalSecondsSpent > 0f) {
                                                val ratio = (usage.usageSeconds.toFloat() / totalSecondsSpent) * 100f
                                                String.format(Locale.getDefault(), "%.1f", ratio)
                                            } else "0.0"
                                            val appColor = BrandColors.getPlatformColor(usage.platform)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                PlatformAppIcon(
                                                    platformName = usage.platform,
                                                    size = 32.dp
                                                )

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = usage.platform,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandColors.SoftWhite
                                                    )
                                                    Text(
                                                        text = "$appRatioPercent% of Daylight ($appDistractionPercent% of distraction)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = BrandColors.SlateGray
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    val formattedAppTime = if (appMinutes >= 60) {
                                                        val h = appMinutes / 60
                                                        val m = appMinutes % 60
                                                        if (m > 0) "${h}h ${m}m ${appSeconds}s" else "${h}h ${appSeconds}s"
                                                    } else {
                                                        "${appMinutes}m ${appSeconds}s"
                                                    }
                                                    Text(
                                                        text = formattedAppTime,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandColors.SoftWhite
                                                    )
                                                    Text(
                                                        text = "${usage.openCount} opens",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = BrandColors.SlateGray
                                                    )
                                                }
                                            }

                                            if (index < nonZeroUsages.size - 1) {
                                                HorizontalDivider(color = BrandColors.SlateGray.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Social Sessions Chronological Timeline Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "RECENT ACTIVITY TIMELINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.PrimaryIndigo
                                    )
                                    Text(
                                        text = "${displaySessions.size} entries • ${todaysTotalMinutes}m matched stats",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandColors.SlateGray,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = !sortNewestFirst,
                                        onClick = { sortNewestFirst = false },
                                        selectedColor = BrandColors.PrimaryIndigo,
                                        unselectedColor = BrandColors.SlateGray
                                    ) {
                                        Text("Earliest First", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    FilterChip(
                                        selected = sortNewestFirst,
                                        onClick = { sortNewestFirst = true },
                                        selectedColor = BrandColors.PrimaryIndigo,
                                        unselectedColor = BrandColors.SlateGray
                                    ) {
                                        Text("Recent First", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            if (displaySessions.isEmpty()) {
                                Text(
                                    text = "No session logs recorded today yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandColors.SlateGray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp)
                                ) {
                                    displaySessions.forEachIndexed { index, session ->
                                        val startFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.startTime))
                                        val endFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.endTime))
                                        val elapsedSecs = (session.endTime - session.startTime) / 1000
                                        val elapsedMins = elapsedSecs / 60
                                        val elapsedSecsRem = elapsedSecs % 60
                                        
                                        val appColor = BrandColors.getPlatformColor(session.platform)

                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Dynamic chronological timeline line and bullet
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(32.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(appColor, shape = CircleShape)
                                                        .padding(2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(BrandColors.TonalSurface, shape = CircleShape)
                                                    )
                                                }

                                                if (index < displaySessions.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .weight(1f, fill = false)
                                                            .height(48.dp)
                                                            .background(BrandColors.SlateGray.copy(alpha = 0.3f))
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Session info card
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(bottom = 12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .background(appColor, shape = CircleShape)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = session.platform,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BrandColors.SoftWhite
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "$startFormat - $endFormat",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = BrandColors.SlateGray
                                                        )
                                                    }

                                                    val formattedTimelineTime = if (elapsedMins >= 60) {
                                                        val h = elapsedMins / 60
                                                        val m = elapsedMins % 60
                                                        if (m > 0) "${h}h ${m}m ${elapsedSecsRem}s" else "${h}h ${elapsedSecsRem}s"
                                                    } else {
                                                        "${elapsedMins}m ${elapsedSecsRem}s"
                                                    }
                                                    Text(
                                                        text = formattedTimelineTime,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandColors.SecondaryTeal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold, color = BrandColors.PrimaryIndigo)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
