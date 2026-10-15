package id.sela.assistant

import org.json.JSONObject
import java.util.Base64

/**
 * Logika murni "Tanya Sela" — tanpa import Android sehingga bisa di-unit-test
 * di JVM biasa.
 */
object SelaAiLogic {
    /**
     * Ambil chatgpt_account_id dari klaim JWT
     * (https://api.openai.com/auth → chatgpt_account_id).
     */
    fun deriveAccountId(jwt: String): String {
        return runCatching {
            val parts = jwt.split(".")
            if (parts.size < 2) return ""
            val payload = String(
                Base64.getUrlDecoder().decode(parts[1]),
                Charsets.UTF_8,
            )
            val json = JSONObject(payload)
            val authClaim = json.optJSONObject("https://api.openai.com/auth")
            val fromClaim = authClaim?.optString("chatgpt_account_id").orEmpty()
            if (fromClaim.isNotBlank()) return fromClaim
            val topLevel = json.optString("chatgpt_account_id")
            if (topLevel.isNotBlank()) return topLevel
            val orgs = json.optJSONArray("organizations")
            if (orgs != null && orgs.length() > 0) {
                val id = orgs.optJSONObject(0)?.optString("id").orEmpty()
                if (id.isNotBlank()) return id
            }
            ""
        }.getOrDefault("")
    }

    /**
     * Ambil teks jawaban. Format utama: OpenAI Chat Completions API
     * (choices[0].message.content). Format Responses API lama
     * (output[] -> content[] -> output_text) tetap didukung sebagai fallback.
     */
    fun parseAnswer(jsonText: String): String? {
        return runCatching {
            val root = JSONObject(jsonText)
            if (!root.isNull("error")) {
                val err = root.optJSONObject("error")
                val msg = err?.optString("message").orEmpty()
                return if (msg.isNotBlank()) "AI error: $msg" else null
            }
            // Format Chat Completions: choices[0].message.content
            val choices = root.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val content = choices.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                if (content.isNotBlank()) return content.trim()
            }
            // Fallback: format Responses API lama
            val output = root.optJSONArray("output") ?: return null
            val sb = StringBuilder()
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                if (item.optString("type") != "message") continue
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val part = content.optJSONObject(j) ?: continue
                    if (part.optString("type") == "output_text") {
                        val t = part.optString("text")
                        if (t.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(t)
                        }
                    }
                }
            }
            sb.toString().takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
