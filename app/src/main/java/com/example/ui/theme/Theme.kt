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

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = Color(0xFF003912),
    primaryContainer = Color(0xFF1B4D2B),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF003912),
    secondaryContainer = Color(0xFF2E5939),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkGreenBackground,
    onBackground = DarkGreenTextPrimary,
    surface = DarkGreenSurface,
    onSurface = DarkGreenTextPrimary,
    surfaceVariant = DarkGreenSurfaceVariant,
    onSurfaceVariant = DarkGreenTextSecondary,
    outline = Color(0xFF2D5A3C),
    outlineVariant = Color(0xFF1E402B)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = SoftMintContainer,
    onPrimaryContainer = Color(0xFF073315),
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = LightSageCard,
    onSecondaryContainer = Color(0xFF0F3820),
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldLight,
    onTertiaryContainer = GoldText,
    background = MintBackground,
    onBackground = TextPrimaryGreen,
    surface = SageSurface,
    onSurface = TextPrimaryGreen,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = TextSecondaryGreen,
    outline = BorderLightGreen,
    outlineVariant = Color(0xFFE0EFE6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We intentionally prefer our curated light green organization theme for high brand polish
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
