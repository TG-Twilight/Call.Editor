package com.twilight.calleditor

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import android.app.Activity

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallTheme(preferences: AppPreferences = AppPreferences(), content: @Composable () -> Unit) {
    val dark = when (preferences.theme) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
    val context = LocalContext.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val colors = if (preferences.dynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme() else expressiveLightColorScheme()
    MaterialExpressiveTheme(colorScheme = colors, content = content)
}
