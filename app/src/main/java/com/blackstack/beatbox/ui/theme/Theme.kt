package com.blackstack.beatbox.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.blackstack.beatbox.AppTheme

/**
 * BeatBox'in tek tema giriş noktası.
 *
 * Renk şeması seçilen [AppTheme]'e göre üretilir. Dinamik temada albüm
 * kapağından çıkarılan [dynamicPrimary] renk kullanılır; diğer temalarda
 * parametre yok sayılır.
 */
@Composable
fun BeatBoxTheme(
    theme: AppTheme,
    dynamicPrimary: Color? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = beatBoxColorScheme(theme, dynamicPrimary),
        typography = BeatBoxTypography,
        shapes = BeatBoxShapes,
        content = content
    )
}

/**
 * Saf fonksiyon: tema + isteğe bağlı albüm rengi -> Material renk şeması.
 *
 * Dinamik renk yalnızca [AppTheme.DYNAMIC] için uygulanır. Diğer temalar
 * kullanıcının açıkça seçtiği paleti korur.
 */
fun beatBoxColorScheme(
    theme: AppTheme,
    dynamicPrimary: Color? = null
): ColorScheme {
    val base = when (theme) {
        AppTheme.DYNAMIC -> darkColorScheme(
            primary = AuroraCyan,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF003840),
            onPrimaryContainer = Color(0xFF80F2FF),
            secondary = Color(0xFF9C27B0),
            onSecondary = Color.White,
            background = Color(0xFF0C0D15),
            onBackground = Color.White,
            surface = Color(0xFF141622),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1E2235),
            onSurfaceVariant = Color(0xFFD0D0D0)
        )

        AppTheme.AMOLED -> darkColorScheme(
            primary = AuroraCyan,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF003840),
            onPrimaryContainer = Color(0xFF80F2FF),
            secondary = Color(0xFFFF2E93),
            onSecondary = Color.White,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF09090D),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF14141A),
            onSurfaceVariant = Color(0xFFD0D0D0)
        )

        AppTheme.NEON_CYBER -> darkColorScheme(
            primary = Color(0xFF00F5D4),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF00473E),
            onPrimaryContainer = Color(0xFF99FCEF),
            secondary = Color(0xFF7B2CBF),
            onSecondary = Color.White,
            background = Color(0xFF180030),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF27004D),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF3B0073),
            onSurfaceVariant = Color(0xFFE0AAFF)
        )

        AppTheme.ROSE_GOLD -> darkColorScheme(
            primary = Color(0xFFF2CC8F),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF473720),
            onPrimaryContainer = Color(0xFFFAF0CA),
            secondary = Color(0xFFFF758F),
            onSecondary = Color.Black,
            background = Color(0xFF21171C),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF33242B),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF47333C),
            onSurfaceVariant = Color(0xFFF5D0E0)
        )

        AppTheme.DARK -> darkColorScheme(
            primary = Color(0xFF9C27B0),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF3B004D),
            onPrimaryContainer = Color(0xFFF2C8FF),
            secondary = AuroraCyan,
            onSecondary = Color.Black,
            background = Color(0xFF0C0A1A),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF171333),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF241D4D),
            onSurfaceVariant = Color(0xFFE2D0FF)
        )
    }

    if (theme != AppTheme.DYNAMIC || dynamicPrimary == null) return base

    val safePrimary = adjustColorForContrast(dynamicPrimary)
    val onPrimaryText = if (safePrimary.luminance() > 0.55f) Color.Black else Color.White
    return base.copy(
        primary = safePrimary,
        onPrimary = onPrimaryText,
        primaryContainer = safePrimary.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = safePrimary.copy(alpha = 0.85f),
        onSecondary = Color.White
    )
}

/**
 * Albümden gelen rengi metin/ikon okunabilirliği için güvenli aralığa çeker.
 * Doygunluğu ve parlaklığı artırır, aşırı parlak renkleri yumuşatır.
 */
fun adjustColorForContrast(color: Color): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    if (hsv[1] < 0.45f) hsv[1] = 0.65f
    if (hsv[2] < 0.65f) hsv[2] = 0.85f
    if (hsv[2] > 0.95f) hsv[2] = 0.95f
    return Color(AndroidColor.HSVToColor(hsv))
}
