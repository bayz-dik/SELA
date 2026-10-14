package id.sela.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.profileStore by preferencesDataStore(name = "sela_profiles")

/** Satu profil pengguna di perangkat yang sama (fitur #120). */
data class UserProfile(val id: String, val name: String, val homeStopId: String?)

/**
 * Multi-profil ringan (fitur #120).
 *
 * Tiap profil menyimpan: nama tampilan, halte rumah, dan filter rute widget.
 * Berpindah profil menerapkan ulang preferensi tampilan tersebut via
 * [TransitPreferences] (filter widget) — profil BUKAN akun terpisah.
 *
 * KDoc jujur: riwayat perjalanan & favorit TETAP GLOBAL (satu database Room
 * untuk semua profil). Memisahkan riwayat per profil butuh kolom profil di
 * semua tabel — terlalu invasif untuk manfaatnya. Yang dipisah: nama,
 * halte rumah, dan filter widget.
 */
@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: TransitPreferences,
) {
    private val profilesKey = stringPreferencesKey("profiles_json")
    private val activeKey = stringPreferencesKey("active_profile")

    private val data = context.profileStore.data

    val profiles: Flow<List<UserProfile>> = data.map { p ->
        val raw = p[profilesKey] ?: return@map listOf(UserProfile("default", "Pengguna", null))
        runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                UserProfile(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    homeStopId = o.optString("homeStopId").takeIf { o.has("homeStopId") && !o.isNull("homeStopId") },
                )
            }
        }.getOrDefault(listOf(UserProfile("default", "Pengguna", null)))
    }.distinctUntilChanged()

    val activeProfileId: Flow<String> = data.map { it[activeKey] ?: "default" }.distinctUntilChanged()

    suspend fun addProfile(name: String, homeStopId: String? = null): String {
        val id = "p${System.currentTimeMillis()}"
        context.profileStore.edit { p ->
            val current = profilesList(p[profilesKey]) + UserProfile(id, name.trim().ifEmpty { "Pengguna" }, homeStopId)
            p[profilesKey] = JSONArray(current.map {
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("homeStopId", it.homeStopId ?: JSONObject.NULL)
            }).toString()
        }
        return id
    }

    private fun profilesList(raw: String?): List<UserProfile> {
        if (raw.isNullOrEmpty()) return listOf(UserProfile("default", "Pengguna", null))
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                UserProfile(
                    o.getString("id"), o.getString("name"),
                    o.optString("homeStopId").takeIf { o.has("homeStopId") && !o.isNull("homeStopId") },
                )
            }
        }.getOrDefault(listOf(UserProfile("default", "Pengguna", null)))
    }

    suspend fun removeProfile(id: String) {
        if (id == "default") return
        context.profileStore.edit { p ->
            val remaining = profilesList(p[profilesKey]).filterNot { it.id == id }
            p[profilesKey] = JSONArray(remaining.map {
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("homeStopId", it.homeStopId ?: JSONObject.NULL)
            }).toString()
            if (p[activeKey] == id) p[activeKey] = "default"
        }
    }

    /** Ganti profil aktif. Menerapkan ulang filter widget default (null = semua). */
    suspend fun setActiveProfile(id: String) {
        context.profileStore.edit { it[activeKey] = id }
        prefs.setWidgetRouteFilter(null)
    }
}
