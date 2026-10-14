package id.sela.model

import org.junit.Assert.*
import org.junit.Test

/** Uji [DoorSideHint.hint]: heuristik sisi pintu dari selisih sudut, termasuk batas. */
class DoorSideHintTest {

    @Test fun searahPenuhPintuKanan() {
        assertEquals("Pintu kanan", DoorSideHint.hint(90.0, 90.0))
    }

    @Test fun selisihKecilPintuKanan() {
        assertEquals("Pintu kanan", DoorSideHint.hint(0.0, 45.0))
    }

    @Test fun selisihTepatDiBawah90PintuKanan() {
        assertEquals("Pintu kanan", DoorSideHint.hint(0.0, 89.9))
    }

    @Test fun selisihTepat90PintuKiri() {
        assertEquals("Pintu kiri", DoorSideHint.hint(0.0, 90.0))
    }

    @Test fun berlawananArahPintuKiri() {
        assertEquals("Pintu kiri", DoorSideHint.hint(0.0, 180.0))
    }

    @Test fun selisihTepat270PintuKiri() {
        assertEquals("Pintu kiri", DoorSideHint.hint(0.0, 270.0))
    }

    @Test fun selisihSedikitDiAtas270PintuKanan() {
        assertEquals("Pintu kanan", DoorSideHint.hint(0.0, 270.1))
    }

    @Test fun putaranSudutDinormalisasi() {
        // 350° -> 10° = selisih 20° -> bus searah halte.
        assertEquals("Pintu kanan", DoorSideHint.hint(350.0, 10.0))
        // 10° -> 350° = selisih 340° (> 270) -> bus searah halte.
        assertEquals("Pintu kanan", DoorSideHint.hint(10.0, 350.0))
    }

    @Test fun sudutNegatifDinormalisasi() {
        assertEquals("Pintu kanan", DoorSideHint.hint(-90.0, -90.0))
        assertEquals("Pintu kiri", DoorSideHint.hint(-90.0, 90.0))
    }
}
