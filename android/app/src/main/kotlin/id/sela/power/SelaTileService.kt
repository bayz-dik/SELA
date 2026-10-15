package id.sela.power

import android.content.Intent
import android.service.quicksettings.TileService
import id.sela.network.TransitPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile SELA (fitur #96).
 *
 * - Ketuk tile → membuka MainActivity SELA.
 * - Saat tile terlihat ([onStartListening]), subtitle diisi snapshot terakhir
 *   dari DataStore: jumlah bus terpantau & status koneksi.
 *
 * JUJUR: subtitle ini snapshot terakhir yang ditulis repository, BUKAN data
 * live — angka bisa basi bila aplikasi lama tidak dibuka. Tile ini hanya
 * jalan pintas + ringkasan, bukan pengganti layar peta.
 *
 * CATATAN INTEGRASI: service ini perlu didaftarkan di AndroidManifest.xml
 * dengan action android.service.quicksettings.action.QS_TILE (tidak dilakukan
 * di file ini — manifest di luar cakupan tugas ini).
 */
class SelaTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        scope.launch {
            val prefs = TransitPreferences(applicationContext)
            val count = runCatching { prefs.widgetVehicleCount.first() }.getOrDefault(0)
            val status = runCatching { prefs.widgetStatus.first() }.getOrDefault("OFFLINE")
            tile.subtitle = "Bus: $count • $status"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { startActivity(it) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
