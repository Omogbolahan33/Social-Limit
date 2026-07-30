package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val activeAlerts by viewModel.activeAlerts.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = viewModel.hasUsageStatsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BrandColors.DarkSlateBg),
        containerColor = BrandColors.DarkSlateBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SOCIAL LIMIT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = BrandColors.SoftWhite,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (hasPermission) "REAL-TIME SECURED" else "PERMISSION REQUIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasPermission) BrandColors.SecondaryTeal else BrandColors.ErrorRose,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (hasPermission) {
                        IconButton(
                            onClick = {
                                viewModel.syncActualUsage()
                                android.widget.Toast.makeText(
                                    context,
                                    "System Usage stats updated successfully!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Manual sync with phone metrics",
                                tint = BrandColors.SoftWhite
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Notification & Preference Settings",
                            tint = BrandColors.SoftWhite
                        )
                    }
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
                    if (hasPermission) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(10.dp)
                                .background(BrandColors.SecondaryTeal, shape = CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(10.dp)
                                .background(BrandColors.ErrorRose, shape = CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandColors.DarkSlateBg,
                    titleContentColor = BrandColors.SoftWhite,
                    actionIconContentColor = BrandColors.SoftWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BrandColors.TonalSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandColors.PrimaryIndigo,
                        selectedTextColor = BrandColors.PrimaryIndigo,
                        indicatorColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Adjust, contentDescription = "Goals") },
                    label = { Text("Limits", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandColors.PrimaryIndigo,
                        selectedTextColor = BrandColors.PrimaryIndigo,
                        indicatorColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Block, contentDescription = "Focus Filters") },
                    label = { Text("Blocks", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandColors.PrimaryIndigo,
                        selectedTextColor = BrandColors.PrimaryIndigo,
                        indicatorColor = BrandColors.PrimaryIndigo.copy(alpha = 0.15f),
                        unselectedIconColor = BrandColors.SlateGray,
                        unselectedTextColor = BrandColors.SlateGray
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Advice") },
                    label = { Text("AI Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                3 -> AIInsightsScreen(viewModel = viewModel)
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showNotificationsDialog) {
        ActiveAlertsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }
}
