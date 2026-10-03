package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ScreentimeViewModel

data class SuggestedApp(
    val packageName: String,
    val name: String,
    val category: String,
    val defaultSelected: Boolean = true
)

val PRESET_POPULAR_APPS = listOf(
    SuggestedApp("com.instagram.android", "Instagram", "Social Media", true),
    SuggestedApp("com.zhiliaoapp.musically", "TikTok", "Short Video", true),
    SuggestedApp("com.google.android.youtube", "YouTube", "Video Streaming", true),
    SuggestedApp("com.twitter.android", "Twitter/X", "Social & News", true),
    SuggestedApp("com.facebook.katana", "Facebook", "Social Network", false),
    SuggestedApp("com.whatsapp", "WhatsApp", "Messaging & Calls", true),
    SuggestedApp("com.snapchat.android", "Snapchat", "Photo & Social", false),
    SuggestedApp("com.reddit.frontpage", "Reddit", "Forums & News", false),
    SuggestedApp("com.linkedin.android", "LinkedIn", "Professional", false),
    SuggestedApp("com.netflix.mediaclient", "Netflix", "Streaming Video", false),
    SuggestedApp("org.telegram.messenger", "Telegram", "Messaging", false),
    SuggestedApp("com.pinterest", "Pinterest", "Inspiration & Media", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: ScreentimeViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }
    var enteredName by remember { mutableStateOf("") }
    
    // Selected apps map: packageName -> appName
    val selectedApps = remember {
        mutableStateMapOf<String, String>().apply {
            PRESET_POPULAR_APPS.filter { it.defaultSelected }.forEach { put(it.packageName, it.name) }
        }
    }
    
    var selectedGoalMinutes by remember { mutableStateOf(120L) } // 2 hours default
    var hasPermission by remember { mutableStateOf(viewModel.hasUsageStatsPermission()) }
    val installedApps by viewModel.installedApps.collectAsState()
    var showInstalledAppsList by remember { mutableStateOf(false) }

    // Re-check permission on resume
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = viewModel.hasUsageStatsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BrandColors.DarkSlateBg),
        containerColor = BrandColors.DarkSlateBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Welcome Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.SoftWhite
                        )
                    }
                },
                actions = {
                    if (currentStep < 3) {
                        TextButton(
                            onClick = {
                                viewModel.completeOnboarding(
                                    name = enteredName.ifBlank { "Friend" },
                                    selectedApps = selectedApps,
                                    defaultGoalMinutes = selectedGoalMinutes
                                )
                                onFinish()
                            }
                        ) {
                            Text(
                                text = "Skip",
                                color = BrandColors.SlateGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandColors.DarkSlateBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (step in 0..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (step <= currentStep) BrandColors.PrimaryIndigo
                                else BrandColors.SlateGray.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    0 -> StepEnterName(
                        name = enteredName,
                        onNameChange = { enteredName = it },
                        onNext = { currentStep = 1 }
                    )
                    1 -> StepSelectApps(
                        selectedApps = selectedApps,
                        installedApps = installedApps,
                        showInstalledAppsList = showInstalledAppsList,
                        onToggleShowInstalled = {
                            if (!showInstalledAppsList && installedApps.isEmpty()) {
                                viewModel.loadInstalledApps()
                            }
                            showInstalledAppsList = !showInstalledAppsList
                        },
                        onToggleApp = { pkg, name, selected ->
                            if (selected) selectedApps[pkg] = name else selectedApps.remove(pkg)
                        },
                        onSelectAllPresets = {
                            PRESET_POPULAR_APPS.forEach { selectedApps[it.packageName] = it.name }
                        },
                        onClearAllPresets = {
                            selectedApps.clear()
                        },
                        onNext = { currentStep = 2 },
                        onBack = { currentStep = 0 }
                    )
                    2 -> StepSelectGoal(
                        selectedGoalMinutes = selectedGoalMinutes,
                        onGoalSelected = { selectedGoalMinutes = it },
                        onNext = { currentStep = 3 },
                        onBack = { currentStep = 1 }
                    )
                    3 -> StepPermissions(
                        hasPermission = hasPermission,
                        onOpenSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        onFinish = {
                            viewModel.completeOnboarding(
                                name = enteredName.ifBlank { "Friend" },
                                selectedApps = selectedApps,
                                defaultGoalMinutes = selectedGoalMinutes
                            )
                            onFinish()
                        },
                        onBack = { currentStep = 2 }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepEnterName(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(BrandColors.PrimaryIndigo.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = BrandColors.NeonAmber,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Welcome to ScreenTime",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = BrandColors.SoftWhite
            )

            Text(
                text = "Track your sunlight focus, reclaim wasted hours, and build balanced digital habits.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.SlateGray,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "What is your name?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.SoftWhite
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Enter your name (e.g. Alex)") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandColors.PrimaryIndigo)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandColors.PrimaryIndigo,
                    unfocusedBorderColor = BrandColors.SlateGray.copy(alpha = 0.3f),
                    focusedContainerColor = BrandColors.TonalSurface,
                    unfocusedContainerColor = BrandColors.TonalSurface,
                    focusedTextColor = BrandColors.SoftWhite,
                    unfocusedTextColor = BrandColors.SoftWhite
                )
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
        ) {
            Text("Continue", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StepSelectApps(
    selectedApps: Map<String, String>,
    installedApps: List<Pair<String, String>>,
    showInstalledAppsList: Boolean,
    onToggleShowInstalled: () -> Unit,
    onToggleApp: (String, String, Boolean) -> Unit,
    onSelectAllPresets: () -> Unit,
    onClearAllPresets: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Select Monitored Apps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = BrandColors.SoftWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose the apps you'd like ScreenTime to monitor for daylight efficiency and limits:",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.SlateGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Select All / Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedApps.size} apps selected",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.PrimaryIndigo
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectAllPresets) {
                        Text("Select All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandColors.PrimaryIndigo)
                    }
                    TextButton(onClick = onClearAllPresets) {
                        Text("Clear", fontSize = 12.sp, color = BrandColors.SlateGray)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PRESET_POPULAR_APPS) { app ->
                    val isChecked = selectedApps.containsKey(app.packageName)
                    val platformColor = BrandColors.getPlatformColor(app.name)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app.packageName, app.name, !isChecked) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) BrandColors.TonalSurface else BrandColors.TonalSurface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChecked) platformColor.copy(alpha = 0.5f) else BrandColors.SlateGray.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlatformAppIcon(
                                    platformName = app.name,
                                    packageName = app.packageName,
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SoftWhite
                                    )
                                    Text(
                                        text = app.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandColors.SlateGray
                                    )
                                }
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleApp(app.packageName, app.name, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandColors.PrimaryIndigo,
                                    uncheckedColor = BrandColors.SlateGray
                                )
                            )
                        }
                    }
                }

                // Installed apps expansion toggle
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleShowInstalled() },
                        colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = BrandColors.PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (showInstalledAppsList) "Hide Device Installed Apps" else "Add Other Installed Apps from Phone",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite
                                )
                            }
                            Icon(
                                imageVector = if (showInstalledAppsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = BrandColors.SlateGray
                            )
                        }
                    }
                }

                if (showInstalledAppsList) {
                    items(installedApps) { (pkg, label) ->
                        val isChecked = selectedApps.containsKey(pkg)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleApp(pkg, label, !isChecked) },
                            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlatformAppIcon(
                                        platformName = label,
                                        packageName = pkg,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandColors.SoftWhite
                                        )
                                        Text(
                                            text = pkg,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = BrandColors.SlateGray,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleApp(pkg, label, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = BrandColors.PrimaryIndigo)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.3f))
            ) {
                Text("Back", color = BrandColors.SoftWhite)
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
            ) {
                Text("Continue", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StepSelectGoal(
    selectedGoalMinutes: Long,
    onGoalSelected: (Long) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val presets = listOf(
        Triple(60L, "Strict Focus (1 hr/day)", "Best for detox and maximizing productive daytime."),
        Triple(120L, "Balanced Routine (2 hrs/day)", "Recommended. Healthy limit for staying connected."),
        Triple(180L, "Relaxed Pace (3 hrs/day)", "Gradual reduction for heavy social media users.")
    )

    var isCustomMode by remember { mutableStateOf(selectedGoalMinutes !in listOf(60L, 120L, 180L)) }
    var customHours by remember { mutableStateOf((selectedGoalMinutes / 60).toInt().coerceAtLeast(0)) }
    var customMinutes by remember { mutableStateOf((selectedGoalMinutes % 60).toInt().coerceIn(0, 59)) }

    fun applyCustomGoal(h: Int, m: Int) {
        val clampedH = h.coerceAtLeast(0)
        val clampedM = m.coerceIn(0, 59)
        customHours = clampedH
        customMinutes = clampedM
        val total = (clampedH * 60L + clampedM).coerceAtLeast(5L)
        onGoalSelected(total)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(BrandColors.ForestGreen.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = null,
                    tint = BrandColors.ForestGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Set Your Daily Focus Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = BrandColors.SoftWhite
            )

            Text(
                text = "ScreenTime will alert you and track your daylight leak ratio against this daily baseline.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.SlateGray
            )

            // Preset options
            presets.forEach { (mins, title, desc) ->
                val isSelected = !isCustomMode && selectedGoalMinutes == mins
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isCustomMode = false
                            customHours = (mins / 60).toInt()
                            customMinutes = (mins % 60).toInt()
                            onGoalSelected(mins)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BrandColors.PrimaryIndigo.copy(alpha = 0.08f) else BrandColors.TonalSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) BrandColors.PrimaryIndigo else BrandColors.SlateGray.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                isCustomMode = false
                                customHours = (mins / 60).toInt()
                                customMinutes = (mins % 60).toInt()
                                onGoalSelected(mins)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandColors.PrimaryIndigo)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray
                            )
                        }
                    }
                }
            }

            // Custom Goal Option Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isCustomMode = true
                        applyCustomGoal(customHours, customMinutes)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isCustomMode) BrandColors.PrimaryIndigo.copy(alpha = 0.08f) else BrandColors.TonalSurface
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (isCustomMode) 2.dp else 1.dp,
                    if (isCustomMode) BrandColors.PrimaryIndigo else BrandColors.SlateGray.copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCustomMode,
                            onClick = {
                                isCustomMode = true
                                applyCustomGoal(customHours, customMinutes)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandColors.PrimaryIndigo)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Custom Daily Limit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite
                            )
                            Text(
                                text = "Specify your exact target hours and minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray
                            )
                        }
                    }

                    if (isCustomMode) {
                        HorizontalDivider(color = BrandColors.SlateGray.copy(alpha = 0.15f))

                        // Current Custom Duration Badge
                        val totalMins = customHours * 60 + customMinutes
                        val formattedTarget = if (customHours > 0 && customMinutes > 0) {
                            "${customHours}h ${customMinutes}m"
                        } else if (customHours > 0) {
                            "${customHours}h (0m)"
                        } else {
                            "${customMinutes}m"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandColors.PrimaryIndigo.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Limit:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandColors.PrimaryIndigo
                            )
                            Text(
                                text = "$formattedTarget ($totalMins min/day)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = BrandColors.PrimaryIndigo
                            )
                        }

                        // Hours and Minutes Steppers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Hours Selector
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "HOURS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SlateGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = { applyCustomGoal(customHours - 1, customMinutes) },
                                            enabled = customHours > 0
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease hour", tint = BrandColors.PrimaryIndigo)
                                        }
                                        Text(
                                            text = "${customHours}h",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = BrandColors.SoftWhite,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { applyCustomGoal(customHours + 1, customMinutes) },
                                            enabled = customHours < 12
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase hour", tint = BrandColors.PrimaryIndigo)
                                        }
                                    }
                                }
                            }

                            // Minutes Selector
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "MINUTES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandColors.SlateGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val newM = if (customMinutes >= 5) customMinutes - 5 else 55
                                                val newH = if (customMinutes < 5 && customHours > 0) customHours - 1 else customHours
                                                applyCustomGoal(newH, newM)
                                            },
                                            enabled = customHours > 0 || customMinutes > 5
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease minute", tint = BrandColors.PrimaryIndigo)
                                        }
                                        Text(
                                            text = "${customMinutes}m",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = BrandColors.SoftWhite,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val newM = (customMinutes + 5) % 60
                                                val newH = if (customMinutes + 5 >= 60) customHours + 1 else customHours
                                                applyCustomGoal(newH, newM)
                                            },
                                            enabled = customHours < 12
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase minute", tint = BrandColors.PrimaryIndigo)
                                        }
                                    }
                                }
                            }
                        }

                        // Quick chip shortcuts
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandColors.SlateGray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair("30m", 30L),
                                Pair("45m", 45L),
                                Pair("1h 15m", 75L),
                                Pair("1h 30m", 90L),
                                Pair("2h 30m", 150L)
                            ).forEach { (label, mins) ->
                                OutlinedButton(
                                    onClick = {
                                        applyCustomGoal((mins / 60).toInt(), (mins % 60).toInt())
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (totalMins.toLong() == mins) BrandColors.PrimaryIndigo else BrandColors.SlateGray.copy(alpha = 0.2f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (totalMins.toLong() == mins) BrandColors.PrimaryIndigo.copy(alpha = 0.1f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalMins.toLong() == mins) BrandColors.PrimaryIndigo else BrandColors.SoftWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BrandColors.PrimaryIndigo,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "You can adjust individual per-app time limits anytime in the Limits tab.",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.SlateGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.3f))
            ) {
                Text("Back", color = BrandColors.SoftWhite)
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
            ) {
                Text("Continue", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StepPermissions(
    hasPermission: Boolean,
    onOpenSettings: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (hasPermission) BrandColors.ForestGreen.copy(alpha = 0.15f)
                        else BrandColors.PrimaryIndigo.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (hasPermission) BrandColors.ForestGreen else BrandColors.PrimaryIndigo,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = if (hasPermission) "You're All Set!" else "Enable Usage Access",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = BrandColors.SoftWhite
            )

            Text(
                text = if (hasPermission) {
                    "Usage Access is already active. ScreenTime is ready to automatically detect your Day Start and track foreground sessions."
                } else {
                    "Android requires Usage Access to detect foreground app usage and calculate your real-time day start accurately."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.SlateGray,
                lineHeight = 22.sp
            )

            // Privacy guarantee card
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = BrandColors.ForestGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("100% Private & On-Device", fontWeight = FontWeight.Bold, color = BrandColors.SoftWhite, fontSize = 13.sp)
                    }
                    Text(
                        text = "Your usage history is stored entirely in local device storage. No account is required and no analytics are uploaded to the cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandColors.SlateGray
                    )
                }
            }

            if (!hasPermission) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Usage Access in Settings", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandColors.ForestGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandColors.ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Usage Permission Active",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.ForestGreen
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.SlateGray.copy(alpha = 0.3f))
            ) {
                Text("Back", color = BrandColors.SoftWhite)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo)
            ) {
                Text("Get Started", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
