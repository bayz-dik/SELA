package id.sela

import id.sela.voice.VoiceAction
import id.sela.voice.VoiceCommand
import org.junit.Assert.*
import org.junit.Test

/**
 * Test parser perintah suara (fitur #10) — murni, tanpa Android.
 */
class VoiceCommandTest {
    @Test fun koridorKapanDatang() {
        val action = VoiceCommand.parseCommand("koridor 1 kapan datang")
        assertEquals(VoiceAction.ShowEta("1"), action)
    }

    @Test fun busDenganHuruf() {
        assertEquals(VoiceAction.ShowEta("1A"), VoiceCommand.parseCommand("bus 1a kapan"))
        assertEquals(VoiceAction.ShowEta("6B"), VoiceCommand.parseCommand("kapan bus 6b datang"))
    }

    @Test fun ruteDiDepan() {
        assertEquals(VoiceAction.ShowEta("9"), VoiceCommand.parseCommand("9 jam berapa datang"))
    }

    @Test fun halteTerdekat() {
        assertEquals(VoiceAction.NearestStop, VoiceCommand.parseCommand("halte terdekat di mana"))
        assertEquals(VoiceAction.NearestStop, VoiceCommand.parseCommand("tunjukkan halte paling dekat"))
    }

    @Test fun pulang() {
        assertEquals(VoiceAction.GoHome, VoiceCommand.parseCommand("antar aku pulang"))
        assertEquals(VoiceAction.GoHome, VoiceCommand.parseCommand("pulang"))
    }

    @Test fun unknown() {
        assertEquals(VoiceAction.Unknown, VoiceCommand.parseCommand("halo apa kabar"))
        assertEquals(VoiceAction.Unknown, VoiceCommand.parseCommand(""))
        // Menyebut rute tanpa kata tanya → bukan perintah ETA.
        assertEquals(VoiceAction.Unknown, VoiceCommand.parseCommand("koridor 1"))
    }
}
