package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color styling constants matching the premium theme
object BrandColors {
    val DarkSlateBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val SlateGray: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val TonalSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val PrimaryIndigo: Color @Composable get() = MaterialTheme.colorScheme.primary
    val SecondaryTeal: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val NeonAmber: Color get() = Color(0xFFFF9500) // Apple orange (always high-contrast)
    val ErrorRose: Color @Composable get() = MaterialTheme.colorScheme.error
    val SoftWhite: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    
    val PlatformColors: Map<String, Color> @Composable get() = mapOf(
        "instagram" to Color(0xFFE1306C),
        "tiktok" to (if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF000000)),
        "youtube" to Color(0xFFFF3B30),
        "twitter/x" to Color(0xFF007AFF),
        "facebook" to Color(0xFF1877F2),
        "linkedin" to Color(0xFF007AFF),
        "pinterest" to Color(0xFFFF3B30),
        "whatsapp" to Color(0xFF34C759),
        "other" to MaterialTheme.colorScheme.primary
    )

    @Composable
    fun getPlatformColor(name: String): Color {
        return PlatformColors[name.lowercase().trim()] ?: MaterialTheme.colorScheme.primary
    }

    fun getPlatformIcon(name: String): ImageVector {
        return when (name.lowercase().trim()) {
            "instagram" -> Icons.Default.CameraAlt
            "tiktok" -> Icons.Default.MusicNote
            "youtube" -> Icons.Default.PlayCircle
            "twitter/x", "twitter" -> Icons.Default.AlternateEmail
            "facebook" -> Icons.Default.People
            "linkedin" -> Icons.Default.Work
            "pinterest" -> Icons.Default.Collections
            "whatsapp" -> Icons.Default.Chat
            "tiktok_block" -> Icons.Default.Block
            else -> Icons.Default.Apps
        }
    }
}

/**
 * Beautiful styled Circular Gauge for Screen Time Progress against overall daily limits.
 */
@Composable
fun ScreentimeRadialChart(
    percentage: Float,
    totalMinutesStr: String,
    goalMinutesStr: String,
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "radial_percentage"
    )

    val colorScheme = MaterialTheme.colorScheme
    val slateGrayColor = BrandColors.SlateGray
    val primaryIndigoColor = BrandColors.PrimaryIndigo
    val secondaryTealColor = BrandColors.SecondaryTeal
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(160.dp)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background track
            drawArc(
                color = slateGrayColor.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic Progress track
            val strokeBrush = Brush.sweepGradient(
                colors = listOf(
                    primaryIndigoColor,
                    secondaryTealColor,
                    primaryIndigoColor
                )
            )

            drawArc(
                brush = strokeBrush,
                startAngle = 135f,
                sweepAngle = 270f * animatedPercentage,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = totalMinutesStr,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.SoftWhite,
                    letterSpacing = (-1).sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "of $goalMinutesStr limit",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BrandColors.SlateGray,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Responsive Custom Weekly Screen Time Trends Bar Chart (Draws standard 7 days of comparisons).
 */
@Composable
fun WeeklyTrendsBarChart(
    days: List<Pair<String, Long>>, // (Day name, Screen Time in minutes)
    modifier: Modifier = Modifier,
    peakLimitMins: Long = 180L,
    containerColor: Color = Color.Unspecified,
    border: androidx.compose.foundation.BorderStroke? = null
) {
    val resolvedBgColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor
    val resolvedBorder = border ?: premiumCardBorder()
    val resolvedElevation = premiumCardElevation()

    if (days.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = resolvedBorder,
            elevation = resolvedElevation
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Text(
                    text = "No analytics data available yet.",
                    color = BrandColors.SlateGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    val maxVal = (days.maxOfOrNull { it.second } ?: 100).coerceAtLeast(100L).toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = resolvedBgColor),
        border = resolvedBorder,
        elevation = resolvedElevation,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Weekly Screen Time trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColors.SoftWhite
            )
            Text(
                text = "Comparing average daily usage",
                style = MaterialTheme.typography.bodySmall,
                color = BrandColors.SlateGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { (dayName, mins) ->
                    val proportion = (mins.toFloat() / maxVal).coerceIn(0.04f, 1.0f)
                    val animatedProportion by animateFloatAsState(
                        targetValue = proportion,
                        animationSpec = tween(durationMillis = 600, delayMillis = 100),
                        label = "bar_height"
                    )

                    val maxMins = days.maxOfOrNull { it.second } ?: 1L
                    val isOverLimit = mins > peakLimitMins
                    val isPeakDay = mins == maxMins && mins > 0
                    val barColor = when {
                        isOverLimit -> BrandColors.ErrorRose
                        isPeakDay -> BrandColors.PrimaryIndigo
                        else -> Color(0xFFE8DEF8)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(animatedProportion)
                                .width(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            barColor,
                                            barColor.copy(alpha = 0.5f)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val barText = if (mins > 60) {
                            String.format(Locale.getDefault(), "%02d:%02d", mins / 60, mins % 60)
                        } else if (mins > 0) {
                            "${mins}m"
                        } else {
                            "0"
                        }
                        Text(
                            text = barText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverLimit) BrandColors.ErrorRose else BrandColors.SoftWhite
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BrandColors.SlateGray,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun premiumCardElevation(): CardElevation {
    return CardDefaults.cardElevation(
        defaultElevation = 4.dp,
        pressedElevation = 2.dp,
        focusedElevation = 4.dp,
        hoveredElevation = 4.dp
    )
}

@Composable
fun premiumCardBorder(): androidx.compose.foundation.BorderStroke? {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    return if (isDark) {
        androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            Color(0xFFFFFFFF).copy(alpha = 0.08f)
        )
    } else {
        null
    }
}

