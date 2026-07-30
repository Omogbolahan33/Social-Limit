package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockScheduleEntity
import com.example.viewmodel.ScreentimeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockScreen(
    viewModel: ScreentimeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAccessibilityAndBlockerEnabled by remember { mutableStateOf(false) }

    // Periodically inspect if accessibility service is actively permitted
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityAndBlockerEnabled = checkAccessibilityPermission(context)
            delay(2000)
        }
    }

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Schedules, 1 = Pomodoro Lock

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Setup Assistant Header
        if (!isAccessibilityAndBlockerEnabled) {
            AccessibilityAssistCard(
                onEnableClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Redirecting to settings...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sub Navigation tab controls
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = BrandColors.TonalSurface,
            contentColor = BrandColors.PrimaryIndigo,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            divider = {}
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedules", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                selectedContentColor = BrandColors.SoftWhite,
                unselectedContentColor = BrandColors.SlateGray
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pomodoro Lock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                selectedContentColor = BrandColors.SoftWhite,
                unselectedContentColor = BrandColors.SlateGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                0 -> BlockSchedulesTabContent(viewModel = viewModel)
                1 -> BlockerPomodoroTabContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AccessibilityAssistCard(onEnableClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = premiumCardBorder(),
        elevation = premiumCardElevation(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandColors.ErrorRose.copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Active Warning",
                    tint = BrandColors.ErrorRose,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "BLOCK PROTECTION IS OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandColors.ErrorRose,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "To prevent openings of locked apps, activate the Social Limit Blocker service in accessibility settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.ErrorRose),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Turn Blocker Assist ON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockSchedulesTabContent(viewModel: ScreentimeViewModel) {
    val scheduleList by viewModel.allSchedules.collectAsState()
    var showAddBlockDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (scheduleList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "No filters",
                    tint = BrandColors.SlateGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No focus blocks configured",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.SoftWhite
                )
                Text(
                    text = "Configure focused working schedules to lock distracting platforms from launching during designated hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.SlateGray,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showAddBlockDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                ) {
                    Text("Add Focus Block")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Focus Schedule Blocks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.SoftWhite
                    )
                    Text(
                        text = "Targeted apps are actively locked during scheduled working periods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandColors.SlateGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(scheduleList) { schedule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = premiumCardElevation(),
                        border = premiumCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (schedule.isEnabled) BrandColors.PrimaryIndigo.copy(alpha = 0.15f) 
                                            else BrandColors.SlateGray.copy(alpha = 0.15f), 
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Active",
                                        tint = if (schedule.isEnabled) BrandColors.PrimaryIndigo else BrandColors.SlateGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = schedule.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccessTime, 
                                            contentDescription = "Time", 
                                            tint = BrandColors.SlateGray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${schedule.startTime} - ${schedule.endTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandColors.SecondaryTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Switch(
                                    checked = schedule.isEnabled,
                                    onCheckedChange = { viewModel.toggleScheduleEnabled(schedule, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BrandColors.SoftWhite,
                                        checkedTrackColor = BrandColors.PrimaryIndigo,
                                        uncheckedThumbColor = BrandColors.SlateGray,
                                        uncheckedTrackColor = BrandColors.SlateGray.copy(alpha = 0.2f)
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(onClick = { viewModel.deleteSchedule(schedule) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = BrandColors.ErrorRose.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "Locked platforms:",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandColors.SlateGray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val split = schedule.blockedPlatforms.split(",").filter { it.isNotBlank() }
                                if (split.isEmpty()) {
                                    Text("None selected", color = BrandColors.SlateGray, fontSize = 11.sp)
                                } else {
                                    split.forEach { p ->
                                        val pColor = BrandColors.getPlatformColor(p)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(pColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = BrandColors.getPlatformIcon(p),
                                                    contentDescription = null,
                                                    tint = pColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = p,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = pColor,
                                                    fontWeight = FontWeight.Bold
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

        FloatingActionButton(
            onClick = { showAddBlockDialog = true },
            containerColor = BrandColors.PrimaryIndigo,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Schedule")
        }
    }

    if (showAddBlockDialog) {
        var labelInput by remember { mutableStateOf("Study Session") }
        var startTimeInput by remember { mutableStateOf("09:00") }
        var endTimeInput by remember { mutableStateOf("17:00") }
        
        val platformChoices = listOf("Instagram", "TikTok", "YouTube", "Twitter/X", "Facebook", "LinkedIn", "WhatsApp")
        val blockedPlatformsState = remember { mutableStateMapOf<String, Boolean>().apply {
            platformChoices.forEach { put(it, true) }
        }}

        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Configure Focus Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Designate focused intervals to dynamically block access to selected distraction apps.")

                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Block Schedule Title") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTimeInput,
                            onValueChange = { startTimeInput = it },
                            label = { Text("Start (HH:MM)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTimeInput,
                            onValueChange = { endTimeInput = it },
                            label = { Text("End (HH:MM)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Selected Blocker Apps:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandColors.SoftWhite)
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        platformChoices.forEach { platform ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { blockedPlatformsState[platform] = !(blockedPlatformsState[platform] ?: false) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = blockedPlatformsState[platform] ?: false,
                                    onCheckedChange = { blockedPlatformsState[platform] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = BrandColors.PrimaryIndigo)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = BrandColors.getPlatformIcon(platform),
                                    contentDescription = null,
                                    tint = BrandColors.getPlatformColor(platform),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(platform, color = BrandColors.SoftWhite, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val serialized = blockedPlatformsState.filter { it.value }.keys.joinToString(",")
                        val entity = BlockScheduleEntity(
                            label = labelInput,
                            startTime = startTimeInput,
                            endTime = endTimeInput,
                            blockedPlatforms = serialized,
                            isEnabled = true
                        )
                        viewModel.saveSchedule(entity)
                        showAddBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                ) {
                    Text("Save Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BlockerPomodoroTabContent(viewModel: ScreentimeViewModel) {
    val timeLeftSeconds by viewModel.focusTimeLeftSeconds.collectAsState()
    val totalDurationMinutes by viewModel.focusTotalDurationMinutes.collectAsState()
    val activeBlockedPlatforms by viewModel.focusBlockedPlatforms.collectAsState()

    val context = LocalContext.current

    if (timeLeftSeconds > 0) {
        // Intercept back actions when Focus Mode is active so users can't navigate out
        BackHandler {
            android.widget.Toast.makeText(context, "Shield Active: Keep your focus locked!", android.widget.Toast.LENGTH_SHORT).show()
        }

        // ACTIVE FOCUS COUNTDOWN SCREEN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pulsing timer ring visual layout
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowing"
            )

            Text(
                "FOCUS SESSION IS ACTIVE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = BrandColors.SecondaryTeal,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Background visual circular gradient glowing aura
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(BrandColors.PrimaryIndigo.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Sub circular indicator ring
                CircularProgressIndicator(
                    progress = { timeLeftSeconds.toFloat() / (totalDurationMinutes * 60f) },
                    modifier = Modifier.size(190.dp),
                    color = BrandColors.PrimaryIndigo,
                    strokeWidth = 10.dp,
                    trackColor = BrandColors.SlateGray.copy(alpha = 0.15f)
                )

                // Inner static solid bubble
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .background(BrandColors.TonalSurface, CircleShape)
                ) {
                    val minutes = timeLeftSeconds / 60
                    val seconds = timeLeftSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Seconds Left",
                        fontSize = 11.sp,
                        color = BrandColors.SlateGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Locked platform notification summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = premiumCardElevation(),
                border = premiumCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = BrandColors.SecondaryTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "SCREEN SHIELD ENGAGED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandColors.SecondaryTeal,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Attempting to launch any of the following social apps during this sprint will trigger the breath lock overlay instantly.",
                        fontSize = 12.sp,
                        color = BrandColors.SlateGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeBlockedPlatforms.forEach { platform ->
                            val pColor = BrandColors.getPlatformColor(platform)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(BrandColors.getPlatformIcon(platform), contentDescription = null, tint = pColor, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(platform, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pColor)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.stopFocusSession() },
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.ErrorRose.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, BrandColors.ErrorRose.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel Lock Session", color = BrandColors.ErrorRose, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        // CONFIGURE / IDLE POMODORO SCREEN
        var selectedMinutes by remember { mutableStateOf(25) }
        val platformChoices = listOf("Instagram", "TikTok", "YouTube", "Twitter/X", "Facebook", "LinkedIn", "WhatsApp")
        val blockedPlatformsState = remember { mutableStateMapOf<String, Boolean>().apply {
            listOf("Instagram", "TikTok", "YouTube").forEach { put(it, true) }
        }}

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Focused Lock Sprints",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.SoftWhite
                )
                Text(
                    text = "Begin an immediate focused sprint which strictly shields you from opening selected platforms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.SlateGray
                )
            }

            // Duration selector section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sprint Duration Goal:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandColors.SoftWhite
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            Button(
                                onClick = { selectedMinutes = mins },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) BrandColors.PrimaryIndigo else BrandColors.TonalSurface
                                ),
                                border = if (isSelected) null else BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "${mins}m",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else BrandColors.SoftWhite
                                )
                            }
                        }
                    }
                }
            }

            // Platform choices card list
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = premiumCardElevation(),
                    border = premiumCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Lock Distractions & Apps:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandColors.SoftWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        platformChoices.forEach { platform ->
                            val isChecked = blockedPlatformsState[platform] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { blockedPlatformsState[platform] = !isChecked }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { blockedPlatformsState[platform] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = BrandColors.PrimaryIndigo)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = BrandColors.getPlatformIcon(platform),
                                    contentDescription = null,
                                    tint = BrandColors.getPlatformColor(platform),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(platform, color = BrandColors.SoftWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val selectedList = blockedPlatformsState.filter { it.value }.keys.toList()
                        if (selectedList.isEmpty()) {
                            android.widget.Toast.makeText(context, "Select at least 1 app to lock!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.startFocusSession(selectedMinutes, selectedList)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start ${selectedMinutes}-Min Blocker Sprint",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Utility checker for accessibility services enabled
fun checkAccessibilityPermission(context: Context): Boolean {
    val service = "${context.packageName}/${com.example.service.ScreentimeAccessibilityService::class.java.canonicalName}"
    try {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (enabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return settingValue.contains(service) || settingValue.contains(context.packageName)
        }
    } catch (_: Exception) {}
    return false
}
