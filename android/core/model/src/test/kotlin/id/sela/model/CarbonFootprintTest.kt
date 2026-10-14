package id.sela.model

import org.junit.Assert.*
import org.junit.Test

/** Uji [CarbonFootprint]: rumus estimasi dan format teks bagikan. */
class CarbonFootprintTest {

    @Test fun sepuluhTripMenghematEmpatSetengahKg() {
        assertEquals(4.5, CarbonFootprint.co2SavedKg(10), 1e-9)
    }

    @Test fun satuTripMenghemat045Kg() {
        // 1 × 7,5 km × 0,06 kg/km = 0,45 kg.
        assertEquals(0.45, CarbonFootprint.co2SavedKg(1), 1e-9)
    }

    @Test fun nolTripNolKg() {
        assertEquals(0.0, CarbonFootprint.co2SavedKg(0), 0.0)
    }

    @Test fun teksBagikanSatuDesimalKomaIndonesia() {
        assertEquals(
            "Bulan ini aku hemat 4,5 kg CO₂ naik TransJakarta dibanding motor (via SELA)",
            CarbonFootprint.shareText(4.5),
        )
    }

    @Test fun teksBagikanNol() {
        assertEquals(
            "Bulan ini aku hemat 0,0 kg CO₂ naik TransJakarta dibanding motor (via SELA)",
            CarbonFootprint.shareText(0.0),
        )
    }

    @Test fun teksBagikanDibulatkanSatuDesimal() {
        assertEquals(
            "Bulan ini aku hemat 12,3 kg CO₂ naik TransJakarta dibanding motor (via SELA)",
            CarbonFootprint.shareText(12.34),
        )
    }
}
