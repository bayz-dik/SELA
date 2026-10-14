package id.sela.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Uji unit untuk logika murni [SelaAiLogic.parseAnswer] dan
 * [SelaAiLogic.deriveAccountId] — murni JVM, tanpa Android.
 */
class SelaAiClientTest {

    private fun jwtWith(payloadJson: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload = enc.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.sig"
    }

    @Test fun deriveAccountId_dariAuthClaim() {
        val jwt = jwtWith(
            """{"https://api.openai.com/auth":{"chatgpt_account_id":"acct_123"}}""",
        )
        assertEquals("acct_123", SelaAiLogic.deriveAccountId(jwt))
    }

    @Test fun deriveAccountId_dariTopLevel() {
        val jwt = jwtWith("""{"chatgpt_account_id":"acct_456"}""")
        assertEquals("acct_456", SelaAiLogic.deriveAccountId(jwt))
    }

    @Test fun deriveAccountId_dariOrganizations() {
        val jwt = jwtWith("""{"organizations":[{"id":"org_789"}]}""")
        assertEquals("org_789", SelaAiLogic.deriveAccountId(jwt))
    }

    @Test fun deriveAccountId_kosongJikaTidakAda() {
        val jwt = jwtWith("""{"sub":"user_1"}""")
        assertEquals("", SelaAiLogic.deriveAccountId(jwt))
    }

    @Test fun deriveAccountId_tokenRusak() {
        assertEquals("", SelaAiLogic.deriveAccountId("bukan-jwt"))
    }

    @Test fun parseAnswer_responsesApi() {
        val json = """
        {"id":"resp_1","output":[
          {"type":"message","role":"assistant","content":[
            {"type":"output_text","text":"Bus 1A sekitar 300 meter lagi."}
          ]}
        ]}
        """.trimIndent()
        assertEquals(
            "Bus 1A sekitar 300 meter lagi.",
            SelaAiLogic.parseAnswer(json),
        )
    }

    @Test fun parseAnswer_gabunganMultiPart() {
        val json = """
        {"output":[
          {"type":"reasoning","content":[]},
          {"type":"message","content":[
            {"type":"output_text","text":"Halo."},
            {"type":"output_text","text":"Ada yang bisa dibantu?"}
          ]}
        ]}
        """.trimIndent()
        val out = SelaAiLogic.parseAnswer(json) ?: ""
        assertTrue("Halo." in out)
        assertTrue("Ada yang bisa dibantu?" in out)
    }

    @Test fun parseAnswer_kosongJikaTidakAdaTeks() {
        val json = """{"output":[{"type":"message","content":[]}]}"""
        assertNull(SelaAiLogic.parseAnswer(json))
    }

    @Test fun parseAnswer_jsonRusak() {
        assertNull(SelaAiLogic.parseAnswer("bukan json"))
    }
}
