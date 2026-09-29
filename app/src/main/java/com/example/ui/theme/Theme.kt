package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00BCD4),
    secondary = Color(0xFF00B0FF),
    tertiary = TagBasilGreen,
    background = GoogleDarkBackground,
    surface = GoogleDarkSurface,
    surfaceVariant = GoogleDarkCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = GoogleBluePrimary,
    secondary = GoogleBlueSecondary,
    tertiary = TagSageGreen,
    background = GoogleSurfaceLight,
    surface = GoogleCardBackground,
    surfaceVariant = Color(0xFFF1F3F4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F1F1F),
    onSurface = Color(0xFF1F1F1F)
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF49454F),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onBackground = Color(0xFFE6E0E9),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFA5D6A7),
    background = Color(0xFF1B1F1B),
    surface = Color(0xFF1B1F1B),
    surfaceVariant = Color(0xFF424940),
    onPrimary = Color(0xFF1B5E20),
    onSecondary = Color(0xFF2E7D32),
    onBackground = Color(0xFFE8ECE8),
    onSurface = Color(0xFFE8ECE8),
    onSurfaceVariant = Color(0xFFC2C9BD)
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF81C784),
    background = Color(0xFFFCFDFC),
    surface = Color(0xFFFCFDFC),
    surfaceVariant = Color(0xFFDEE5D8),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF424940)
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF48FB1),
    secondary = Color(0xFFF48FB1),
    background = Color(0xFF211A1C),
    surface = Color(0xFF211A1C),
    surfaceVariant = Color(0xFF53434B),
    onPrimary = Color(0xFF880E4F),
    onSecondary = Color(0xFFAD1457),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFFD7C1CB)
)

private val PinkLightColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    secondary = Color(0xFFF06292),
    background = Color(0xFFFEFDFD),
    surface = Color(0xFFFEFDFD),
    surfaceVariant = Color(0xFFF4DED9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E1C1D),
    onSurface = Color(0xFF1E1C1D),
    onSurfaceVariant = Color(0xFF53434B)
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB74D),
    secondary = Color(0xFFFFCC80),
    background = Color(0xFF211C18),
    surface = Color(0xFF211C18),
    surfaceVariant = Color(0xFF52443C),
    onPrimary = Color(0xFFE65100),
    onSecondary = Color(0xFFF57C00),
    onBackground = Color(0xFFEDE0D4),
    onSurface = Color(0xFFEDE0D4),
    onSurfaceVariant = Color(0xFFD7C3B5)
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9800),
    secondary = Color(0xFFFFB74D),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF4E0D2),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF201A17),
    onSurface = Color(0xFF201A17),
    onSurfaceVariant = Color(0xFF52443C)
)

@Composable
fun DailyPlannerTheme(
    darkTheme: Boolean = true,
    themeColor: String = "Default",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeColor) {
        "Purple" -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        "Green" -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        "Pink" -> if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
        "Orange" -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        else -> if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Backwards compatibility alias for template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    DailyPlannerTheme(darkTheme = darkTheme, themeColor = "Default", content = content)
}
