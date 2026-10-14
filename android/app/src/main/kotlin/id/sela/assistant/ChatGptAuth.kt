package id.sela.assistant

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Login ChatGPT via OAuth (aliran PKCE) — implementasi Kotlin dari
 * https://github.com/EvanZhouDev/openai-oauth (tidak resmi / komunitas).
 *
 * Token disimpan di EncryptedSharedPreferences, tidak pernah di-log.
 */
object ChatGptAuth {
    // Konstanta dari openai-oauth (packages/core/src/runtime.ts)
    private const val ISSUER = "https://auth.openai.com"
    private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    private const val SCOPE = "openid profile email offline_access"
    const val REDIRECT_URI = "sela://oauth/callback"

    private const val PREFS_NAME = "sela_chatgpt_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_ID_TOKEN = "id_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_ACCOUNT_ID = "account_id"
    private const val KEY_VERIFIER = "pending_verifier"
    private const val KEY_STATE = "pending_state"

    /** Status login untuk UI. */
    sealed interface AuthState {
        data object LoggedOut : AuthState
        data object WaitingBrowser : AuthState
        data object LoggedIn : AuthState
        data class Failed(val message: String) : AuthState
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    /** Dipanggil sekali dari Application. */
    fun init(context: Context) {
        appContext = context.applicationContext
        _authState.value =
            if (loadPrefs()?.getString(KEY_REFRESH, null).isNullOrBlank()) {
                AuthState.LoggedOut
            } else {
                AuthState.LoggedIn
            }
    }

    val isLoggedIn: Boolean
        get() = _authState.value is AuthState.LoggedIn

    private fun prefs(): SharedPreferences {
        val ctx = appContext
            ?: throw IllegalStateException("ChatGptAuth belum di-init")
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            ctx,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun loadPrefs(): SharedPreferences? = runCatching { prefs() }.getOrNull()

    // ---------- PKCE ----------

    private fun randomUrlSafe(nBytes: Int): String {
        val bytes = ByteArray(nBytes)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Mulai login: buka browser ke halaman OAuth ChatGPT.
     * @return true jika browser berhasil dibuka.
     */
    fun beginLogin(context: Context): Boolean {
        val verifier = randomUrlSafe(48)
        val state = randomUrlSafe(24)
        runCatching {
            prefs().edit()
                .putString(KEY_VERIFIER, verifier)
                .putString(KEY_STATE, state)
                .apply()
        }

        val url = Uri.parse("$ISSUER/oauth/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()

        return runCatching {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
            _authState.value = AuthState.WaitingBrowser
            true
        }.getOrDefault(false)
    }

    /**
     * Tangani redirect kembali dari browser. Dipanggil oleh OAuthCallbackActivity.
     * @return true jika login berhasil.
     */
    suspend fun handleCallback(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val err = uri.getQueryParameter("error")
        if (!err.isNullOrBlank()) {
            _authState.value = AuthState.Failed("Login dibatalkan ($err).")
            return@withContext false
        }
        val saved = loadPrefs() ?: run {
            _authState.value = AuthState.Failed("Penyimpanan tidak tersedia.")
            return@withContext false
        }
        val verifier = saved.getString(KEY_VERIFIER, null)
        val savedState = saved.getString(KEY_STATE, null)
        if (code.isNullOrBlank() || verifier.isNullOrBlank() || savedState.isNullOrBlank()
            || state != savedState
        ) {
            _authState.value = AuthState.Failed("Kode login tidak valid. Coba lagi.")
            return@withContext false
        }
        // Hapus verifier sekali pakai
        saved.edit().remove(KEY_VERIFIER).remove(KEY_STATE).apply()

        val tokens = exchangeCode(code, verifier)
        if (tokens == null) {
            _authState.value = AuthState.Failed("Gagal menukar kode login. Coba lagi.")
            return@withContext false
        }
        saveTokens(tokens)
        _authState.value = AuthState.LoggedIn
        true
    }

    private data class Tokens(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val expiresAtMs: Long,
    )

    /** Tukar authorization code jadi token (form-encoded, sesuai openai-oauth). */
    private fun exchangeCode(code: String, verifier: String): Tokens? {
        return try {
            val body = listOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to REDIRECT_URI,
                "client_id" to CLIENT_ID,
                "code_verifier" to verifier,
            ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
            val json = postForm("$ISSUER/oauth/token", body) ?: return null
            tokensFromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    /** Refresh access token memakai refresh token (JSON, sesuai openai-oauth). */
    private fun refreshTokens(refreshToken: String): Tokens? {
        return try {
            val body = JSONObject()
                .put("grant_type", "refresh_token")
                .put("refresh_token", refreshToken)
                .put("client_id", CLIENT_ID)
                .toString()
            val json = postJson("$ISSUER/oauth/token", body) ?: return null
            tokensFromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun tokensFromJson(json: JSONObject): Tokens? {
        val access = json.optString("access_token").takeIf { it.isNotBlank() } ?: return null
        val refresh = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val idToken = json.optString("id_token").takeIf { it.isNotBlank() }
        val expiresIn = json.optLong("expires_in", 3600)
        // Simpan sedikit lebih awal dari kedaluwarsa agar aman
        val expiresAt = System.currentTimeMillis() + (expiresIn - 60).coerceAtLeast(60) * 1000
        return Tokens(access, refresh, idToken, expiresAt)
    }

    private fun saveTokens(t: Tokens) {
        val accountId = t.idToken?.let(::deriveAccountId).orEmpty()
        val editor = prefs().edit()
            .putString(KEY_ACCESS, t.accessToken)
            .putString(KEY_ID_TOKEN, t.idToken)
            .putString(KEY_ACCOUNT_ID, accountId)
            .putLong(KEY_EXPIRES_AT, t.expiresAtMs)
        // Jangan timpa refresh token dengan nilai kosong
        if (!t.refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH, t.refreshToken)
        }
        editor.apply()
    }

    /** Ambil access token yang masih berlaku; refresh otomatis bila perlu. */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val saved = loadPrefs() ?: return@withContext null
        val access = saved.getString(KEY_ACCESS, null)
        val expiresAt = saved.getLong(KEY_EXPIRES_AT, 0)
        if (!access.isNullOrBlank() && System.currentTimeMillis() < expiresAt) {
            return@withContext access
        }
        val refresh = saved.getString(KEY_REFRESH, null)
        if (refresh.isNullOrBlank()) {
            withContext(Dispatchers.Main) { _authState.value = AuthState.LoggedOut }
            return@withContext null
        }
        val tokens = refreshTokens(refresh)
        if (tokens == null) {
            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Failed("Sesi kedaluwarsa. Login ulang ya.")
            }
            return@withContext null
        }
        saveTokens(tokens)
        tokens.accessToken
    }

    fun getAccountId(): String? =
        loadPrefs()?.getString(KEY_ACCOUNT_ID, null)?.takeIf { it.isNotBlank() }

    /** Keluar: hapus semua token. */
    fun logout() {
        runCatching { prefs().edit().clear().apply() }
        _authState.value = AuthState.LoggedOut
    }

    // ---------- HTTP helpers (tanpa token di log) ----------

    private fun postForm(url: String, body: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            readJson(conn)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun postJson(url: String, body: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            readJson(conn)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun readJson(conn: HttpURLConnection): JSONObject? {
        val code = conn.responseCode
        if (code !in 200..299) return null
        val text = conn.inputStream.bufferedReader().readText()
        return runCatching { JSONObject(text) }.getOrNull()
    }

    // ---------- JWT ----------

    /**
     * Ambil chatgpt_account_id dari klaim JWT
     * (https://api.openai.com/auth → chatgpt_account_id).
     * Delegasi ke [SelaAiLogic] agar bisa di-unit-test.
     */
    fun deriveAccountId(jwt: String): String = SelaAiLogic.deriveAccountId(jwt)
}
