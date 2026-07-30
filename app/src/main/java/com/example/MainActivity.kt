package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AnalyticsRepository
import com.example.data.AppDatabase
import com.example.ui.ScreentimeApp
import com.example.ui.components.BrandColors
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ScreentimeViewModel
import com.example.viewmodel.ScreentimeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, DAOs, and repository directly
        val database = AppDatabase.getDatabase(this)
        val repository = AnalyticsRepository(
            dailyUsageDao = database.dailyUsageDao(),
            platformGoalDao = database.platformGoalDao(),
            blockScheduleDao = database.blockScheduleDao(),
            usageSessionDao = database.usageSessionDao()
        )
        
        // Instantiate the ViewModel
        val factory = ScreentimeViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[ScreentimeViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            MyApplicationTheme(darkTheme = systemDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrandColors.DarkSlateBg
                ) {
                    ScreentimeApp(viewModel = viewModel)
                }
            }
        }
    }
}
