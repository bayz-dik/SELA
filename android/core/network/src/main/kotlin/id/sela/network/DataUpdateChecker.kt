package id.sela.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pemeriksa pembaruan data GTFS (fitur #1).
 *
 * Cara kerja: APK membawa data bawaan (assets/gtfs). Jika pengguna mengisi URL
 * sumber pembaruan di Pengaturan, checker mengunduh `version.json` dari URL itu
 * (`{"version":"...","files":["routes.json","stops-0.json",...]}`). Kalau versi
 * berbeda dari yang tersimpan, file-file diunduh ke files/gtfs-update dan
 * [LocalGtfsRepository] otomatis memakainya (override atas assets).
 *
 * Default: URL kosong → pembaruan otomatis MATI, app 100% pakai data bawaan.
 * Tidak ada data pengguna yang dikirim ke mana pun; ini hanya unduhan HTTP biasa.
 */
@Singleton
class DataUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: TransitPreferences,
    private val localGtfs: LocalGtfsRepository,
    @TransitScope private val scope: CoroutineScope,
) {
    private val gson = Gson()

    data class CheckResult(
        val checked: Boolean,
        val updated: Boolean,
        val version: String?,
        val message: String,
    )

    /** Dipanggil saat aplikasi dibuka; diam-diam, tidak mengganggu UI. */
    fun checkInBackground() {
        scope.launch { checkNow() }
    }

    suspend fun checkNow(): CheckResult = withContext(Dispatchers.IO) {
        val base = preferences.gtfsUpdateUrl.first().trim().trimEnd('/')
        if (base.isEmpty()) {
            return@withContext CheckResult(false, false, null, "URL pembaruan belum diisi — memakai data bawaan.")
        }
        try {
            val manifest = downloadJson("$base/version.json")
            val version = manifest.get("version")?.asString?.takeIf { it.isNotBlank() }
                ?: return@withContext CheckResult(true, false, null, "version.json tidak valid.")
            val current = preferences.gtfsVersion.first()
            if (version == current && localGtfs.hasUpdate()) {
                return@withContext CheckResult(true, false, version, "Data sudah versi terbaru ($version).")
            }
            val files = manifest.getAsJsonArray("files")?.map { it.asString } ?: listOf("routes.json")
            val payload = mutableMapOf<String, String>()
            for (name in files) {
                if (!name.matches(Regex("^(routes|stops-\\d+)\\.json$"))) continue
                payload[name] = downloadText("$base/$name")
            }
            if (payload.isEmpty()) {
                return@withContext CheckResult(true, false, version, "Tidak ada file valid yang bisa diunduh.")
            }
            localGtfs.applyUpdate(payload)
            preferences.setGtfsVersion(version, System.currentTimeMillis())
            CheckResult(true, true, version, "Data diperbarui ke versi $version (${payload.size} file).")
        } catch (e: Exception) {
            CheckResult(true, false, null, "Gagal memeriksa pembaruan: ${e.message ?: e.javaClass.simpleName}. Data lama tetap dipakai.")
        }
    }

    suspend fun resetToBundled(): CheckResult = withContext(Dispatchers.IO) {
        localGtfs.resetToBundled()
        preferences.setGtfsVersion("bundled-2026-10-14", System.currentTimeMillis())
        CheckResult(true, true, "bundled-2026-10-14", "Kembali memakai data bawaan APK.")
    }

    private fun downloadText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "SELA-Android/0.4.0")
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            if (text.length > 5_000_000) throw IllegalStateException("File terlalu besar")
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadJson(url: String): JsonObject =
        gson.fromJson(downloadText(url), JsonObject::class.java)
            ?: throw IllegalStateException("Bukan JSON valid")
}
