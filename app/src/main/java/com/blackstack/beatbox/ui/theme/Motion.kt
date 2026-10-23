package com.blackstack.beatbox.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * BeatBox hareket dili.
 *
 * Tüm yeni bileşenler süre ve yay değerlerini buradan almalı. Amaç, uygulamanın
 * her yerinde aynı "yumuşak ama çevik" hissi korumak.
 */
object BeatBoxMotion {
    /** Dokunma geri bildirimi, ripple ve anlık renk değişimleri. */
    const val Instant = 120

    /** İkon morph, çip ve küçük durum geçişleri. */
    const val Quick = 180

    /** Sayfa geçişi, alt sayfa, standart görünürlük. */
    const val Standard = 280

    /** Hero morph, splash ve sinematik açılışlar. */
    const val Cinematic = 460

    /** Liste öğeleri arasında önerilen gecikme. */
    const val Stagger = 40

    val Soft = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 380f
    )

    val Snappy = spring<Float>(
        dampingRatio = 0.70f,
        stiffness = 900f
    )

    val Bouncy = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = 500f
    )

    val StandardEasing = FastOutSlowInEasing

    /** Hızlı başlayıp yumuşak biten, "premium" hissi veren eğri. */
    val ExpressiveEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    fun <T> soft(durationMillis: Int = Standard): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = StandardEasing)

    fun <T> expressive(durationMillis: Int = Cinematic): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = ExpressiveEasing)

    fun slideSpec(durationMillis: Int = Standard): FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = durationMillis, easing = StandardEasing)
}
