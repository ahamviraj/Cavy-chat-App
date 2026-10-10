package com.virudaya.cavy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CavyPink,
    onPrimary = CavyWhite,
    primaryContainer = CavyPinkContainerLight,
    onPrimaryContainer = CavyPinkDark,
    secondary = CavyBlack,
    onSecondary = CavyWhite,
    background = CavyLightBackground,
    onBackground = CavyLightOnSurface,
    surface = CavyLightSurface,
    onSurface = CavyLightOnSurface,
    surfaceVariant = CavyLightSurfaceVariant,
    onSurfaceVariant = CavyLightOnSurfaceVariant,
    outline = CavyBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CavyPink,
    onPrimary = CavyWhite,
    primaryContainer = CavyPinkContainerDark,
    onPrimaryContainer = CavyPinkLight,
    secondary = CavyWhite,
    onSecondary = CavyBlack,
    background = CavyDarkBackground,
    onBackground = CavyDarkOnSurface,
    surface = CavyDarkSurface,
    onSurface = CavyDarkOnSurface,
    surfaceVariant = CavyDarkSurfaceVariant,
    onSurfaceVariant = CavyDarkOnSurfaceVariant,
    outline = CavyBorderDark
)

@Composable
fun CavyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
