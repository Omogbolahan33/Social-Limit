package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color styling constants strictly from: White, Blue, Blank, Orange, Brown, Chocolate, Green and Gold
object BrandColors {
    val DarkSlateBg = Color(0xFFF8F9FA)   // Main canvas background: clean white/off-white (zero purple)
    val SlateGray = Color(0xFF546E7A)     // Neutral blue-slate body text
    val TonalSurface = Color(0xFFFFFFFF)  // Primary item container surface background: Pure White
    val PrimaryIndigo = Color(0xFF1565C0) // Main brand accent: Classic Royal Blue
    val SecondaryTeal = Color(0xFF3E2723) // Deep Rich Chocolate Brown for strong highlight contrasts
    val NeonAmber = Color(0xFFE65100)     // Vibrant Orange
    val SolarAmber = Color(0xFFFFB300)    // Warm Solar Amber / Gold for throttling and daylight indicators
    val Gold = Color(0xFFFFB300)          // Brilliant Gold
    val Chocolate = Color(0xFF4E342E)     // Warm Chocolate Brown
    val ForestGreen = Color(0xFF2E7D32)   // Rich Emerald Green
    val ErrorRose = Color(0xFFE65100)     // Alert / limit-reached warnings: Intense Vibrant Orange
    val SoftWhite = Color(0xFF121212)     // High-contrast primary texts: Charcoal Black
    
    val PlatformColors = mapOf(
        "instagram" to Color(0xFFE65100), // Vibrant Orange
        "tiktok" to Color(0xFF121212),    // Blank / Black
        "youtube" to Color(0xFFD84315),   // Deep Burnt Orange
        "twitter/x" to Color(0xFF0288D1), // Sky Blue
        "facebook" to Color(0xFF1565C0),  // Royal Blue
        "linkedin" to Color(0xFF0D47A1),  // Deep Blue
        "pinterest" to Color(0xFF5D4037), // Warm Chocolate Brown
        "whatsapp" to Color(0xFF2E7D32),  // Forest Green
        "other" to Color(0xFF1565C0)      // Royal Blue
    )

    fun getPlatformColor(name: String): Color {
        return PlatformColors[name.lowercase().trim()] ?: PlatformColors["other"]!!
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
            "whatsapp" -> Icons.AutoMirrored.Filled.Chat
            "tiktok_block" -> Icons.Default.Block
            else -> Icons.Default.Apps
        }
    }
}

/**
 * Universal Composable to render real application icons.
 * 1. Checks if the app is installed on the user's Android device and loads the actual system launcher icon via PackageManager.
 * 2. If not installed or running on emulator, displays authentic official brand vector drawables.
 * 3. Falls back gracefully to a stylized M3 brand badge for any custom unrecognized app.
 */
@Composable
fun PlatformAppIcon(
    platformName: String,
    packageName: String? = null,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    clipShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp)
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val targetPkg = packageName ?: when (platformName.lowercase().trim()) {
        "instagram" -> "com.instagram.android"
        "tiktok" -> "com.zhiliaoapp.musically"
        "youtube" -> "com.google.android.youtube"
        "twitter/x", "twitter", "x" -> "com.twitter.android"
        "facebook" -> "com.facebook.katana"
        "whatsapp" -> "com.whatsapp"
        "linkedin" -> "com.linkedin.android"
        "reddit" -> "com.reddit.frontpage"
        "snapchat" -> "com.snapchat.android"
        "netflix" -> "com.netflix.mediaclient"
        "telegram" -> "org.telegram.messenger"
        "pinterest" -> "com.pinterest"
        else -> null
    }

    // 1. Fetch real installed app launcher icon from Android OS if package exists on phone
    val systemAppIcon = remember(targetPkg) {
        if (targetPkg != null) {
            try {
                val drawable = pm.getApplicationIcon(targetPkg)
                val px = (size.value * 2.5f).toInt().coerceAtLeast(64)
                val bitmap = drawable.toBitmap(px, px)
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (systemAppIcon != null) {
        Image(
            bitmap = systemAppIcon,
            contentDescription = platformName,
            modifier = modifier
                .size(size)
                .clip(clipShape),
            contentScale = ContentScale.Crop
        )
        return
    }

    // 2. Official authentic brand vector drawable
    val brandDrawableRes: Int? = remember(platformName) {
        when (platformName.lowercase().trim()) {
            "youtube" -> com.example.R.drawable.ic_brand_youtube
            "instagram" -> com.example.R.drawable.ic_brand_instagram
            "tiktok" -> com.example.R.drawable.ic_brand_tiktok
            "whatsapp" -> com.example.R.drawable.ic_brand_whatsapp
            "twitter/x", "twitter", "x" -> com.example.R.drawable.ic_brand_twitter
            "facebook" -> com.example.R.drawable.ic_brand_facebook
            "linkedin" -> com.example.R.drawable.ic_brand_linkedin
            "reddit" -> com.example.R.drawable.ic_brand_reddit
            "snapchat" -> com.example.R.drawable.ic_brand_snapchat
            "netflix" -> com.example.R.drawable.ic_brand_netflix
            "telegram" -> com.example.R.drawable.ic_brand_telegram
            "pinterest" -> com.example.R.drawable.ic_brand_pinterest
            else -> null
        }
    }

    if (brandDrawableRes != null) {
        Image(
            painter = painterResource(id = brandDrawableRes),
            contentDescription = platformName,
            modifier = modifier
                .size(size)
                .clip(clipShape),
            contentScale = ContentScale.Fit
        )
        return
    }

    // 3. Clean M3 fallback with brand accent
    val platformColor = BrandColors.getPlatformColor(platformName)
    Box(
        modifier = modifier
            .size(size)
            .background(platformColor.copy(alpha = 0.15f), shape = clipShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = platformName.take(1).uppercase(),
            fontWeight = FontWeight.Bold,
            color = platformColor,
            fontSize = (size.value * 0.45f).sp
        )
    }
}

/**
 * Beautiful styled Circular Gauge for Screen Time Progress against overall daily limits and Daylight Sun efficiency.
 */
@Composable
fun ScreentimeRadialChart(
    percentage: Float,
    totalMinutesStr: String,
    goalMinutesStr: String,
    daylightPercentage: Float = 0f,
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "radial_percentage"
    )
    val animatedDaylightPercentage by animateFloatAsState(
        targetValue = daylightPercentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "radial_daylight_percentage"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(152.dp)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStrokeWidth = 9.5.dp.toPx()
            val innerStrokeWidth = 6.dp.toPx()
            val innerPadding = 13.dp.toPx()

            // 1. Outer Ring: Daily Goal progress
            drawArc(
                color = BrandColors.SlateGray.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
            )

            val outerBrush = if (percentage >= 1f) {
                Brush.sweepGradient(listOf(BrandColors.NeonAmber, BrandColors.ErrorRose))
            } else {
                Brush.sweepGradient(
                    listOf(
                        BrandColors.PrimaryIndigo,
                        BrandColors.Gold,
                        BrandColors.PrimaryIndigo
                    )
                )
            }

            drawArc(
                brush = outerBrush,
                startAngle = 135f,
                sweepAngle = 270f * animatedPercentage,
                useCenter = false,
                style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
            )

            // 2. Inner Ring: Daylight sun allocation progress
            val innerSize = Size(
                width = size.width - (innerPadding * 2),
                height = size.height - (innerPadding * 2)
            )
            val innerTopLeft = Offset(innerPadding, innerPadding)

            drawArc(
                color = BrandColors.NeonAmber.copy(alpha = 0.12f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
            )

            val innerColors = when {
                daylightPercentage < 0.15f -> listOf(Color(0xFF2E7D32), Color(0xFF4CAF50))
                daylightPercentage < 0.35f -> listOf(BrandColors.NeonAmber, Color(0xFFFFB300))
                else -> listOf(BrandColors.NeonAmber, BrandColors.ErrorRose)
            }

            drawArc(
                brush = Brush.sweepGradient(innerColors),
                startAngle = 135f,
                sweepAngle = 270f * animatedDaylightPercentage,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = totalMinutesStr,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = BrandColors.SoftWhite,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "of $goalMinutesStr",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BrandColors.SlateGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(BrandColors.PrimaryIndigo, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Goal",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        color = BrandColors.SlateGray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                when {
                                    daylightPercentage < 0.15f -> Color(0xFF2E7D32)
                                    daylightPercentage < 0.35f -> BrandColors.NeonAmber
                                    else -> BrandColors.ErrorRose
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Sun",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        color = BrandColors.SlateGray
                    )
                }
            }
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
    containerColor: Color = Color.White,
    border: androidx.compose.foundation.BorderStroke? = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
) {
    if (days.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandColors.TonalSurface)
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
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        else -> Color(0xFFE3F2FD)
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

data class DaytimeWeatherState(
    val periodName: String,
    val greetingPrefix: String,
    val weatherEmoji: String,
    val smileyEmoji: String,
    val weatherIcon: ImageVector,
    val accentColor: Color,
    val conditionText: String,
    val isDaylight: Boolean
)

object GreetingHelper {
    fun getCurrentDaytimeState(hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): DaytimeWeatherState {
        return when (hour) {
            in 5..11 -> DaytimeWeatherState(
                periodName = "Morning",
                greetingPrefix = "Good Morning",
                weatherEmoji = "🌅",
                smileyEmoji = "😊",
                weatherIcon = Icons.Default.WbSunny,
                accentColor = Color(0xFFFFA000), // Amber Gold
                conditionText = "Sunrise Active",
                isDaylight = true
            )
            in 12..16 -> DaytimeWeatherState(
                periodName = "Afternoon",
                greetingPrefix = "Good Afternoon",
                weatherEmoji = "☀️",
                smileyEmoji = "😎",
                weatherIcon = Icons.Default.WbSunny,
                accentColor = Color(0xFFFFB300), // Solar Amber
                conditionText = "Daylight Peak",
                isDaylight = true
            )
            in 17..20 -> DaytimeWeatherState(
                periodName = "Evening",
                greetingPrefix = "Good Evening",
                weatherEmoji = "🌇",
                smileyEmoji = "😌",
                weatherIcon = Icons.Default.WbTwilight,
                accentColor = Color(0xFFFF7043), // Sunset Coral
                conditionText = "Golden Twilight",
                isDaylight = false
            )
            else -> DaytimeWeatherState(
                periodName = "Night",
                greetingPrefix = "Good Night",
                weatherEmoji = "🌙",
                smileyEmoji = "😴",
                weatherIcon = Icons.Default.NightsStay,
                accentColor = Color(0xFF8C9EFF), // Moon Violet
                conditionText = "Night Recovery",
                isDaylight = false
            )
        }
    }

    fun getDaytimePeriod(hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): Pair<String, String> {
        val state = getCurrentDaytimeState(hour)
        return state.periodName.lowercase() to state.weatherEmoji
    }

    fun formatGreeting(name: String, hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): String {
        val state = getCurrentDaytimeState(hour)
        val trimmed = name.trim()
        return if (trimmed.isNotEmpty()) {
            "Hi, $trimmed ${state.smileyEmoji} Good ${state.periodName.lowercase()} ${state.weatherEmoji}"
        } else {
            "${state.greetingPrefix} ${state.smileyEmoji} ${state.weatherEmoji}"
        }
    }
}

