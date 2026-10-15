package id.sela.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji unit untuk [KonduktorAi] — murni JVM, tanpa Android. */
class KonduktorAiTest {

    @Test fun telat_tanpaRuteAlternatif() {
        val out = KonduktorAi.reply("busnya telat banget")
        assertTrue(out.startsWith("Sabar ya..."))
        assertTrue("Lihat ETA live di peta biar pasti." in out)
    }

    @Test fun telat_denganRuteAlternatif() {
        val out = KonduktorAi.reply("kapan datang?", KonduktorCtx(altRoutes = listOf("Koridor 1", "6B")))
        assertTrue("coba cek rute alternatif: Koridor 1, 6B." in out)
    }

    @Test fun kataKunciKapan() {
        assertTrue(KonduktorAi.reply("kapan busnya sampai?").startsWith("Sabar ya..."))
    }

    @Test fun rute() {
        val out = KonduktorAi.reply("rute koridor 1 lewat mana?")
        assertTrue("koridor TransJakarta 1–13 plus JakLingko" in out)
    }

    @Test fun kataKunciJurusan() {
        assertTrue("Pilih halte di peta" in KonduktorAi.reply("jurusan ke blok m?"))
    }

    @Test fun macet() {
        assertTrue("15 menit lebih awal" in KonduktorAi.reply("jalan macet parah"))
    }

    @Test fun halte_denganHalteTerdekat() {
        val out = KonduktorAi.reply("halte apa ini?", KonduktorCtx(nearestStopName = "Dukuh Atas"))
        assertEquals("Halte terdekatmu: Dukuh Atas. Buka detailnya buat lihat ETA.", out)
    }

    @Test fun halte_tanpaHalteTerdekat() {
        assertEquals(
            "Pilih halte di peta buat lihat ETA live.",
            KonduktorAi.reply("info halte dong"),
        )
    }

    @Test fun sapaan() {
        assertEquals(
            "Halo! Mau ke mana hari ini? Tanya aja soal bus atau halte.",
            KonduktorAi.reply("halo"),
        )
        assertTrue("Halo!" in KonduktorAi.reply("Selamat pagi"))
    }

    @Test fun prioritas_kataKunciSpesifikMengalahkanSapaan() {
        // "halo, busnya kapan datang?" mengandung sapaan DAN kata kunci "kapan":
        // kata kunci spesifik harus menang.
        val out = KonduktorAi.reply("halo, busnya kapan datang?")
        assertTrue(out.startsWith("Sabar ya..."))
    }

    @Test fun prioritas_telatMengalahkanRute() {
        val out = KonduktorAi.reply("rute 1 telat terus")
        assertTrue(out.startsWith("Sabar ya..."))
    }

    @Test fun default_pertanyaanTakDikenal() {
        val out = KonduktorAi.reply("berapa harga tiket kereta?")
        assertTrue(out.startsWith("Aku versi offline"))
        assertTrue("bus koridor 1 kapan datang" in out)
    }

    @Test fun caseInsensitive() {
        assertTrue(KonduktorAi.reply("MACET").contains("nyebelin"))
    }
}
