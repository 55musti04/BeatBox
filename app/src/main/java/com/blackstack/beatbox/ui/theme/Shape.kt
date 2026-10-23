package com.blackstack.beatbox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Aurora Glass köşe ölçeği.
 *
 * Kartlarda 20–24 dp, cam panellerde 24–32 dp, kontrol dock'unda tam yuvarlak
 * kullanılır. Material bileşenleri için de aynı ölçek geçerlidir.
 */
val BeatBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Yüzen cam paneller ve alt sayfalar için. */
val GlassPanelShape = RoundedCornerShape(28.dp)

/** Mini player, kontrol dock'u ve pill navigasyon için. */
val PillShape = RoundedCornerShape(percent = 50)

/** Albüm kapağı ve büyük görsel kartlar için. */
val ArtworkShape = RoundedCornerShape(32.dp)
