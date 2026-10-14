package id.sela.mesh

import android.content.Context
import android.os.Build
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Berbagi info ETA halte ke HP di sekitar via Nearby Connections (P2P).
 *
 * Batasan yang perlu dipahami:
 * - Butuh izin BLUETOOTH_ADVERTISE / BLUETOOTH_CONNECT (Android 12+) dan lokasi
 *   (ACCESS_FINE_LOCATION) agar discovery jalan. Izin diminta dari layar, bukan dari sini.
 * - Advertising + discovery HANYA berjalan saat layar MeshScreen dibuka ([startHosting]
 *   dipanggil, [stop] saat layar ditutup). Tidak ada background mesh.
 * - Ini P2P 1-hop (bintang, Strategy.P2P_STAR), BUKAN mesh multi-hop beneran:
 *   pesan hanya sampai ke HP yang terhubung langsung, tidak diteruskan ke HP lain.
 * - Payload hanya JSON string sederhana (lihat [MeshSnapshot.toJson]).
 */
@Singleton
class MeshShare @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        /** ID layanan Nearby; harus sama di semua HP yang mau saling menemukan. */
        const val SERVICE_ID = "id.sela.mesh"
    }

    private val client: ConnectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val peers = mutableMapOf<String, String>() // endpointId -> nama HP

    private val payloadCallback = object : PayloadCallback() {
        var onPayload: ((String, String) -> Unit)? = null
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            onPayload?.invoke(endpointId, String(bytes, Charsets.UTF_8))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Terima semua koneksi masuk otomatis; ini fitur antar-teman, bukan publik.
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) = Unit
        override fun onDisconnected(endpointId: String) {
            peers.remove(endpointId)
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        var onPeer: ((String) -> Unit)? = null
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            peers[endpointId] = info.endpointName
            client.requestConnection(Build.MODEL, endpointId, lifecycleCallback)
            onPeer?.invoke(info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            peers.remove(endpointId)
        }
    }

    /**
     * Mulai advertising + discovery.
     *
     * @param scope dipakai untuk meneruskan callback ke thread utama dengan aman.
     * @param onPeer dipanggil dengan nama HP setiap ada perangkat baru ditemukan.
     * @param onPayload dipanggil dengan (endpointId, jsonString) saat payload diterima.
     */
    fun startHosting(
        scope: CoroutineScope,
        onPeer: (String) -> Unit,
        onPayload: (String, String) -> Unit,
    ) {
        discoveryCallback.onPeer = { name -> scope.launch(Dispatchers.Main) { onPeer(name) } }
        payloadCallback.onPayload =
            { endpointId, json -> scope.launch(Dispatchers.Main) { onPayload(endpointId, json) } }
        client.startAdvertising(
            Build.MODEL,
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build(),
        )
        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build(),
        )
    }

    /** Hentikan advertising, discovery, dan semua koneksi. */
    fun stop() {
        discoveryCallback.onPeer = null
        payloadCallback.onPayload = null
        peers.clear()
        client.stopAllEndpoints()
        client.stopAdvertising()
        client.stopDiscovery()
    }

    /**
     * Kirim snapshot ETA ke satu endpoint yang sudah terhubung.
     *
     * @param endpointId id endpoint tujuan (dari discovery).
     * @param json hasil [MeshSnapshot.toJson].
     */
    fun sendSnapshot(endpointId: String, json: String) {
        client.sendPayload(endpointId, Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
    }

    /** Daftar nama HP yang ditemukan (untuk tampilan). */
    fun peerNames(): List<String> = peers.values.toList()

    /** endpointId per nama HP, untuk tombol kirim per item. */
    fun endpointIdFor(name: String): String? = peers.entries.firstOrNull { it.value == name }?.key
}

/**
 * Snapshot ETA satu halte yang dikirim antar HP.
 *
 * @param etaMinutes menit sampai bus tiba; -1 artinya "tanpa data ETA" (tampil "—").
 */
data class MeshSnapshot(
    val stopId: String,
    val stopName: String,
    val etaMinutes: Int,
    val timestamp: Long,
)

/** Serialisasi manual tanpa library JSON; escape minimal (backslash dan kutip). */
fun MeshSnapshot.toJson(): String {
    fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    return "{\"stopId\":\"${esc(stopId)}\"," +
        "\"stopName\":\"${esc(stopName)}\"," +
        "\"etaMinutes\":$etaMinutes," +
        "\"timestamp\":$timestamp}"
}

/**
 * Parser minimal untuk payload yang kita kirim sendiri.
 * Bukan parser JSON umum — hanya membaca empat field [MeshSnapshot].
 * Mengembalikan null kalau formatnya tidak dikenali.
 */
fun parseMeshSnapshot(json: String): MeshSnapshot? {
    fun str(key: String): String? {
        val m = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(json) ?: return null
        return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
    }
    fun num(key: String): Long? {
        val m = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(json) ?: return null
        return m.groupValues[1].toLongOrNull()
    }
    val stopId = str("stopId") ?: return null
    val stopName = str("stopName") ?: return null
    val eta = num("etaMinutes")?.toInt() ?: return null
    val ts = num("timestamp") ?: return null
    return MeshSnapshot(stopId, stopName, eta, ts)
}
