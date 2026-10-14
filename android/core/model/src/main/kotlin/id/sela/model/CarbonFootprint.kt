package id.sela.model

import java.util.Locale

/**
 * Estimasi jejak karbon yang dihemat (fitur #20).
 *
 * Batasan JUJUR: ini ESTIMASI KASAR untuk ilustrasi, BUKAN pengukuran emisi
 * nyata. Asumsi yang dipakai: setiap trip menempuh 7,5 km, dan faktor emisi
 * motor bensin 60 g CO2/km. Angka sebenarnya sangat tergantung jarak tempuh
 * aktual, jenis kendaraan pembanding, kemacetan, dan tingkat keterisian bus.
 * Jangan pakai angka ini untuk klaim resmi, laporan, atau offset karbon.
 */
object CarbonFootprint {
    /**
     * Estimasi CO2 yang dihemat (kg) dibanding naik motor untuk [trips]
     * perjalanan. Rumus: trips × 7,5 km × 0,06 kg CO2/km.
     */
    fun co2SavedKg(trips: Int): Double = trips * 7.5 * 0.06

    /**
     * Teks siap-bagikan, mis. "Bulan ini aku hemat 4,5 kg CO₂ naik TransJakarta
     * dibanding motor (via SELA)". Angka diformat 1 desimal memakai [Locale]
     * "id" (koma sebagai pemisah desimal).
     */
    fun shareText(kg: Double): String =
        "Bulan ini aku hemat ${String.format(Locale("id"), "%.1f", kg)} kg CO₂ naik TransJakarta dibanding motor (via SELA)"
}
