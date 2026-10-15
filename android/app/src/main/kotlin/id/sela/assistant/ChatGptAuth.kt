package id.sela.assistant

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Login ChatGPT via OAuth — alur "Sign in with ChatGPT" resmi untuk open-source
 * (dynamic client registration, lih. https://developers.openai.com/siwc/).
 *
 * RIWAYAT: implementasi lama memakai client_id statis
 * `app_EMoamEEZ73f0CkXaXp7hrann` ke `https://auth.openai.com/oauth/authorize`
 * dengan redirect `sela://oauth/callback`. Sejak ~2026, OpenAI menolak semua
 * request authorize untuk client itu (HTTP 400 `invalid_authorize_request`).
 * Alur itu SUDAH MATI dan tidak bisa diperbaiki dari sisi kita.
 *
 * Alur baru (terverifikasi 2026-10-15):
 * - Authorize: `https://auth.openai.com/api/accounts/authorize`
 *   dengan `client_id=dynamic_agent_client` → OpenAI menerbitkan client_id
 *   per-akun (`oaiapp_...`) saat callback pertama.
 * - Redirect: loopback `http://127.0.0.1:<port>/auth/callback`
 *   (127.0.0.1, bukan localhost, bukan custom scheme).
 * - Token: `https://auth.openai.com/api/accounts/oauth/token`
 * - Token yang didapat berlaku untuk `https://api.openai.com/v1`
 *   (chat completions standar).
 *
 * Token disimpan di EncryptedSharedPreferences, tidak pernah di-log.
 */
object ChatGptAuth {
    // Endpoint resmi dari https://auth.openai.com/.well-known/openid-configuration
    private const val AUTHORIZE_URL = "https://auth.openai.com/api/accounts/authorize"
    private const val TOKEN_URL = "https://auth.openai.com/api/accounts/oauth/token"
    private const val RESOURCE = "https://api.openai.com/v1"

    // Dynamic client registration — client_id ini memicu OpenAI menerbitkan
    // client_id per-akun saat callback pertama.
    private const val DYNAMIC_CLIENT_ID = "dynamic_agent_client"
    private const val AGENT_NAME_HINT = "sela"
    private const val SCOPE =
        "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct"

    // Batas tunggu user menyelesaikan login di browser: 5 menit.
    private const val LOGIN_TIMEOUT_MS = 5 * 60 * 1000L

    private const val PREFS_NAME = "sela_chatgpt_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_ID_TOKEN = "id_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_ACCOUNT_ID = "account_id"
    private const val KEY_CLIENT_ID = "issued_client_id"
    private const val KEY_HOST_ID = "ext_agent_host_id"

    /** Status login untuk UI. */
    sealed interface AuthState {
        data object LoggedOut : AuthState
        data object WaitingBrowser : AuthState
        data object LoggedIn : AuthState
        data class Failed(val message: String) : AuthState
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

    /** ID host permanen per perangkat untuk dynamic registration. */
    private fun extAgentHostId(): String {
        val p = prefs()
        val existing = p.getString(KEY_HOST_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val fresh = "urn:uuid:${UUID.randomUUID()}"
        p.edit().putString(KEY_HOST_ID, fresh).apply()
        return fresh
    }

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
     * Mulai login: buka loopback server, lalu buka browser ke halaman OAuth.
     * Callback ditangkap langsung oleh server lokal (bukan via Activity).
     * @return true jika browser berhasil dibuka.
     */
    fun beginLogin(context: Context): Boolean {
        // Cegah login ganda.
        if (_authState.value is AuthState.WaitingBrowser) return true
        _authState.value = AuthState.WaitingBrowser
        scope.launch {
            val ctx = context.applicationContext
            var server: ServerSocket? = null
            try {
                // Semua I/O (termasuk EncryptedSharedPreferences) di thread background.
                data class LoginSetup(
                    val server: ServerSocket,
                    val redirectUri: String,
                    val hostId: String,
                )
                val setup = withContext(Dispatchers.IO) {
                    val s = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
                    s.soTimeout = LOGIN_TIMEOUT_MS.toInt()
                    LoginSetup(
                        server = s,
                        redirectUri = "http://127.0.0.1:${s.localPort}/auth/callback",
                        hostId = extAgentHostId(),
                    )
                }
                server = setup.server
                val redirectUri = setup.redirectUri
                val verifier = randomUrlSafe(48)
                val state = randomUrlSafe(24)
                val nonce = randomUrlSafe(24)

                val url = Uri.parse(AUTHORIZE_URL).buildUpon()
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("client_id", DYNAMIC_CLIENT_ID)
                    .appendQueryParameter("agent_name_hint", AGENT_NAME_HINT)
                    .appendQueryParameter("ext_agent_host_id", setup.hostId)
                    .appendQueryParameter("redirect_uri", redirectUri)
                    .appendQueryParameter("scope", SCOPE)
                    .appendQueryParameter("resource", RESOURCE)
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("nonce", nonce)
                    .appendQueryParameter("code_challenge", codeChallenge(verifier))
                    .appendQueryParameter("code_challenge_method", "S256")
                    .build()
                    .toString()

                CustomTabsIntent.Builder().build().launchUrl(ctx, Uri.parse(url))

                // Tunggu callback di background; user punya 5 menit.
                // soTimeout pada server membuat accept() melempar
                // SocketTimeoutException saat waktu habis (tidak bocor thread).
                val result = withContext(Dispatchers.IO) {
                    runCatching { waitForCallback(setup.server, state) }.getOrNull()
                }
                runCatching { setup.server.close() }
                if (result == null) {
                    _authState.value = AuthState.Failed("Waktu login habis. Coba lagi ya.")
                    return@launch
                }
                if (result.error != null) {
                    _authState.value = AuthState.Failed("Login dibatalkan (${result.error}).")
                    return@launch
                }
                val code = result.code
                val issuedClientId = result.issuedClientId
                if (code.isNullOrBlank() || issuedClientId.isNullOrBlank()) {
                    _authState.value = AuthState.Failed("Callback login tidak valid. Coba lagi.")
                    return@launch
                }
                val tokens = withContext(Dispatchers.IO) {
                    exchangeCode(code, verifier, redirectUri, issuedClientId, nonce)
                }
                if (tokens == null) {
                    _authState.value = AuthState.Failed("Gagal menukar kode login. Coba lagi.")
                    return@launch
                }
                withContext(Dispatchers.IO) { saveTokens(tokens, issuedClientId) }
                _authState.value = AuthState.LoggedIn
            } catch (_: Exception) {
                runCatching { server?.close() }
                _authState.value = AuthState.Failed("Gagal membuka browser. Coba lagi ya.")
            }
        }
        return true
    }

    private data class CallbackResult(
        val code: String?,
        val stateOk: Boolean,
        val issuedClientId: String?,
        val error: String?,
    )

    /**
     * Tunggu satu request ke /auth/callback di loopback server.
     * Mengembalikan hasil parsing query + menutup koneksi dengan halaman sukses.
     */
    private fun waitForCallback(server: ServerSocket, expectedState: String): CallbackResult {
        val socket: Socket = server.accept()
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val requestLine = reader.readLine().orEmpty()
            // Buang sisa header.
            var line: String?
            do {
                line = reader.readLine()
            } while (line != null && line.isNotEmpty())

            // Contoh: GET /auth/callback?code=...&state=...&client_id=oaiapp_... HTTP/1.1
            val path = requestLine.substringAfter(" ").substringBefore(" ")
            val query = Uri.parse("http://x$path")

            // Balas browser dengan halaman sukses sederhana.
            val html = "<html><body style='font-family:sans-serif;text-align:center;" +
                "padding-top:40px'><h2>Login SELA berhasil ✅</h2>" +
                "<p>Kembali ke aplikasi SELA ya.</p></body></html>"
            val body = html.toByteArray(Charsets.UTF_8)
            val out = s.getOutputStream()
            out.write(
                ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n")
                    .toByteArray(Charsets.US_ASCII),
            )
            out.write(body)
            out.flush()

            val err = query.getQueryParameter("error")
            if (!err.isNullOrBlank()) {
                return CallbackResult(null, false, null, err)
            }
            return CallbackResult(
                code = query.getQueryParameter("code"),
                stateOk = query.getQueryParameter("state") == expectedState,
                // OpenAI menerbitkan client_id per-akun di callback ini.
                issuedClientId = query.getQueryParameter("client_id"),
                error = null,
            ).let {
                if (!it.stateOk) it.copy(code = null, issuedClientId = null) else it
            }
        }
    }

    /**
     * Tangani redirect kustom lama (sela://oauth/callback).
     * Alur lama sudah mati di sisi OpenAI; method ini dipertahankan agar
     * [OAuthCallbackActivity] tidak crash bila terpanggil.
     */
    suspend fun handleCallback(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        _authState.value =
            AuthState.Failed("Alur login lama sudah tidak berlaku. Pakai tombol login ya.")
        false
    }

    private data class Tokens(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val expiresAtMs: Long,
    )

    /** Tukar authorization code jadi token (form-encoded, tanpa secret). */
    private fun exchangeCode(
        code: String,
        verifier: String,
        redirectUri: String,
        issuedClientId: String,
        nonce: String,
    ): Tokens? {
        return try {
            val body = listOf(
                "grant_type" to "authorization_code",
                "client_id" to issuedClientId,
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to redirectUri,
                "resource" to RESOURCE,
            ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
            val json = postForm(TOKEN_URL, body) ?: return null
            val tokens = tokensFromJson(json) ?: return null
            // Validasi ringan ID token: aud harus = issued client_id, belum expired,
            // nonce cocok. (Verifikasi signature penuh via JWKS = hardening lanjutan.)
            if (!validateIdToken(tokens.idToken, issuedClientId, nonce)) return null
            tokens
        } catch (_: Exception) {
            null
        }
    }

    /** Refresh access token memakai refresh token. */
    private fun refreshTokens(refreshToken: String, issuedClientId: String): Tokens? {
        return try {
            val body = listOf(
                "grant_type" to "refresh_token",
                "client_id" to issuedClientId,
                "refresh_token" to refreshToken,
                "resource" to RESOURCE,
            ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
            val json = postForm(TOKEN_URL, body) ?: return null
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

    /**
     * Validasi ringan klaim ID token (tanpa verifikasi signature).
     * Memastikan token memang diterbitkan untuk client_id kita.
     */
    private fun validateIdToken(idToken: String?, expectedAud: String, expectedNonce: String): Boolean {
        if (idToken.isNullOrBlank()) return false
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return false
            val payloadJson = JSONObject(
                String(
                    Base64.getUrlDecoder().decode(parts[1].padBase64()),
                    Charsets.UTF_8,
                ),
            )
            val aud = payloadJson.optString("aud")
            val exp = payloadJson.optLong("exp", 0)
            val nonce = payloadJson.optString("nonce")
            val nowSec = System.currentTimeMillis() / 1000
            aud == expectedAud && exp > nowSec && (nonce.isBlank() || nonce == expectedNonce)
        } catch (_: Exception) {
            false
        }
    }

    private fun String.padBase64(): String {
        var s = this
        while (s.length % 4 != 0) s += "="
        return s
    }

    private fun saveTokens(t: Tokens, issuedClientId: String) {
        val accountId = t.idToken?.let(::deriveAccountId).orEmpty()
        val editor = prefs().edit()
            .putString(KEY_ACCESS, t.accessToken)
            .putString(KEY_ID_TOKEN, t.idToken)
            .putString(KEY_ACCOUNT_ID, accountId)
            .putString(KEY_CLIENT_ID, issuedClientId)
            .putLong(KEY_EXPIRES_AT, t.expiresAtMs)
        // Jangan timpa refresh token dengan nilai kosong (rotasi bisa kosong sesekali)
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
        val issuedClientId = saved.getString(KEY_CLIENT_ID, null)
        if (refresh.isNullOrBlank() || issuedClientId.isNullOrBlank()) {
            withContext(Dispatchers.Main) { _authState.value = AuthState.LoggedOut }
            return@withContext null
        }
        val tokens = refreshTokens(refresh, issuedClientId)
        if (tokens == null) {
            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Failed("Sesi kedaluwarsa. Login ulang ya.")
            }
            return@withContext null
        }
        saveTokens(tokens, issuedClientId)
        tokens.accessToken
    }

    fun getAccountId(): String? =
        loadPrefs()?.getString(KEY_ACCOUNT_ID, null)?.takeIf { it.isNotBlank() }

    /** Keluar: hapus semua token (client_id & host_id dipertahankan). */
    fun logout() {
        runCatching {
            prefs().edit()
                .remove(KEY_ACCESS)
                .remove(KEY_REFRESH)
                .remove(KEY_ID_TOKEN)
                .remove(KEY_EXPIRES_AT)
                .remove(KEY_ACCOUNT_ID)
                .apply()
        }
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

    private fun readJson(conn: HttpURLConnection): JSONObject? {
        val code = conn.responseCode
        if (code !in 200..299) return null
        val text = conn.inputStream.bufferedReader().readText()
        return runCatching { JSONObject(text) }.getOrNull()
    }

    // ---------- JWT ----------

    /**
     * Ambil chatgpt_account_id dari klaim JWT.
     * Delegasi ke [SelaAiLogic] agar bisa di-unit-test.
     */
    fun deriveAccountId(jwt: String): String = SelaAiLogic.deriveAccountId(jwt)
}
