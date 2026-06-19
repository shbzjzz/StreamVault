package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val StreamVaultColorScheme = darkColorScheme(
  primary = StreamVaultAccent,
  secondary = StreamVaultSub,
  tertiary = StreamVaultGold,
  background = StreamVaultBg,
  surface = StreamVaultBg2,
  onPrimary = Color.White,
  onSecondary = StreamVaultText,
  onBackground = StreamVaultText,
  onSurface = StreamVaultText
)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode by default for cinematic StreamVault
  dynamicColor: Boolean = false, // Disable dynamic colors by default so branding is preserved
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> StreamVaultColorScheme
      else -> StreamVaultColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
