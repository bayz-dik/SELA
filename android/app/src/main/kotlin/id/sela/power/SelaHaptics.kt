package id.sela.power

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Jenis getaran SELA (fitur #93).
 *
 * Pola dibedakan supaya pengguna bisa membedakan notifikasi tanpa melihat layar:
 * kedatangan bus (panjang-pendek), peringatan umum (3× pendek), SOS (panjang),
 * dan tick (sangat pendek, untuk umpan balik sentuhan).
 */
enum class HapticKind {
    ARRIVAL,
    ALERT,
    SOS,
    TICK,
}

/**
 * Helper getaran SELA (fitur #93 "Haptic feedback").
 *
 * Memakai [VibrationEffect.createWaveform] yang tersedia sejak API 26 — selaras
 * dengan minSdk 26 sehingga tidak perlu pengecekan versi. Getaran HANYA terjadi
 * bila perangkat benar-benar punya motor getar ([Vibrator.hasVibrator]); di
 * perangkat tanpa vibrator (mis. beberapa tablet/TV), panggilan ini diam saja
 * tanpa error.
 *
 * @param enabled Saklar dari pemanggil (mis. preferensi pengguna). Jika false,
 * fungsi langsung kembali tanpa getaran apa pun.
 */
object SelaHaptics {
    fun buzz(context: Context, kind: HapticKind, enabled: Boolean = true) {
        if (!enabled) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            ?: return
        if (!vibrator.hasVibrator()) return

        val (timings, amplitudes) = when (kind) {
            HapticKind.ARRIVAL -> longArrayOf(0, 300, 150, 150) to intArrayOf(0, 255, 0, 180)
            HapticKind.ALERT -> longArrayOf(0, 120, 100, 120, 100, 120) to
                intArrayOf(0, 200, 0, 200, 0, 200)
            HapticKind.SOS -> longArrayOf(0, 800) to intArrayOf(0, 255)
            HapticKind.TICK -> longArrayOf(0, 20) to intArrayOf(0, 120)
        }
        runCatching {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
}
