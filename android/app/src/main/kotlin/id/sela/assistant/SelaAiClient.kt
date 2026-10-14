package id.sela.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klien AI "Tanya Sela" — memanggil Codex Responses API milik akun ChatGPT
 * pengguna langsung dari HP (tanpa relay, tanpa Telegram).
 *
 * Endpoint & header mengikuti https://github.com/EvanZhouDev/openai-oauth
 * (tidak resmi / komunitas).
 */
object SelaAiClient {
    private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"

    /**
     * Daftar model Codex yang didukung (sesuai katalog akun ChatGPT Plus).
     * Urutan mengikuti preferensi Bayu.
     */
    val MODELS = listOf(
        "gpt-6-astra",
        "gpt-6.1-sol",
        "gpt-6-sol",
        "gpt-6-luna",
        "gpt-5.6-sol",
        "gpt-5.6-terra",
        "gpt-5.6-luna",
        "gpt-5.5",
    )

    /** Model default; ganti bila model tidak tersedia untuk akun. */
    const val DEFAULT_MODEL = "gpt-6-astra"

    /** Hasil pemanggilan AI. */
    sealed interface AiResult {
        data class Ok(val text: String) : AiResult
        /** Perlu login (ulang). */
        data object NeedLogin : AiResult
        data class Error(val message: String) : AiResult
    }

    /**
     * Tanya ke AI. Mengurus token (termasuk refresh) via [ChatGptAuth].
     */
    suspend fun ask(question: String, model: String = DEFAULT_MODEL): AiResult =
        withContext(Dispatchers.IO) {
            val token = ChatGptAuth.getAccessToken()
                ?: return@withContext AiResult.NeedLogin
            val accountId = ChatGptAuth.getAccountId().orEmpty()

            val body = JSONObject()
                .put("model", model)
                .put("stream", false)
                .put(
                    "input",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "content",
                                "Kamu asisten app SELA (info bus TransJakarta Jakarta). " +
                                    "Jawab singkat, jelas, Bahasa Indonesia santai. " +
                                    "Pertanyaan: $question",
                            ),
                    ),
                )
                .toString()

            var conn: HttpURLConnection? = null
            try {
                conn = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20000
                    readTimeout = 90000
                    setRequestProperty("Content-Type", "application/json")
                    // Jangan pernah log nilai header ini
                    setRequestProperty("Authorization", "Bearer $token")
                    if (accountId.isNotBlank()) {
                        setRequestProperty("chatgpt-account-id", accountId)
                    }
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code == 401 || code == 403) {
                    return@withContext AiResult.NeedLogin
                }
                if (code !in 200..299) {
                    return@withContext AiResult.Error(
                        "Server AI sibuk (HTTP $code). Coba lagi ya.",
                    )
                }
                val text = conn.inputStream.bufferedReader().readText()
                val answer = parseAnswer(text)
                if (answer.isNullOrBlank()) {
                    AiResult.Error("AI tidak memberi jawaban. Coba tanya lain.")
                } else {
                    AiResult.Ok(answer)
                }
            } catch (e: java.net.SocketTimeoutException) {
                AiResult.Error("Koneksi lambat / timeout. Coba lagi ya.")
            } catch (_: java.io.IOException) {
                AiResult.Error("Tidak ada internet. Cek koneksi kamu.")
            } catch (_: Exception) {
                AiResult.Error("Gagal menghubungi AI. Coba lagi ya.")
            } finally {
                conn?.disconnect()
            }
        }

    /**
     * Ambil teks jawaban dari format OpenAI Responses API.
     * Delegasi ke [SelaAiLogic] agar bisa di-unit-test.
     */
    fun parseAnswer(jsonText: String): String? = SelaAiLogic.parseAnswer(jsonText)
}
