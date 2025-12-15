package dev.jasonpearson.design.system.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.jasonpearson.experimentation.Experiment
import dev.jasonpearson.experimentation.ExperimentRepository

// CompositionLocal for providing experiments to composables
val LocalExperiments = staticCompositionLocalOf<List<Experiment<*>>> { emptyList() }

/** Helper function to get a specific experiment by name from the current context */
@Composable
inline fun <reified T : Experiment<*>> getExperiment(experimentName: String): T? {
  val experiments = LocalExperiments.current
  return experiments.find { it.name == experimentName } as? T
}

// JP Light Color Scheme
private val JPLightColorScheme =
    lightColorScheme(
        primary = JPLalala,
        onPrimary = JPWhite,
        primaryContainer = JPWhite,
        onPrimaryContainer = JPLalala,
        secondary = JPRed,
        onSecondary = JPWhite,
        secondaryContainer = JPRed,
        onSecondaryContainer = JPWhite,
        tertiary = JPLalala,
        onTertiary = JPWhite,
        tertiaryContainer = JPWhite,
        onTertiaryContainer = JPDarkGrey,
        error = JPError,
        onError = JPWhite,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = JPEggshell,
        onBackground = JPLalala,
        surface = JPWhite,
        onSurface = JPBlack,
        surfaceVariant = JPEggshell,
        onSurfaceVariant = JPLalala,
        outline = JPDarkGrey,
        outlineVariant = JPDarkGrey,
        scrim = JPBlack,
        inverseSurface = JPLalala,
        inverseOnSurface = JPWhite,
        inversePrimary = JPLalala,
        surfaceDim = JPEggshell,
        surfaceBright = JPWhite,
        surfaceContainerLowest = JPEggshell,
        surfaceContainerLow = JPEggshell,
        surfaceContainer = JPEggshell,
        surfaceContainerHigh = JPEggshell,
        surfaceContainerHighest = JPEggshell)

// JP Dark Color Scheme
private val JPDarkColorScheme =
    darkColorScheme(
        primary = JPWhite,
        onPrimary = JPLalala,
        primaryContainer = JPDarkGrey,
        onPrimaryContainer = JPWhite,
        secondary = JPRed,
        onSecondary = JPWhite,
        secondaryContainer = JPRed,
        onSecondaryContainer = JPWhite,
        tertiary = JPLightGrey,
        onTertiary = JPLalala,
        tertiaryContainer = JPDarkGrey,
        onTertiaryContainer = JPLightGrey,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = JPBlack,
        onBackground = JPWhite,
        surface = JPLalala,
        onSurface = JPWhite,
        surfaceVariant = JPLalala,
        onSurfaceVariant = JPWhite,
        outline = JPEggshell,
        outlineVariant = JPLalala,
        scrim = JPLalala,
        inverseSurface = JPWhite,
        inverseOnSurface = JPLalala,
        inversePrimary = JPLalala,
        surfaceDim = JPLalala,
        surfaceBright = JPEggshell,
        surfaceContainerLowest = JPLalala,
        surfaceContainerLow = JPLalala,
        surfaceContainer = JPLalala,
        surfaceContainerHigh = JPLalala,
        surfaceContainerHighest = JPBlack)

@Composable
fun JPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to use design system colors
    experimentRepository: ExperimentRepository? = null,
    content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val experiments = experimentRepository?.getExperiments() ?: emptyList()

  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> JPDarkColorScheme
        else -> JPLightColorScheme
      }

  CompositionLocalProvider(
      LocalExperiments provides experiments,
  ) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = JPTypography,
        shapes = JPShapes,
        content = content,
    )
  }
}
