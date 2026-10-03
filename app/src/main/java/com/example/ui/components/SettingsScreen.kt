package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ScreentimeViewModel

@Composable
fun SettingsScreen(
    viewModel: ScreentimeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tickerEnabled by viewModel.tickerNotificationEnabled.collectAsState()
    val systemEnabled by viewModel.systemNotificationEnabled.collectAsState()
    val efficiencyLockEnabled by viewModel.efficiencyLockEnabled.collectAsState()
    val efficiencyLockThreshold by viewModel.efficiencyLockThreshold.collectAsState()
    val efficiencyLockedPlatforms by viewModel.efficiencyLockedPlatforms.collectAsState()
    val userName by viewModel.userName.collectAsState()
    var hasPermission by remember { mutableStateOf(viewModel.hasUsageStatsPermission()) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandColors.DarkSlateBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Screen Section Title
        Text(
            text = "SETTINGS & PREFERENCES",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = BrandColors.SoftWhite,
            letterSpacing = 1.sp
        )

        // 0. User Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(BrandColors.PrimaryIndigo.copy(alpha = 0.12f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userName.isNotBlank()) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = BrandColors.PrimaryIndigo
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BrandColors.PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (userName.isNotBlank()) userName else "Friend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                            Text(
                                text = "Display Name",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            editNameInput = userName
                            showEditNameDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = BrandColors.PrimaryIndigo)
                    }
                }

                HorizontalDivider(color = BrandColors.SlateGray.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "First-Time Onboarding",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.SoftWhite
                        )
                        Text(
                            text = "Reconfigure monitored apps, goals, and setup",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandColors.SlateGray
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetOnboarding() },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.PrimaryIndigo)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandColors.PrimaryIndigo)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replay Tour", fontSize = 12.sp, color = BrandColors.PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 1. Permission Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (hasPermission) BrandColors.ForestGreen.copy(alpha = 0.15f)
                                else BrandColors.ErrorRose.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) BrandColors.ForestGreen else BrandColors.ErrorRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hasPermission) "Usage Access Granted" else "Permission Required",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.SoftWhite
                        )
                        Text(
                            text = if (hasPermission) "Real-time phone metrics active" else "Needed to read screen time",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandColors.SlateGray
                        )
                    }
                }

                if (!hasPermission) {
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandColors.ErrorRose),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Notification Settings
        Text(
            text = "NOTIFICATION SETTINGS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = BrandColors.PrimaryIndigo
        )

        // Live Countdown Widget
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                                .size(36.dp)
                                .background(BrandColors.SecondaryTeal.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BrandColors.SecondaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
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
                    text = "Triggers a live countdown widget in your notification drawer when within 10 minutes of active platform limits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.SlateGray,
                    lineHeight = 16.sp
                )
            }
        }

        // Limit Exceed Alert
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                                .size(36.dp)
                                .background(BrandColors.ErrorRose.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationImportant,
                                contentDescription = null,
                                tint = BrandColors.ErrorRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
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
                    text = "Delivers high-priority system notifications immediately upon exceeding configured platform screen time goals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.SlateGray,
                    lineHeight = 16.sp
                )
            }
        }

        // 4. Daylight Efficiency Lockout (Cool-off Mode)
        Text(
            text = "DAYLIGHT EFFICIENCY COOL-OFF",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = BrandColors.PrimaryIndigo
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                                .size(36.dp)
                                .background(BrandColors.PrimaryIndigo.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = BrandColors.PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
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
                    text = "Lock specified social media apps when your overall Daylight Screen Efficiency falls below your configured threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.SlateGray,
                    lineHeight = 16.sp
                )

                if (efficiencyLockEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BrandColors.SlateGray.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(12.dp))

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
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val updatedList = if (isChecked) {
                                            efficiencyLockedPlatforms - platform
                                        } else {
                                            efficiencyLockedPlatforms + platform
                                        }
                                        viewModel.setEfficiencyLockedPlatforms(updatedList)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
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

        // 5. Danger Zone / Reset
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.ErrorRose.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DANGER ZONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.ErrorRose
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reset all tracked usage history, custom schedules, and user limit configurations back to defaults.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.SlateGray,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.ErrorRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandColors.ErrorRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("Are you sure you want to clear all tracked screentime records and reset goals to defaults? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCacheAndRefresh()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "All screentime data cleared.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset", color = BrandColors.ErrorRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = BrandColors.SlateGray)
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Update Your Name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter how you'd like ScreenTime to greet you:", style = MaterialTheme.typography.bodyMedium, color = BrandColors.SlateGray)
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        placeholder = { Text("e.g. Alex") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(editNameInput)
                        showEditNameDialog = false
                        Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = BrandColors.SlateGray)
                }
            }
        )
    }
}
