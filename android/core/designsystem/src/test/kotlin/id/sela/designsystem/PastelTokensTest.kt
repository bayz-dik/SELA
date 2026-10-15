package id.sela.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class PastelTokensTest {
    @Test fun backgroundIsWarmOffWhite() {
        assertEquals(Color(0xFFFDFBF7), SelaPastel.background)
        assertEquals(SelaPastel.background, SelaPastelScheme.background)
    }

    @Test fun inkIsDarkEnoughForText() {
        assertTrue(SelaPastel.ink.luminance() < 0.05f)
        assertTrue(SelaPastel.inkSoft.luminance() < 0.15f)
    }

    @Test fun corePastelsAreDistinct() {
        val core = listOf(
            SelaPastel.blue, SelaPastel.lavender, SelaPastel.mint,
            SelaPastel.purple, SelaPastel.pink, SelaPastel.peach, SelaPastel.sunny,
        )
        assertEquals(core.size, core.toSet().size)
    }

    @Test fun serviceColorsStayDistinct() {
        val services = listOf(SelaPastel.brt, SelaPastel.jakLingko, SelaPastel.bus)
        assertEquals(3, services.toSet().size)
    }

    @Test fun deepVariantsAreDarkerThanPastels() {
        assertTrue(SelaPastel.blueDeep.luminance() < SelaPastel.blue.luminance())
        assertTrue(SelaPastel.mintDeep.luminance() < SelaPastel.mint.luminance())
        assertTrue(SelaPastel.lavenderDeep.luminance() < SelaPastel.lavender.luminance())
    }

    @Test fun shadowsGrowInSteps() {
        assertTrue(SelaPastelShadows.soft < SelaPastelShadows.card)
        assertTrue(SelaPastelShadows.card < SelaPastelShadows.lifted)
        assertTrue(SelaPastelShadows.lifted < SelaPastelShadows.floating)
    }

    @Test fun motionDurationsAscend() {
        assertTrue(SelaMotion.instant < SelaMotion.quick)
        assertTrue(SelaMotion.quick < SelaMotion.standard)
        assertTrue(SelaMotion.standard < SelaMotion.relaxed)
        assertTrue(SelaMotion.relaxed < SelaMotion.playful)
    }

    @Test fun pressScaleIsSubtle() {
        assertTrue(SelaMotion.pressScaleDown in 0.90f..0.98f)
    }

    @Test fun displayTypeIsLargest() {
        assertTrue(
            SelaPastelTypography.displayLarge.fontSize > SelaPastelTypography.headlineLarge.fontSize,
        )
        assertTrue(
            SelaPastelTypography.headlineLarge.fontSize > SelaPastelTypography.titleLarge.fontSize,
        )
        assertTrue(SelaPastelTypography.titleLarge.fontSize > SelaPastelTypography.bodyLarge.fontSize)
    }

    @Test fun auroraGradientsExist() {
        assertNotNull(SelaPastelGradients.auroraBlueLavender)
        assertNotNull(SelaPastelGradients.auroraMintCream)
        assertNotNull(SelaPastelGradients.auroraPurplePink)
    }

    @Test fun mascotHasThreeExpressions() {
        assertEquals(3, SelaMascotExpression.entries.size)
    }
}
