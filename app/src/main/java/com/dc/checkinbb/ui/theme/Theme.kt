package com.dc.checkinbb.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import com.dc.checkinbb.data.AppTheme

private val BoyColorScheme = lightColorScheme(
    primary = BoyPrimary,
    secondary = BoySecondary,
    background = BoyBackground,
    surface = BoySurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val GirlColorScheme = lightColorScheme(
    primary = GirlPrimary,
    secondary = GirlSecondary,
    background = GirlBackground,
    surface = GirlSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun CheckInBBTheme(
    appTheme: AppTheme = AppTheme.BOY,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic to enforce our themes
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.BOY -> BoyColorScheme
        AppTheme.GIRL -> GirlColorScheme
    }
    
    // ... rest of the function ...
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
