package to.wim.screensavertimeout.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// TVs are dark: one dark Material 3 tonal scheme generated from the icon's middle gradient stop
// #7C4DFF. No dynamic color: Google TV has no wallpaper colours to follow.
private val TvColors =
    darkColorScheme(
        primary = Color(0xFFCDBDFF),
        onPrimary = Color(0xFF34275E),
        primaryContainer = Color(0xFF4B3E76),
        onPrimaryContainer = Color(0xFFE8DEFF),
        inversePrimary = Color(0xFF63568F),
        secondary = Color(0xFFCBC3DC),
        onSecondary = Color(0xFF322E41),
        secondaryContainer = Color(0xFF494458),
        onSecondaryContainer = Color(0xFFE7DEF8),
        tertiary = Color(0xFFEEB8CA),
        onTertiary = Color(0xFF492533),
        tertiaryContainer = Color(0xFF633B4A),
        onTertiaryContainer = Color(0xFFFFD9E4),
        background = Color(0xFF141318),
        onBackground = Color(0xFFE6E1E9),
        surface = Color(0xFF141318),
        onSurface = Color(0xFFE6E1E9),
        surfaceVariant = Color(0xFF2B292F),
        onSurfaceVariant = Color(0xFFCAC4CF),
        inverseSurface = Color(0xFFE6E1E9),
        inverseOnSurface = Color(0xFF312F35),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        border = Color(0xFF938F99),
        borderVariant = Color(0xFF48454E),
        scrim = Color(0xFF000000),
    )

/**
 * TV Material's Text takes its colour from LocalContentColor, which is black unless a Surface sets
 * it. The screen draws its own background, so the content colour is set here: light on dark.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TvColors) {
        CompositionLocalProvider(LocalContentColor provides TvColors.onBackground, content = content)
    }
}
