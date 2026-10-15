package id.sela.designsystem
import androidx.compose.ui.unit.dp
import id.sela.model.ServiceType
import org.junit.Assert.*
import org.junit.Test
class SelaTokensTest {
    @Test fun serviceLabelsAreIndonesian() {
        assertEquals(listOf("BRT", "JakLingko", "Bus Biasa"), ServiceType.entries.map { it.label() })
    }
    @Test fun eachServiceHasDistinctColor() {
        assertEquals(3, ServiceType.entries.map { it.color() }.toSet().size)
    }
    @Test fun spacingUsesFourAndEightDp() {
        assertEquals(4.dp, SelaTokens.spaceSmall); assertEquals(8.dp, SelaTokens.spaceMedium)
    }
}
