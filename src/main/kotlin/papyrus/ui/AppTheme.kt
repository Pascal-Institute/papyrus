package papyrus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Papyrus App Color Palette
 * Modern, professional financial analysis theme
 */
object AppColors {
    // Primary Brand Colors
    val Primary = Color(0xFF1A73E8)          // Google Blue
    val PrimaryVariant = Color(0xFF1557B0)   // Darker Blue
    val PrimaryLight = Color(0xFFE8F0FE)     // Light Blue Background
    
    // Secondary/Accent Colors
    val Secondary = Color(0xFF00897B)        // Teal
    val SecondaryVariant = Color(0xFF00695C)
    val SecondaryLight = Color(0xFFE0F2F1)
    
    // Status Colors
    val Success = Color(0xFF34A853)          // Green
    val SuccessLight = Color(0xFFE6F4EA)
    val Warning = Color(0xFFFBBC04)          // Yellow
    val WarningLight = Color(0xFFFEF7E0)
    val Error = Color(0xFFEA4335)            // Red
    val ErrorLight = Color(0xFFFCE8E6)
    val Info = Color(0xFF4285F4)             // Blue
    val InfoLight = Color(0xFFE8F0FE)
    
    // Neutral Colors
    val Background = Color(0xFFF8F9FA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F3F4)
    val OnBackground = Color(0xFF202124)
    val OnSurface = Color(0xFF202124)
    val OnSurfaceSecondary = Color(0xFF5F6368)
    val Divider = Color(0xFFDADCE0)
    
    // Card Colors
    val CardBackground = Color(0xFFFFFFFF)
    val CardElevated = Color(0xFFFAFAFA)
    
    // Financial Specific Colors
    val Revenue = Color(0xFF1A73E8)
    val Income = Color(0xFF34A853)
    val Assets = Color(0xFF00897B)
    val Liabilities = Color(0xFFEA4335)
    val Equity = Color(0xFF9334E6)
    val EPS = Color(0xFFFBBC04)
    
    // Dark Theme Colors
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnSurface = Color(0xFFE8EAED)
}

/**
 * Custom Typography for the app
 */
object AppTypography {
    val Headline1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = AppColors.OnSurface
    )
    
    val Headline2 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        color = AppColors.OnSurface
    )
    
    val Headline3 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
        color = AppColors.OnSurface
    )
    
    val Subtitle1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        color = AppColors.OnSurface
    )
    
    val Subtitle2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        color = AppColors.OnSurfaceSecondary
    )
    
    val Body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        color = AppColors.OnSurface
    )
    
    val Body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        color = AppColors.OnSurfaceSecondary
    )
    
    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = AppColors.OnSurfaceSecondary
    )
    
    val Button = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp
    )
    
    val Monospace = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        color = AppColors.OnSurface
    )
}

/**
 * Custom Shapes for the app
 */
object AppShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(50)
}

/**
 * Dimensions and Spacing
 */
object AppDimens {
    val PaddingXSmall = 4.dp
    val PaddingSmall = 8.dp
    val PaddingMedium = 16.dp
    val PaddingLarge = 24.dp
    val PaddingXLarge = 32.dp
    
    val IconSmall = 16.dp
    val IconMedium = 24.dp
    val IconLarge = 32.dp
    val IconXLarge = 48.dp
    val IconHuge = 64.dp
    
    val CardElevation = 2.dp
    val CardElevationHigh = 4.dp
    
    val SidebarWidth = 380.dp
    val MinCardWidth = 300.dp
    
    val ButtonHeight = 40.dp
    val ButtonHeightLarge = 48.dp
    
    val DividerThickness = 1.dp
    
    val ProgressBarHeight = 4.dp
}

/**
 * Composable Material Theme wrapper for the app
 */
@Composable
fun PapyrusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors(
            primary = AppColors.Primary,
            primaryVariant = AppColors.PrimaryVariant,
            secondary = AppColors.Secondary,
            secondaryVariant = AppColors.SecondaryVariant,
            background = AppColors.DarkBackground,
            surface = AppColors.DarkSurface,
            error = AppColors.Error,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = AppColors.DarkOnSurface,
            onSurface = AppColors.DarkOnSurface,
            onError = Color.White
        )
    } else {
        lightColors(
            primary = AppColors.Primary,
            primaryVariant = AppColors.PrimaryVariant,
            secondary = AppColors.Secondary,
            secondaryVariant = AppColors.SecondaryVariant,
            background = AppColors.Background,
            surface = AppColors.Surface,
            error = AppColors.Error,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = AppColors.OnBackground,
            onSurface = AppColors.OnSurface,
            onError = Color.White
        )
    }

    val typography = Typography(
        h1 = AppTypography.Headline1,
        h2 = AppTypography.Headline2,
        h3 = AppTypography.Headline3,
        h4 = AppTypography.Headline3,
        h5 = AppTypography.Subtitle1,
        h6 = AppTypography.Subtitle1,
        subtitle1 = AppTypography.Subtitle1,
        subtitle2 = AppTypography.Subtitle2,
        body1 = AppTypography.Body1,
        body2 = AppTypography.Body2,
        button = AppTypography.Button,
        caption = AppTypography.Caption
    )

    val shapes = Shapes(
        small = AppShapes.Small,
        medium = AppShapes.Medium,
        large = AppShapes.Large
    )

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
