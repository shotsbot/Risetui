package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun animateColorScheme(scheme: ColorScheme): ColorScheme {
    val spec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 500)
    return ColorScheme(
        primary = animateColorAsState(scheme.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(scheme.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(scheme.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(scheme.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(scheme.inversePrimary, spec, label = "inversePrimary").value,
        secondary = animateColorAsState(scheme.secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(scheme.onSecondary, spec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(scheme.secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(scheme.onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(scheme.tertiary, spec, label = "tertiary").value,
        onTertiary = animateColorAsState(scheme.onTertiary, spec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(scheme.tertiaryContainer, spec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(scheme.onTertiaryContainer, spec, label = "onTertiaryContainer").value,
        background = animateColorAsState(scheme.background, spec, label = "background").value,
        onBackground = animateColorAsState(scheme.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(scheme.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(scheme.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(scheme.surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(scheme.onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(scheme.surfaceTint, spec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(scheme.inverseSurface, spec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(scheme.inverseOnSurface, spec, label = "inverseOnSurface").value,
        error = animateColorAsState(scheme.error, spec, label = "error").value,
        onError = animateColorAsState(scheme.onError, spec, label = "onError").value,
        errorContainer = animateColorAsState(scheme.errorContainer, spec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(scheme.onErrorContainer, spec, label = "onErrorContainer").value,
        outline = animateColorAsState(scheme.outline, spec, label = "outline").value,
        outlineVariant = animateColorAsState(scheme.outlineVariant, spec, label = "outlineVariant").value,
        scrim = animateColorAsState(scheme.scrim, spec, label = "scrim").value,
        surfaceBright = animateColorAsState(scheme.surfaceBright, spec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(scheme.surfaceDim, spec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(scheme.surfaceContainer, spec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(scheme.surfaceContainerHigh, spec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(scheme.surfaceContainerHighest, spec, label = "surfaceContainerHighest").value,
        surfaceContainerLow = animateColorAsState(scheme.surfaceContainerLow, spec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(scheme.surfaceContainerLowest, spec, label = "surfaceContainerLowest").value,
    )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val animatedColorScheme = animateColorScheme(colorScheme)

  MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
}
