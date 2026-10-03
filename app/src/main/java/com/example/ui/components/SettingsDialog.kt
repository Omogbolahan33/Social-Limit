package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ScreentimeViewModel

@Composable
fun SettingsDialog(
    viewModel: ScreentimeViewModel,
    onDismiss: () -> Unit
) {
    val tickerEnabled by viewModel.tickerNotificationEnabled.collectAsState()
    val systemEnabled by viewModel.systemNotificationEnabled.collectAsState()
    val efficiencyLockEnabled by viewModel.efficiencyLockEnabled.collectAsState()
    val efficiencyLockThreshold by viewModel.efficiencyLockThreshold.collectAsState()
    val efficiencyLockedPlatforms by viewModel.efficiencyLockedPlatforms.collectAsState()
    val throttleEnabled by viewModel.throttleEnabled.collectAsState()
    val maxContinuousMinutes by viewModel.throttleMaxContinuousMinutes.collectAsState()
    val cooloffMinutes by viewModel.throttleCooloffMinutes.collectAsState()

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BrandColors.PrimaryIndigo.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings Icon",
                                tint = BrandColors.PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PREFERENCES",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                            Text(
                                text = "App & Notification Control",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray
                            )
                        }
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

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title section
                    Text(
                        text = "NOTIFICATION SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.PrimaryIndigo
                    )

                    // 1. Live ticker count-down widget settings
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(BrandColors.SecondaryTeal.copy(alpha = 0.15f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = BrandColors.SecondaryTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Countdown Widget",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                }
                                Switch(
                                    checked = tickerEnabled,
                                    onCheckedChange = { viewModel.setTickerNotificationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandColors.PrimaryIndigo,
                                        uncheckedThumbColor = BrandColors.SlateGray,
                                        uncheckedTrackColor = BrandColors.TonalSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Triggers a live, persistent, second-by-second countdown widget in your notification drawer when nested within 10 minutes of active platform limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // 2. Daily Limit Alert warnings
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(BrandColors.ErrorRose.copy(alpha = 0.15f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationImportant,
                                            contentDescription = null,
                                            tint = BrandColors.ErrorRose,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Limit Exceed Alerts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                }
                                Switch(
                                    checked = systemEnabled,
                                    onCheckedChange = { viewModel.setSystemNotificationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandColors.PrimaryIndigo,
                                        uncheckedThumbColor = BrandColors.SlateGray,
                                        uncheckedTrackColor = BrandColors.TonalSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Delivers high-priority system notifications immediately upon exceeding configured platform screen time goals today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // 3. Daylight Efficiency Lockout (Cool-off Mode)
                    Text(
                        text = "DAYLIGHT EFFICIENCY COOL-OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.PrimaryIndigo,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(BrandColors.PrimaryIndigo.copy(alpha = 0.15f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = BrandColors.PrimaryIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Daylight Lockout",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                }
                                Switch(
                                    checked = efficiencyLockEnabled,
                                    onCheckedChange = { viewModel.setEfficiencyLockEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandColors.PrimaryIndigo,
                                        uncheckedThumbColor = BrandColors.SlateGray,
                                        uncheckedTrackColor = BrandColors.TonalSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Instantly lock specified high-drain social media apps when your overall Daylight Screen Efficiency falls below your configured threshold.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                lineHeight = 15.sp
                            )

                            if (efficiencyLockEnabled) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = BrandColors.SlateGray.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Threshold Slider Section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Minimum Efficiency Threshold",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                    Text(
                                        text = "$efficiencyLockThreshold%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.PrimaryIndigo
                                    )
                                }
                                Slider(
                                    value = efficiencyLockThreshold.toFloat(),
                                    onValueChange = { viewModel.setEfficiencyLockThreshold(it.toInt()) },
                                    valueRange = 10f..95f,
                                    steps = 16,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BrandColors.PrimaryIndigo,
                                        activeTrackColor = BrandColors.PrimaryIndigo,
                                        inactiveTrackColor = BrandColors.DarkSlateBg
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Choose social apps to lock:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Multi-select lists
                                val defaultPlatforms = listOf("Instagram", "TikTok", "YouTube", "Twitter/X", "Facebook", "LinkedIn", "WhatsApp")
                                val allTracked = defaultPlatforms.toMutableList()
                                for (custom in viewModel.getCustomMonitoredPlatforms()) {
                                    if (!allTracked.contains(custom)) {
                                        allTracked.add(custom)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    allTracked.forEach { platform ->
                                        val isChecked = efficiencyLockedPlatforms.contains(platform)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val updatedList = if (isChecked) {
                                                        efficiencyLockedPlatforms - platform
                                                    } else {
                                                        efficiencyLockedPlatforms + platform
                                                    }
                                                    viewModel.setEfficiencyLockedPlatforms(updatedList)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = {
                                                    val updatedList = if (isChecked) {
                                                        efficiencyLockedPlatforms - platform
                                                    } else {
                                                        efficiencyLockedPlatforms + platform
                                                    }
                                                    viewModel.setEfficiencyLockedPlatforms(updatedList)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = BrandColors.PrimaryIndigo,
                                                    uncheckedColor = BrandColors.SlateGray,
                                                    checkmarkColor = Color.White
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = platform,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isChecked) BrandColors.SoftWhite else BrandColors.SlateGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // App Usage Throttling Card (Cut-Off & Cool-Down)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandColors.SolarAmber.copy(alpha = 0.06f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SolarAmber.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(BrandColors.SolarAmber.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassBottom,
                                            contentDescription = null,
                                            tint = BrandColors.SolarAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Usage Throttling",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                }
                                Switch(
                                    checked = throttleEnabled,
                                    onCheckedChange = { viewModel.setThrottleEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandColors.SolarAmber,
                                        uncheckedThumbColor = BrandColors.SlateGray,
                                        uncheckedTrackColor = BrandColors.TonalSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Like being cut off at a beer parlor, using monitored apps continuously for an extended stretch locks access until you complete a mandatory cool-off period.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                lineHeight = 15.sp
                            )

                            if (throttleEnabled) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = BrandColors.SolarAmber.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Continuous stretch limit
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Duration Before Cut-Off",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                    Text(
                                        text = "$maxContinuousMinutes min",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SolarAmber
                                    )
                                }
                                Slider(
                                    value = maxContinuousMinutes.toFloat(),
                                    onValueChange = { viewModel.setThrottleMaxContinuousMinutes(it.toInt()) },
                                    valueRange = 5f..90f,
                                    steps = 16,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BrandColors.SolarAmber,
                                        activeTrackColor = BrandColors.SolarAmber,
                                        inactiveTrackColor = BrandColors.DarkSlateBg
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Cool-off period
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mandatory Cool-Off Period",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                    Text(
                                        text = "$cooloffMinutes min",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.PrimaryIndigo
                                    )
                                }
                                Slider(
                                    value = cooloffMinutes.toFloat(),
                                    onValueChange = { viewModel.setThrottleCooloffMinutes(it.toInt()) },
                                    valueRange = 2f..45f,
                                    steps = 14,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BrandColors.PrimaryIndigo,
                                        activeTrackColor = BrandColors.PrimaryIndigo,
                                        inactiveTrackColor = BrandColors.DarkSlateBg
                                    )
                                )
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
