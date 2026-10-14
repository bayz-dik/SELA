package id.sela.model

import org.junit.Assert.*
import org.junit.Test

/** Uji [SmartTips.tipFor]: aturan tips termasuk batas jam/waktu tunggu dan urutan aturan. */
class SmartTipsTest {

    private val sibuk = "Berangkat 15 menit lebih awal — jam sibuk antrean bus panjang."
    private val lengang = "Jam segini bus biasanya lengang — enak buat santai."
    private val pulang = "Jam pulang kerja: pertimbangkan koridor alternatif."
    private val pekan = "Akhir pekan: jadwal lebih renggang, cek ETA dulu."
    private val netral = "Pantau ETA live sebelum berangkat."

    @Test fun jamSibukPagiTungguLama() {
        assertEquals(sibuk, SmartTips.tipFor(7, 12.0))
    }

    @Test fun batasJamDelapanMasihSibuk() {
        assertEquals(sibuk, SmartTips.tipFor(8, 11.0))
    }

    @Test fun tungguTepatSepuluhMenitBukanJamSibuk() {
        assertEquals(netral, SmartTips.tipFor(7, 10.0))
    }

    @Test fun siangLengangTungguSingkat() {
        assertEquals(lengang, SmartTips.tipFor(10, 3.0))
    }

    @Test fun batasJamLimaBelasMasihLengang() {
        assertEquals(lengang, SmartTips.tipFor(15, 3.9))
    }

    @Test fun tungguTepatEmpatMenitBukanLengang() {
        assertEquals(netral, SmartTips.tipFor(10, 4.0))
    }

    @Test fun jamPulangKerjaTungguLama() {
        assertEquals(pulang, SmartTips.tipFor(18, 15.0))
    }

    @Test fun tungguTepatDuaBelasMenitBukanPulangKerja() {
        assertEquals(netral, SmartTips.tipFor(17, 12.0))
    }

    @Test fun akhirPekan() {
        assertEquals(pekan, SmartTips.tipFor(12, 5.0, isWeekend = true))
    }

    @Test fun aturanJamSibukMenangAtasAkhirPekan() {
        // Aturan dicek berurutan: kondisi jam sibuk cocok lebih dulu.
        assertEquals(sibuk, SmartTips.tipFor(7, 12.0, isWeekend = true))
    }

    @Test fun netralDiLuarSemuaAturan() {
        assertEquals(netral, SmartTips.tipFor(2, 20.0))
        assertEquals(netral, SmartTips.tipFor(12, 8.0))
    }
}
