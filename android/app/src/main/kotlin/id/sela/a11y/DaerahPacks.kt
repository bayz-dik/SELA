package id.sela.a11y

import java.util.Locale

/**
 * Paket bahasa daerah untuk pengumuman suara (fitur #22).
 *
 * Catatan jujur: terjemahan di bawah ini adalah template sederhana, bukan
 * terjemahan baku/bersertifikat. Kalau ada penutur asli yang mengoreksi,
 * perbaiki teksnya di sini — struktur pack-nya tidak perlu berubah.
 * Kualitas suara mengikuti mesin TTS yang terinstal di HP; SELA tidak
 * membundel paket suara sendiri.
 */

private object JawaPack : AnnouncementPack {
    override val lang = "jawa"
    override val label = "Bahasa Jawa"
    override val locale: Locale = Locale("id", "ID")
    override fun etaAnnouncement(stopName: String, etaMinutes: Int) =
        "Bis tumuju halte $stopName teko $etaMinutes menit maleh."
    override fun nextStopAnnouncement(stopName: String) = "Halte saklajengipun: $stopName"
    override fun boardingReminder(routeName: String) =
        "Bis $routeName sampun caket. Enggal tumuju lawang halte."
    override fun arrivedAnnouncement(stopName: String) = "Sampun dumugi halte $stopName"
}

private object SundaPack : AnnouncementPack {
    override val lang = "sunda"
    override val label = "Bahasa Sunda"
    override val locale: Locale = Locale("id", "ID")
    override fun etaAnnouncement(stopName: String, etaMinutes: Int) =
        "Beus ka halte $stopName datang $etaMinutes menit deui."
    override fun nextStopAnnouncement(stopName: String) = "Halte salajengna: $stopName"
    override fun boardingReminder(routeName: String) =
        "Beus $routeName parantos caket. Geura ka panto halte."
    override fun arrivedAnnouncement(stopName: String) = "Parantos dugi di halte $stopName"
}

private object BetawiPack : AnnouncementPack {
    override val lang = "betawi"
    override val label = "Bahasa Betawi"
    override val locale: Locale = Locale("id", "ID")
    override fun etaAnnouncement(stopName: String, etaMinutes: Int) =
        "Bis ke halte $stopName dateng $etaMinutes menit lagi."
    override fun nextStopAnnouncement(stopName: String) = "Halte berikutnye: $stopName"
    override fun boardingReminder(routeName: String) =
        "Bis $routeName udah deket. Buruan ke pintu halte."
    override fun arrivedAnnouncement(stopName: String) = "Udah sampe di halte $stopName"
}

/** Semua pack yang tersedia, untuk daftar pilihan di pengaturan. */
val announcementPacks: List<AnnouncementPack> =
    listOf(IndonesianPack, JawaPack, SundaPack, BetawiPack)

/**
 * Ambil pack untuk [lang] (perbandingan case-insensitive).
 * Bahasa yang tidak dikenal jatuh kembali ke Bahasa Indonesia.
 */
fun packFor(lang: String): AnnouncementPack =
    announcementPacks.firstOrNull { it.lang.equals(lang, ignoreCase = true) } ?: IndonesianPack
