package id.sela.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klien AI "Tanya Sela" — memanggil OpenAI API standar
 * (`https://api.openai.com/v1/chat/completions`) memakai token hasil
 * "Sign in with ChatGPT" (dynamic client registration).
 *
 * Token berasal dari akun ChatGPT pengguna sendiri (gratis/Plus/Pro),
 * jadi tidak perlu API key berbayar.
 */
object SelaAiClient {
    private const val CHAT_URL = "https://api.openai.com/v1/chat/completions"

    /**
     * Daftar model yang bisa dipilih di dalam chat.
     * Token plan ChatGPT bisa mengakses model-model standar OpenAI;
     * bila satu model tidak tersedia untuk akun, API mengembalikan error
     * yang jelas dan pengguna bisa pilih model lain.
     */
    val MODELS = listOf(
        "gpt-4o-mini",
        "gpt-4o",
        "gpt-4.1-mini",
        "gpt-4.1",
    )

    /** Model default: murah, cepat, tersedia luas. */
    const val DEFAULT_MODEL = "gpt-4o-mini"

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

            val body = JSONObject()
                .put("model", model)
                .put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("role", "system")
                                .put(
                                    "content",
                                    "Kamu asisten app SELA (info bus TransJakarta Jakarta). " +
                                        "Jawab singkat, jelas, Bahasa Indonesia santai.",
                                ),
                        )
                        .put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", question),
                        ),
                )
                .put("max_tokens", 800)
                .toString()

            var conn: HttpURLConnection? = null
            try {
                conn = (URL(CHAT_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20000
                    readTimeout = 90000
                    setRequestProperty("Content-Type", "application/json")
                    // Jangan pernah log nilai header ini
                    setRequestProperty("Authorization", "Bearer $token")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code == 401 || code == 403) {
                    return@withContext AiResult.NeedLogin
                }
                if (code == 404) {
                    return@withContext AiResult.Error(
                        "Model $model tidak tersedia untuk akunmu. Pilih model lain ya.",
                    )
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
     * Ambil teks jawaban dari format OpenAI Chat Completions API
     * (`choices[0].message.content`). Format Responses API lama tetap
     * didukung sebagai fallback.
     * Delegasi ke [SelaAiLogic] agar bisa di-unit-test.
     */
    fun parseAnswer(jsonText: String): String? = SelaAiLogic.parseAnswer(jsonText)
}
