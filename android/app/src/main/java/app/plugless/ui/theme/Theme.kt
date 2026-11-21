package app.plugless.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ModernLightColorScheme = lightColorScheme(
    primary = BrandBlack,
    onPrimary = Color.White,
    secondary = BrandAccent,
    onSecondary = Color.White,
    background = WhiteBackground,
    onBackground = TextDark,
    surface = WhiteSurface,
    onSurface = TextDark,
    surfaceVariant = CardGrey,
    onSurfaceVariant = TextDark
)

@Composable
fun PluglessTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ModernLightColorScheme,
        typography = Typography,
        content = content
    )
}
