package id.sela.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec

/**
 * Gerakan dunia pastel — ringan, memantul sedikit, tidak pernah kaku.
 * Semua interaksi memakai spring; tween hanya untuk fade/slide sederhana.
 */
object SelaMotion {
    // Durasi (ms)
    const val instant = 120
    const val quick = 200
    const val standard = 300
    const val relaxed = 450
    const val playful = 650

    // Easing
    val emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Spring memantul — untuk kemunculan kartu, maskot, dan press effect. */
    fun bouncySpring(): SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 380f)

    /** Spring lembut — untuk perpindahan layout dan sheet. */
    fun gentleSpring(): SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 220f)

    /** Spring tegas tanpa pantulan — untuk perubahan kecil yang presisi. */
    fun snappySpring(): SpringSpec<Float> =
        spring(dampingRatio = 1f, stiffness = 500f)

    /** Tween emphasized — untuk fade dan slide. */
    fun <T> emphasizedTween(durationMillis: Int = standard): AnimationSpec<T> =
        tween(durationMillis, easing = emphasized)

    // Press effect tombol
    val pressScaleDown = 0.94f
    val pressScaleMin = 0.90f
}
