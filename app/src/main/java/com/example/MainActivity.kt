package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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
    private lateinit var viewModel: ScreentimeViewModel

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
        viewModel = ViewModelProvider(this, factory)[ScreentimeViewModel::class.java]

        // Intercept click transition from our EOD notifications
        val openReport = intent.getBooleanExtra("open_eod_report", false)
        if (openReport) {
            viewModel.setShowEodReportDialog(true)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrandColors.DarkSlateBg
                ) {
                    ScreentimeApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openReport = intent.getBooleanExtra("open_eod_report", false)
        if (openReport) {
            viewModel.setShowEodReportDialog(true)
        }
    }
}
