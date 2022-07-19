package com.wildtangz.cardano.nft.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

private val DarkColorScheme = darkColorScheme(
        primary = WT_BLUE,
        inversePrimary = Color.White,
        secondary = Color.Black,
        tertiary = Color.DarkGray,
        background = WT_BLUE,
        surface = WT_BLUE
)

private val LightColorScheme = lightColorScheme(
        primary = WT_BLUE,
        inversePrimary = Color.Black,
        secondary = Color.White,
        tertiary = Color.Gray,
        background = WT_BLUE,
        surface = WT_BLUE
)

@Composable
fun WildTangzCardanoNFTTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
    )
}