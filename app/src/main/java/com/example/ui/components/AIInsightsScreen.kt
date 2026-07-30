package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AiWeeklyAdviceState
import com.example.viewmodel.ScreentimeViewModel

@Composable
fun AIInsightsScreen(
    viewModel: ScreentimeViewModel,
    modifier: Modifier = Modifier
) {
    val adviceState by viewModel.aiAdviceState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BrandColors.PrimaryIndigo,
                            BrandColors.SecondaryTeal
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEMINI ADVISOR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A personalized AI coaching model built to review your social screen boundaries and deliver targeted productivity hacks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Action Trigger Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = premiumCardElevation(),
            border = premiumCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Analyze Digital Habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.SoftWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your daily screen times, active blocking rules, and limits will be assessed dynamically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.SlateGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.fetchAICoachingAdvice() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Sparkles")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Evaluate Habits with Gemini", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Advice State Renderer
        AnimatedContent(
            targetState = adviceState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ai_state_animation"
        ) { state ->
            when (state) {
                is AiWeeklyAdviceState.Idle -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = premiumCardElevation(),
                        border = premiumCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(BrandColors.SlateGray.copy(alpha = 0.15f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Tips",
                                    tint = BrandColors.NeonAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ready to discover wellness insights?",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SoftWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Click the Evaluate button above, and Gemini will pinpoint distractions and outline custom recommendations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SlateGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is AiWeeklyAdviceState.Loading -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = premiumCardElevation(),
                        border = premiumCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = BrandColors.SecondaryTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Running Digital Diagnostics...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.SecondaryTeal,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Compiling platform averages & testing focus filters against goals standard...",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandColors.SlateGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is AiWeeklyAdviceState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = premiumCardElevation(),
                        border = premiumCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Idea",
                                    tint = BrandColors.NeonAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Habit Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.SoftWhite
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Markdown or clean paragraph display text
                            Text(
                                text = state.advice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandColors.SoftWhite,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandColors.SlateGray.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Confidence",
                                    tint = BrandColors.SecondaryTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generated in real-time by gemini-3.5-flash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandColors.SecondaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is AiWeeklyAdviceState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = premiumCardElevation(),
                        border = premiumCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Could Not Consult AI Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandColors.ErrorRose
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandColors.SoftWhite
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Note: If you have an API key, enter it down into the AI Studio Secrets panel. This app calls the direct REST endpoints to remain offline-safe and customizable.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandColors.SlateGray
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
