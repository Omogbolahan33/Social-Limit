package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.viewmodel.ScreentimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreentimeApp(
    viewModel: ScreentimeViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember { mutableStateOf(viewModel.hasUsageStatsPermission()) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val permitted = viewModel.hasUsageStatsPermission()
                hasPermission = permitted
                if (permitted) {
                    viewModel.syncActualUsage()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!onboardingCompleted || userName.isBlank() || !hasPermission) {
        OnboardingScreen(
            viewModel = viewModel,
            onFinish = {
                hasPermission = viewModel.hasUsageStatsPermission()
            },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BrandColors.DarkSlateBg),
        containerColor = BrandColors.DarkSlateBg,
        topBar = {
            val daytime = GreetingHelper.getCurrentDaytimeState()
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User Avatar with daytime glowing accent ring
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(daytime.accentColor.copy(alpha = 0.15f), shape = CircleShape)
                                .border(1.5.dp, daytime.accentColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userName.isNotBlank()) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = BrandColors.SoftWhite
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BrandColors.SoftWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (userName.isNotBlank()) "Hi, $userName ${daytime.smileyEmoji}" else "${daytime.greetingPrefix} ${daytime.smileyEmoji}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            // Real daytime/weather icon and condition badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = daytime.weatherIcon,
                                    contentDescription = daytime.conditionText,
                                    tint = daytime.accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${daytime.weatherEmoji} ${daytime.conditionText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = daytime.accentColor
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNotificationsDialog = true }
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Active Notifications Bell Icon",
                                tint = if (activeAlerts.isNotEmpty()) BrandColors.ErrorRose else BrandColors.SoftWhite
                            )
                            if (activeAlerts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(BrandColors.ErrorRose, shape = CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandColors.DarkSlateBg,
                    actionIconContentColor = BrandColors.SoftWhite
                )
            )
        },
        bottomBar = {
            val daytime = GreetingHelper.getCurrentDaytimeState()
            NavigationBar(
                containerColor = BrandColors.TonalSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Tab 0: Dashboard
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = daytime.weatherIcon,
                            contentDescription = "Daylight Dashboard"
                        )
                    },
                    label = { Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = daytime.accentColor,
                        selectedTextColor = daytime.accentColor,
                        indicatorColor = daytime.accentColor.copy(alpha = 0.18f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )

                // Tab 1: Limits
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = "Goals & Limits") },
                    label = { Text("Limits", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandColors.PrimaryIndigo,
                        selectedTextColor = BrandColors.PrimaryIndigo,
                        indicatorColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )

                // Tab 2: Focus & Shield
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Focus Shield") },
                    label = { Text("Blocks", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandColors.PrimaryIndigo,
                        selectedTextColor = BrandColors.PrimaryIndigo,
                        indicatorColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )

                // Tab 3: Settings
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandColors.PrimaryIndigo,
                        selectedTextColor = BrandColors.PrimaryIndigo,
                        indicatorColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BrandColors.DarkSlateBg)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel = viewModel)
                1 -> GoalsScreen(viewModel = viewModel)
                2 -> BlockScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    if (showNotificationsDialog) {
        ActiveAlertsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }
}
