package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlatformGoalEntity
import com.example.viewmodel.ScreentimeViewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import java.util.Locale

@Composable
fun GoalsScreen(
    viewModel: ScreentimeViewModel,
    modifier: Modifier = Modifier
) {
    val goalList by viewModel.allGoals.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showManageAppsDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (goalList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = "No goals",
                    tint = BrandColors.SlateGray,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No custom limits configured",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.SoftWhite
                )
                Text(
                    text = "Set daily limits to receive warning alerts when you spend too much time on distracting apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.SlateGray,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showManageAppsDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                            contentColor = BrandColors.PrimaryIndigo
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select Apps")
                    }
                    Button(
                        onClick = { showAddGoalDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                    ) {
                        Text("Add Daily Limit")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your platform limits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                            Text(
                                text = "Exceeding these limits triggers immediate alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                            )
                        }

                        Button(
                            onClick = { showManageAppsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                                contentColor = BrandColors.PrimaryIndigo
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select Apps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(goalList) { goal ->
                    val color = BrandColors.getPlatformColor(goal.platform)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = premiumCardElevation(),
                        border = premiumCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color.copy(alpha = 0.15f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = BrandColors.getPlatformIcon(goal.platform),
                                    contentDescription = goal.platform,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.platform,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite
                                )
                                val h = goal.dailyLimitMinutes / 60
                                val m = goal.dailyLimitMinutes % 60
                                val limitStr = if (goal.dailyLimitMinutes >= 60) {
                                    String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                } else {
                                    "${goal.dailyLimitMinutes}m"
                                }
                                Text(
                                    text = "Daily limit: $limitStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (goal.isEnabled) BrandColors.SecondaryTeal else BrandColors.SlateGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Switch enable/disable goal
                            Switch(
                                checked = goal.isEnabled,
                                onCheckedChange = { viewModel.toggleGoalEnabled(goal, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BrandColors.SoftWhite,
                                    checkedTrackColor = BrandColors.PrimaryIndigo,
                                    uncheckedThumbColor = BrandColors.SlateGray,
                                    uncheckedTrackColor = BrandColors.SlateGray.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Delete Goal icon
                            IconButton(onClick = { viewModel.deleteGoal(goal) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = BrandColors.ErrorRose.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating action button at bottom right
        FloatingActionButton(
            onClick = { showAddGoalDialog = true },
            containerColor = BrandColors.PrimaryIndigo,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Goal")
        }
    }

    if (showAddGoalDialog) {
        val defaultPlatforms = listOf("Instagram", "TikTok", "YouTube", "Twitter/X", "Facebook", "LinkedIn", "WhatsApp")
        val customMonitoredApps by viewModel.customMonitoredApps.collectAsState()
        val platformChoices = (defaultPlatforms + customMonitoredApps.values).distinct()

        var dropdownExpanded by remember { mutableStateOf(false) }
        var selectedPlatform by remember { mutableStateOf(platformChoices.firstOrNull() ?: "Instagram") }

        var selectedHour by remember { mutableStateOf(1) }
        var selectedMinute by remember { mutableStateOf(30) }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Add Social Limit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select which platform you wish to restrict and choose a maximum daily boundary.")

                    Column {
                        Text("Platform:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandColors.SoftWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Button(
                                    onClick = { dropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.SlateGray.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(selectedPlatform, color = BrandColors.SoftWhite, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "More", tint = BrandColors.SoftWhite)
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.7f).background(BrandColors.TonalSurface)
                                ) {
                                    platformChoices.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = BrandColors.SoftWhite) },
                                            onClick = {
                                                selectedPlatform = option
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Duration (Hours & Minutes):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandColors.SoftWhite)
                        Spacer(modifier = Modifier.height(4.dp))
                        TimeRollingPicker(
                            selectedHour = selectedHour,
                            selectedMinute = selectedMinute,
                            onTimeChanged = { h, m ->
                                selectedHour = h
                                selectedMinute = m
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val totalMins = (selectedHour * 60L) + selectedMinute
                        viewModel.saveGoal(selectedPlatform, totalMins.coerceAtLeast(1L))
                        showAddGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                ) {
                    Text("Enable Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManageAppsDialog) {
        val installedApps by viewModel.installedApps.collectAsState()
        val customMonitoredApps by viewModel.customMonitoredApps.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        
        val filteredApps = remember(installedApps, searchQuery) {
            installedApps.filter { app ->
                app.second.contains(searchQuery, ignoreCase = true) ||
                app.first.contains(searchQuery, ignoreCase = true)
            }
        }
        
        AlertDialog(
            onDismissRequest = { showManageAppsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Apps to Monitor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandColors.SoftWhite)
                    IconButton(onClick = { showManageAppsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = BrandColors.SlateGray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Toggle on any launchable app on this device to include it in screen time tracking, schedule filters, and warning limit triggers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandColors.SlateGray
                    )
                    
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandColors.PrimaryIndigo,
                            unfocusedBorderColor = BrandColors.SlateGray.copy(alpha = 0.4f),
                            focusedContainerColor = BrandColors.TonalSurface,
                            unfocusedContainerColor = BrandColors.TonalSurface,
                            focusedTextColor = BrandColors.SoftWhite,
                            unfocusedTextColor = BrandColors.SoftWhite
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No apps found", color = BrandColors.SlateGray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApps) { app ->
                                val (packageName, appName) = app
                                // default apps are monitored by default and shouldn't need separate toggle list, or maybe we show them as enabled/locked?
                                val defaultApps = listOf(
                                    "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.aweme",
                                    "com.google.android.youtube", "com.twitter.android", "com.facebook.katana",
                                    "com.linkedin.android", "com.whatsapp", "com.whatsapp.w4b"
                                )
                                val isDefaultApp = defaultApps.contains(packageName)
                                val isMonitored = isDefaultApp || customMonitoredApps.containsKey(packageName)
                                
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = BrandColors.getPlatformColor(appName).copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = BrandColors.getPlatformIcon(appName),
                                                contentDescription = appName,
                                                tint = BrandColors.getPlatformColor(appName),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandColors.SoftWhite
                                            )
                                            Text(
                                                text = packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = BrandColors.SlateGray,
                                                maxLines = 1
                                            )
                                        }
                                        
                                        Switch(
                                            checked = isMonitored,
                                            onCheckedChange = { checked ->
                                                if (!isDefaultApp) {
                                                    viewModel.toggleAppMonitored(packageName, appName, checked)
                                                }
                                            },
                                            enabled = !isDefaultApp,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = BrandColors.SoftWhite,
                                                checkedTrackColor = BrandColors.PrimaryIndigo,
                                                uncheckedThumbColor = BrandColors.SlateGray,
                                                uncheckedTrackColor = BrandColors.SlateGray.copy(alpha = 0.2f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageAppsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun TimeRollingPicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(BrandColors.TonalSurface, shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hours column
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Hours", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            // Start around the middle of infinite range so that user can scroll up & down
            val baseIndex = 240
            val hourState = rememberLazyListState(initialFirstVisibleItemIndex = baseIndex + selectedHour)
            
            LaunchedEffect(hourState.firstVisibleItemIndex) {
                val index = hourState.firstVisibleItemIndex % 24
                onTimeChanged(index, selectedMinute)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(BrandColors.PrimaryIndigo.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                )
                
                LazyColumn(
                    state = hourState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(1000) { index ->
                        val h = index % 24
                        val isSelected = h == (hourState.firstVisibleItemIndex % 24)
                        Text(
                            text = String.format("%02d", h),
                            style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    else MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) BrandColors.PrimaryIndigo else BrandColors.SlateGray.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
        
        Text(
            text = ":",
            style = MaterialTheme.typography.titleLarge,
            color = BrandColors.SlateGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp)
        )

        // Minutes column
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Minutes", style = MaterialTheme.typography.labelSmall, color = BrandColors.SlateGray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            val baseIndex = 600
            val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = baseIndex + selectedMinute)
            
            LaunchedEffect(minuteState.firstVisibleItemIndex) {
                val index = minuteState.firstVisibleItemIndex % 60
                onTimeChanged(selectedHour, index)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(BrandColors.PrimaryIndigo.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                )

                LazyColumn(
                    state = minuteState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(2000) { index ->
                        val m = index % 60
                        val isSelected = m == (minuteState.firstVisibleItemIndex % 60)
                        Text(
                            text = String.format("%02d", m),
                            style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    else MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) BrandColors.PrimaryIndigo else BrandColors.SlateGray.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
