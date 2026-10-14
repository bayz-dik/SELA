package id.sela.network

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.model.Route
import id.sela.model.ServiceType
import id.sela.model.Stop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Model mentah JSON GTFS — field harus sama persis dengan key JSON (lihat ProGuard rules). */
@Keep
private data class RawRoute(
    val id: String,
    val code: String?,
    val name: String,
    val color: String?,
)

@Keep
private data class RawStop(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val routes: List<String> = emptyList(),
    val code: String? = null,
)

/**
 * Parser GTFS murni tanpa dependency Android — bisa di-unit-test langsung.
 */
internal object GtfsJsonParser {
    private val gson = Gson()

    fun parseRoutes(json: String): List<Route> {
        val type = object : TypeToken<List<RawRoute>>() {}.type
        val raw: List<RawRoute> = gson.fromJson(json, type) ?: return emptyList()
        return raw.mapNotNull { r ->
            if (r.id.isBlank() || r.name.isBlank()) return@mapNotNull null
            Route(
                id = r.id,
                shortName = r.code?.takeIf { it.isNotBlank() } ?: r.id,
                longName = r.name,
                serviceType = ServiceType.BUS, // displayServiceType dihitung dari shortName
                color = r.color?.takeIf { it.isNotBlank() },
            )
        }
    }

    fun parseStops(json: String): List<Stop> {
        val type = object : TypeToken<List<RawStop>>() {}.type
        val raw: List<RawStop> = gson.fromJson(json, type) ?: return emptyList()
        return raw.mapNotNull { s ->
            if (s.id.isBlank() || s.name.isBlank()) return@mapNotNull null
            if (s.lat !in -90.0..90.0 || s.lon !in -180.0..180.0) return@mapNotNull null
            Stop(
                id = s.id,
                name = s.name,
                lat = s.lat,
                lon = s.lon,
                routeIds = s.routes.filter { it.isNotBlank() },
                code = s.code?.takeIf { it.isNotBlank() },
            )
        }
    }
}

/**
 * Repository GTFS lokal: baca dari assets bawaan APK, dengan override dari
 * direktori files/ jika ada hasil pembaruan data (fitur #1).
 */
@Singleton
class LocalGtfsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var cachedRoutes: List<Route>? = null
    private var cachedStops: List<Stop>? = null

    private fun updateDir(): File = File(context.filesDir, "gtfs-update").also { it.mkdirs() }

    suspend fun routes(): List<Route> = withContext(Dispatchers.IO) {
        cachedRoutes ?: loadRoutes().also { cachedRoutes = it }
    }

    suspend fun stops(): List<Stop> = withContext(Dispatchers.IO) {
        cachedStops ?: loadStops().also { cachedStops = it }
    }

    private fun readText(name: String): String {
        val updated = File(updateDir(), name)
        if (updated.exists()) return updated.readText()
        return context.assets.open("gtfs/$name").bufferedReader().use { it.readText() }
    }

    private fun listFiles(): List<String> {
        val updated = updateDir().listFiles { f -> f.name.startsWith("stops-") }?.map { it.name }
        if (!updated.isNullOrEmpty()) return updated.sorted()
        return context.assets.list("gtfs")?.filter { it.startsWith("stops-") }?.sorted() ?: emptyList()
    }

    private fun loadRoutes(): List<Route> = GtfsJsonParser.parseRoutes(readText("routes.json"))

    private fun loadStops(): List<Stop> {
        val stops = mutableListOf<Stop>()
        for (file in listFiles()) {
            stops += GtfsJsonParser.parseStops(readText(file))
        }
        return stops
    }

    /** Simpan hasil unduhan pembaruan data (fitur #1). */
    fun applyUpdate(files: Map<String, String>) {
        val dir = updateDir()
        for ((name, content) in files) {
            if (!name.matches(Regex("^(routes|stops-\\d+)\\.json$"))) continue
            File(dir, name).writeText(content)
        }
        clearCache()
    }

    /** Kembalikan ke data bawaan APK (hapus hasil pembaruan). */
    fun resetToBundled() {
        updateDir().listFiles()?.forEach { it.delete() }
        clearCache()
    }

    fun hasUpdate(): Boolean = (updateDir().listFiles()?.isNotEmpty() == true)

    fun clearCache() {
        cachedRoutes = null
        cachedStops = null
    }
}
