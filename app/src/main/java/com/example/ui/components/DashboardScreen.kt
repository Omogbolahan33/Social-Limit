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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyUsageEntity
import com.example.data.PlatformGoalEntity
import com.example.viewmodel.ScreentimeViewModel
import android.content.Intent
import android.provider.Settings
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasUsageStatsPermission by remember { mutableStateOf(false) }
    var selectedPlatformForDetails by remember { mutableStateOf<String?>(null) }
    var selectedChartTab by remember { mutableStateOf(0) }
    var showDaylightRatioDetails by remember { mutableStateOf(false) }

    // Determine current Today Date String
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Today's platform break down helper
    val todaysUsages = remember(usageList, todayStr) {
        usageList.filter { it.date == todayStr }
    }

    val allSessions by viewModel.allPlatformsTodaySessions.collectAsState()

    val dayStartMinutes = remember(allSessions) {
        val earliestSession = allSessions.minByOrNull { it.startTime }
        if (earliestSession != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = earliestSession.startTime }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } else {
            8 * 60 // Fallback to 8:00 AM if no sessions are recorded today yet
        }
    }

    val dayStartStr = remember(dayStartMinutes) {
        val h = dayStartMinutes / 60
        val m = dayStartMinutes % 60
        val ampm = if (h >= 12) "PM" else "AM"
        val displayHour = if (h % 12 == 0) 12 else h % 12
        String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, ampm)
    }

    val daytimeSoFarMinutes = remember(dayStartMinutes) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val curMins = hour * 60 + minute
        when {
            curMins < dayStartMinutes -> 1f
            else -> (curMins - dayStartMinutes).toFloat().coerceAtLeast(1f)
        }
    }

    LaunchedEffect(todaysUsages) {
        if (todaysUsages.isNotEmpty()) {
            viewModel.loadAllPlatformsTodaySessions(todaysUsages.map { it.platform })
        }
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

    // Dynamic calculations for circular indicator
    val todaysTotalMinutes = remember(todaysUsages) {
        todaysUsages.sumOf { it.usageSeconds } / 60
    }

    val todaysGoalTotal = remember(goalList) {
        val active = goalList.filter { it.isEnabled }
        if (active.isEmpty()) 150L else active.sumOf { it.dailyLimitMinutes }
    }

    val overallPercentage = remember(todaysTotalMinutes, todaysGoalTotal) {
        if (todaysGoalTotal > 0) (todaysTotalMinutes.toFloat() / todaysGoalTotal.toFloat()) else 0f
    }

    // Weekly trend processor (Last 7 days calculation)
    val weeklyDayTimePairs = remember(usageList) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val list = mutableListOf<Pair<String, Long>>()

        for (dayAgo in (0..6).reversed()) {
            calendar.time = Date()
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = premiumCardElevation(),
                    border = premiumCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp) // Premium spacing 20pt
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Active Analytics Tracking",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REAL-TIME TRACKING INACTIVE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "To track daily active minutes on other apps (Instagram, TikTok, YouTube etc.) on this phone automatically, please grant Usage Access.",
                            style = MaterialTheme.typography.bodyLarge, // 16-17 pt
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (e: Exception) {
                                    // Fallback if settings page fails to launch
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp) // Premium height 50pt
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Usage Permission", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = Color.White))
                        }
                    }
                }
            }
        }

        // Main Dashboard Gauge Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = premiumCardElevation(),
                border = premiumCardBorder()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "TODAY'S USAGE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SecondaryTeal
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val todaysTotalMinutesStr = if (todaysTotalMinutes >= 60) {
                                    val h = todaysTotalMinutes / 60
                                    val m = todaysTotalMinutes % 60
                                    if (m > 0) "${h}h ${m}m" else "${h}h"
                                } else {
                                    "${todaysTotalMinutes}m"
                                }
                                Text(
                                    text = todaysTotalMinutesStr,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = BrandColors.SoftWhite
                                )
                                Text(
                                    text = "Total social media time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandColors.SlateGray
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                val radialTotalMinutesStr = if (todaysTotalMinutes >= 60) {
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
                                ScreentimeRadialChart(
                                    percentage = overallPercentage,
                                    totalMinutesStr = radialTotalMinutesStr,
                                    goalMinutesStr = radialGoalMinutesStr
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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

        // Daylight Screentime Efficiency & Ratio Report Card
        item {
            val maxUsagePlatform = remember(todaysUsages) {
                if (todaysUsages.isNotEmpty()) todaysUsages.maxByOrNull { it.usageSeconds } else null
            }
            val combinedRatio = (todaysTotalMinutes.toFloat() / daytimeSoFarMinutes).coerceIn(0f, 1f)
            val combinedPercent = (combinedRatio * 100).toInt()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = premiumCardElevation(),
                border = premiumCardBorder(),
                modifier = Modifier.clickable { showDaylightRatioDetails = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Daylight Screen efficiency Report",
                                tint = BrandColors.PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "DAYLIGHT SCREEN EFFICIENCY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.PrimaryIndigo
                            )
                        }
                        
                        // Rating indicator
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when {
                                        combinedPercent < 15 -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                        combinedPercent < 35 -> Color(0xFFEF6C00).copy(alpha = 0.12f)
                                        else -> BrandColors.ErrorRose.copy(alpha = 0.12f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    combinedPercent < 15 -> "Optimal"
                                    combinedPercent < 35 -> "Moderate Leak"
                                    else -> "Extreme Drain"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    combinedPercent < 15 -> Color(0xFF2E7D32)
                                    combinedPercent < 35 -> Color(0xFFEF6C00)
                                    else -> BrandColors.ErrorRose
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Active Day Started: $dayStartStr (from first daily session)",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandColors.SecondaryTeal,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Daylight Allocation Ratio: $combinedPercent% Spent",
                        style = MaterialTheme.typography.titleMedium,
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
                                .fillMaxWidth(combinedRatio)
                                .height(8.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(BrandColors.PrimaryIndigo, BrandColors.ErrorRose)
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Personalized warning based on TikTok / main time leak
                    if (maxUsagePlatform != null && maxUsagePlatform.usageSeconds > 0) {
                        val singlePlatformMins = maxUsagePlatform.usageSeconds / 60
                        val singleRatioPercent = ((singlePlatformMins.toFloat() / daytimeSoFarMinutes) * 100).toInt()
                        
                        Row(
                            modifier = Modifier
                                .background(BrandColors.ErrorRose.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Daylight Drain Alert",
                                tint = BrandColors.ErrorRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Attention: You've spent $singleRatioPercent% of your active daylight today seeing ${maxUsagePlatform.platform}!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.ErrorRose
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF2E7D32).copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Sun hours Secured",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perfect daytime balance! You've spent 0% of daylight sun hours on social distractions today.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BrandColors.SlateGray.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open details",
                            tint = BrandColors.PrimaryIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tap to view breakdown and detailed timeline",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandColors.PrimaryIndigo,
                            fontWeight = FontWeight.Bold
                        )
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(platformColor.copy(alpha = 0.15f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = BrandColors.getPlatformIcon(usage.platform),
                            contentDescription = usage.platform,
                            tint = platformColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

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
            todaysUsages = todaysUsages,
            allSessions = allSessions,
            onDismiss = { showDaylightRatioDetails = false }
        )
    }
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
    
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Retrieve today's usage seconds
    val todayUsageSecs = remember(usageList, platform) {
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(platformColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = BrandColors.getPlatformIcon(platform),
                            contentDescription = platform,
                            tint = platformColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = platform.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = BrandColors.SoftWhite
                        )
                        val todayOpens = remember(usageList, platform) {
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

                            val calendar = Calendar.getInstance()
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val daySdf = SimpleDateFormat("EEE", Locale.getDefault())
                            
                            val weekDays = remember(usageList, platform) {
                                val list = mutableListOf<Triple<String, Long, Int>>()
                                for (dayAgo in (0..6).reversed()) {
                                    calendar.time = Date()
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

                            val calendar = Calendar.getInstance()
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            
                            val dates30 = remember(usageList, platform) {
                                val list = mutableListOf<String>()
                                for (i in 0..29) {
                                    calendar.time = Date()
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
    dayStartMinutes: Int,
    dayStartStr: String,
    daytimeSoFarMinutes: Float,
    todaysTotalMinutes: Long,
    todaysUsages: List<DailyUsageEntity>,
    allSessions: List<com.example.viewmodel.UsageSession>,
    onDismiss: () -> Unit
) {
    val overallRatio = (todaysTotalMinutes.toFloat() / daytimeSoFarMinutes).coerceIn(0f, 1f)
    val overallPercent = (overallRatio * 100).toInt()

    // Calculate Active Day Hours Elapsed:
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val currentAbsoluteMinutes = hour * 60 + minute
    
    val elapsedMinutes = (currentAbsoluteMinutes - dayStartMinutes).coerceAtLeast(1)
    val elapsedHoursStr = if (elapsedMinutes >= 60) {
        val h = elapsedMinutes / 60
        val m = elapsedMinutes % 60
        if (m > 0) "${h}h ${m}m" else "${h}h"
    } else {
        "${elapsedMinutes}m"
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
                                    val formattedDistraction = if (todaysTotalMinutes >= 60) {
                                        val h = todaysTotalMinutes / 60
                                        val m = todaysTotalMinutes % 60
                                        if (m > 0) "${h}h ${m}m" else "${h}h"
                                    } else {
                                        "${todaysTotalMinutes}m"
                                    }
                                    Text(
                                        text = "$formattedDistraction spent",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.ErrorRose
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = BrandColors.SlateGray.copy(alpha = 0.15f))
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

                            if (todaysUsages.isEmpty()) {
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
                                        todaysUsages.sortedByDescending { it.usageSeconds }.forEachIndexed { index, usage ->
                                            val appMinutes = usage.usageSeconds / 60
                                            val appSeconds = usage.usageSeconds % 60
                                            val appRatioPercent = ((appMinutes.toFloat() / daytimeSoFarMinutes) * 100).toInt()
                                            val appColor = BrandColors.getPlatformColor(usage.platform)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(appColor.copy(alpha = 0.15f), shape = CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = BrandColors.getPlatformIcon(usage.platform),
                                                        contentDescription = usage.platform,
                                                        tint = appColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = usage.platform,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandColors.SoftWhite
                                                    )
                                                    Text(
                                                        text = "$appRatioPercent% of Daylight so far",
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

                                            if (index < todaysUsages.size - 1) {
                                                Divider(color = BrandColors.SlateGray.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Social Sessions Chronological Timeline Section
                        Column {
                            Text(
                                text = "CHRONOLOGICAL USAGE TIMELINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (allSessions.isEmpty()) {
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
                                    allSessions.forEachIndexed { index, session ->
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

                                                if (index < allSessions.size - 1) {
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
