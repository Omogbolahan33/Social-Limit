package com.example.ui.components

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val platform = intent.getStringExtra("blocked_platform") ?: "Social App"
        val reason = intent.getStringExtra("block_reason") ?: "LIMIT_EXCEEDED"

        // Trigger gentle double haptic vibration on entry
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 150, 100, 150)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrandColors.DarkSlateBg
                ) {
                    InterventionContent(
                        platform = platform,
                        reason = reason,
                        onGoHome = {
                            // Minimize/go home
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InterventionContent(
    platform: String,
    reason: String,
    onGoHome: () -> Unit
) {
    val platformColor = BrandColors.getPlatformColor(platform)
    
    // Guided breathing loop animation
    val infiniteTransition = rememberInfiniteTransition(label = "respiration")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Breathing text label helper
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_text"
    )

    val breathingStateText = if (breatheScale > 1.05f) "Inhale deeply..." else "Exhale cleanly..."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandColors.DarkSlateBg,
                        BrandColors.TonalSurface,
                        BrandColors.DarkSlateBg
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Header Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(BrandColors.ErrorRose.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Social Limits Active",
                    tint = BrandColors.ErrorRose,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SOCIAL LEAK SHIELD ENGAGED",
                style = MaterialTheme.typography.labelSmall,
                color = BrandColors.ErrorRose,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        // MIDDLE: Digital Shield Warning and Breathing Coach Bubble
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Block description card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$platform is Locked!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                val blockInfo = when (reason) {
                    "LIMIT_EXCEEDED" -> "You have reached your configured daily usage limit for $platform today."
                    "SCHEDULED_BLOCK" -> "A scheduled Focus block is currently active, restricting $platform."
                    "POMODORO_LOCK" -> "Pomodoro Focus Sprint session is active! Social networks are locked."
                    else -> "Access to $platform is restricted to keep you focused."
                }

                Text(
                    text = blockInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.SlateGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guided Respiration Circle Frame
            Text(
                text = "TAKE A MINDFUL BREATH",
                style = MaterialTheme.typography.labelSmall,
                color = BrandColors.PrimaryIndigo,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(breatheScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(platformColor.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Middle glowing bubble
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(breatheScale * 0.95f)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(platformColor, BrandColors.PrimaryIndigo)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = breathingStateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = breatheAlpha),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // BOTTOM: Safety Action Row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandColors.PrimaryIndigo
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(52.dp)
            ) {
                Text(
                    text = "Acknowledge & Return to safety",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Text(
                text = "Resisting scrolling impulses increases focus efficiency by up to 40%.",
                style = MaterialTheme.typography.labelSmall,
                color = BrandColors.SlateGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}
