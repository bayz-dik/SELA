package id.sela.model

/**
 * Petunjuk sisi pintu bus (fitur #65).
 *
 * Batasan JUJUR: ini HEURISTIK KASAR, bukan deteksi pintu beneran. Logikanya
 * hanya membandingkan arah gerak bus dengan arah ke halte: kalau bus melaju
 * kira-kira searah halte, diasumsikan pintu keluar di sisi kanan arah gerak
 * (patokan bus Indonesia yang setir kanan). Kenyataannya sisi pintu tergantung
 * tipe bus (lantai tinggi BRT vs. lantai rendah), sisi peron halte, dan arah
 * pintu masuk/keluar yang berbeda-beda — tidak ada satupun yang bisa
 * disimpulkan dari dua angka sudut ini. Jangan diandalkan untuk keputusan
 * keselamatan; anggap sebagai tebakan awal saja.
 */
object DoorSideHint {
    /**
     * Menebak sisi pintu keluar dari selisih sudut arah.
     *
     * Selisih = (stopBearingDeg − busBearingDeg) dinormalisasi ke [0, 360).
     * Selisih < 90° atau > 270° berarti bus melaju kira-kira searah halte
     * → "Pintu kanan"; selain itu → "Pintu kiri".
     *
     * @param busBearingDeg arah gerak bus dalam derajat (0 = utara, searah jarum jam).
     * @param stopBearingDeg arah dari bus ke halte dalam derajat (konvensi sama).
     * @return "Pintu kanan" atau "Pintu kiri".
     */
    fun hint(busBearingDeg: Double, stopBearingDeg: Double): String {
        val diff = ((stopBearingDeg - busBearingDeg) % 360.0 + 360.0) % 360.0
        return if (diff < 90.0 || diff > 270.0) "Pintu kanan" else "Pintu kiri"
    }
}
