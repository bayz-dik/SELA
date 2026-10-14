package id.sela.power

/**
 * Penghemat baterai (fitur #55).
 *
 * Bukan "mode" yang hidup sendiri — ini tabel interval yang dipakai pemanggil
 * berdasarkan saklar hemat baterai miliknya (preferensi `batterySaver` belum
 * ada di [id.sela.network.TransitPreferences], jadi diteruskan sebagai
 * parameter Boolean):
 *
 * - Lokasi: 10 detik normal → 30 detik saat hemat.
 * - Reconnect MQTT: 15 detik normal → 60 detik saat hemat.
 *
 * Cara pakai (contoh):
 * ```
 * val interval = BatterySaver.locationIntervalMs(batterySaverEnabled)
 * ```
 * Catatan: nilai yang lebih jarang berarti posisi bus & ETA diperbarui lebih
 * lambat — itu trade-off yang disengaja demi baterai.
 */
object BatterySaver {
    /** Interval pembaruan lokasi: 30 dtk (hemat) vs 10 dtk (normal). */
    fun locationIntervalMs(batterySaver: Boolean): Long =
        if (batterySaver) 30_000L else 10_000L

    /** Jeda reconnect MQTT: 60 dtk (hemat) vs 15 dtk (normal). */
    fun mqttReconnectDelayMs(batterySaver: Boolean): Long =
        if (batterySaver) 60_000L else 15_000L
}
