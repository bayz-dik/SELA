package id.sela.network

import id.sela.model.TripHistory
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data hasil restore backup — dikembalikan agar UI yang menerapkan ke DAO
 * (pemanggil bertanggung jawab menulis ke database).
 */
data class BackupData(
    val favorites: List<Pair<String, String>>,
    val history: List<TripHistory>,
    val extra: Map<String, String>,
)

/**
 * Backup & restore lokal SELA (fitur #118/#119).
 *
 * Format: satu JSON berisi favorit, riwayat perjalanan, dan preferensi
 * penting. Tidak ada server: file bisa disimpan ke mana saja lewat
 * Storage Access Framework (ACTION_CREATE_DOCUMENT untuk ekspor,
 * ACTION_OPEN_DOCUMENT untuk impor) — termasuk Google Drive lewat SAF.
 *
 * Contoh isi:
 * {"version":1,"exportedAt":...,"favorites":[{"id":"..","kind":"stop"}],
 *  "history":[{"routeId":"..","routeName":"..","stopId":null,"stopName":null,"timestamp":..}],
 *  "extra":{"widget_route_filter":"1"}}
 */
@Singleton
class BackupManager @Inject constructor() {

    fun backupJson(
        favorites: List<Pair<String, String>>,
        history: List<TripHistory>,
        extra: Map<String, String> = emptyMap(),
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "id.sela")
        val favArr = JSONArray()
        favorites.forEach { (id, kind) ->
            favArr.put(JSONObject().put("id", id).put("kind", kind))
        }
        root.put("favorites", favArr)
        val histArr = JSONArray()
        history.forEach { t ->
            histArr.put(
                JSONObject()
                    .put("routeId", t.routeId)
                    .put("routeName", t.routeName)
                    .put("stopId", t.stopId ?: JSONObject.NULL)
                    .put("stopName", t.stopName ?: JSONObject.NULL)
                    .put("timestamp", t.timestamp),
            )
        }
        root.put("history", histArr)
        val extraObj = JSONObject()
        extra.forEach { (k, v) -> extraObj.put(k, v) }
        root.put("extra", extraObj)
        return root.toString(2)
    }

    /**
     * Parse JSON backup. Mengembalikan null bila format tidak dikenali.
     * Tidak menulis apa pun — pemanggil yang menerapkan hasilnya.
     */
    fun restoreJson(json: String): BackupData? = runCatching {
        val root = JSONObject(json)
        require(root.optInt("version", 0) == 1) { "versi backup tidak didukung" }
        val favorites = mutableListOf<Pair<String, String>>()
        root.optJSONArray("favorites")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                favorites += o.getString("id") to o.getString("kind")
            }
        }
        val history = mutableListOf<TripHistory>()
        root.optJSONArray("history")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                history += TripHistory(
                    routeId = o.getString("routeId"),
                    routeName = o.getString("routeName"),
                    stopId = o.optString("stopId").takeIf { o.has("stopId") && !o.isNull("stopId") },
                    stopName = o.optString("stopName").takeIf { o.has("stopName") && !o.isNull("stopName") },
                    timestamp = o.getLong("timestamp"),
                )
            }
        }
        val extra = mutableMapOf<String, String>()
        root.optJSONObject("extra")?.let { obj ->
            obj.keys().forEach { k -> extra[k] = obj.optString(k) }
        }
        BackupData(favorites, history, extra)
    }.getOrNull()
}
