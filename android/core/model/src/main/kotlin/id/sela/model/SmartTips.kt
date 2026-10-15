package id.sela.model

/**
 * Tips cerdas berbasis aturan sederhana (fitur #52).
 *
 * Batasan jujur: ini HEURISTIK STATIS berdasarkan jam dan rata-rata waktu
 * tunggu, BUKAN prediksi machine learning dan BUKAN data real-time. Aturan
 * dicek berurutan dari atas; aturan pertama yang kondisinya cocok menang.
 * Nilai [hourOfDay] diharapkan 0–23.
 */
object SmartTips {
    /**
     * Memilih satu tips untuk [hourOfDay] (0–23) dan rata-rata waktu tunggu
     * [avgWaitMinutes] (menit).
     *
     * Urutan aturan:
     * 1. Jam 6–8 dan tunggu > 10 menit → tips berangkat lebih awal.
     * 2. Tunggu < 4 menit dan jam 9–15 → tips jam lengang.
     * 3. Jam 16–19 dan tunggu > 12 menit → tips jam pulang kerja.
     * 4. [isWeekend] → tips akhir pekan.
     * 5. Selain itu → pantau ETA.
     */
    fun tipFor(hourOfDay: Int, avgWaitMinutes: Double, isWeekend: Boolean = false): String = when {
        hourOfDay in 6..8 && avgWaitMinutes > 10.0 ->
            "Berangkat 15 menit lebih awal — jam sibuk antrean bus panjang."
        avgWaitMinutes < 4.0 && hourOfDay in 9..15 ->
            "Jam segini bus biasanya lengang — enak buat santai."
        hourOfDay in 16..19 && avgWaitMinutes > 12.0 ->
            "Jam pulang kerja: pertimbangkan koridor alternatif."
        isWeekend ->
            "Akhir pekan: jadwal lebih renggang, cek ETA dulu."
        else -> "Pantau ETA live sebelum berangkat."
    }
}
