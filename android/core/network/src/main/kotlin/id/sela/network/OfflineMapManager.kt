package id.sela.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Satu region peta offline yang tersimpan di perangkat. */
data class OfflineRegionInfo(val id: Long, val name: String)

/** Progres unduhan region peta offline. */
data class DownloadProgress(
    val regionName: String,
    val completed: Long,
    val total: Long,
    val isComplete: Boolean,
)

/**
 * Pengelola peta offline (fitur #13).
 *
 * Mengunduh tile peta Jakarta (style yang sama dengan peta utama SELA)
 * ke penyimpanan perangkat via MapLibre OfflineManager.
 *
 * JUJUR: unduhan butuh koneksi internet dan bisa berukuran ratusan MB
 * (zoom 10–16). Setelah selesai, peta tetap terbuka tanpa internet;
 * halte, rute, dan posisi bus terakhir (jika pernah dimuat) tetap tampil.
 */
@Singleton
class OfflineMapManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        /**
         * Style yang sama dipakai peta utama (MapScreen.SELA_MAP_STYLE).
         * Disalin di sini karena core:network tidak boleh bergantung ke
         * feature:map — bila style peta utama berubah, ubah juga di sini.
         */
        const val SELA_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
        const val JAKARTA_REGION_NAME = "jakarta"
        const val MIN_ZOOM = 10.0
        const val MAX_ZOOM = 16.0
    }

    private val appContext = context.applicationContext

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    /** Progres unduhan aktif; null bila tidak ada unduhan berjalan. */
    val downloadProgress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val refreshSignal = MutableStateFlow(0)

    /** Daftar region offline yang tersimpan; panggil [refreshRegions] untuk memuat ulang. */
    val regions: Flow<List<OfflineRegionInfo>> =
        refreshSignal.map { listRegions() }.flowOn(Dispatchers.IO)

    private fun manager(): OfflineManager {
        MapLibre.getInstance(appContext)
        return OfflineManager.getInstance(appContext)
    }

    /** Muat ulang daftar region dari penyimpanan. */
    fun refreshRegions() {
        refreshSignal.value += 1
    }

    /**
     * Unduh tile peta Jakarta (batas selatan −6.37, barat 106.68,
     * utara −6.09, timur 107.0; zoom 10–16). Progres mengalir lewat
     * [downloadProgress]. Melewati bila region "jakarta" sudah ada.
     */
    suspend fun downloadJakarta(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val mgr = manager()
            if (listRegions(mgr).any { it.name == JAKARTA_REGION_NAME }) {
                error("Peta Jakarta sudah tersimpan di perangkat.")
            }
            val bounds = LatLngBounds.from(-6.09, 107.0, -6.37, 106.68)
            val definition = OfflineTilePyramidRegionDefinition(
                SELA_MAP_STYLE_URL,
                bounds,
                MIN_ZOOM,
                MAX_ZOOM,
                appContext.resources.displayMetrics.density,
            )
            val metadata = JSONObject()
                .put("name", JAKARTA_REGION_NAME)
                .toString()
                .toByteArray(Charsets.UTF_8)
            val region = suspendCancellableCoroutine<OfflineRegion> { cont ->
                mgr.createOfflineRegion(
                    definition,
                    metadata,
                    object : OfflineManager.CreateOfflineRegionCallback {
                        override fun onCreate(r: OfflineRegion) {
                            cont.resume(r)
                        }

                        override fun onError(e: String) {
                            cont.resumeWithException(IllegalStateException(e.ifBlank { "Gagal membuat region offline." }))
                        }
                    },
                )
            }
            region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                override fun onStatusChanged(status: OfflineRegionStatus) {
                    _progress.value = DownloadProgress(
                        regionName = JAKARTA_REGION_NAME,
                        completed = status.completedResourceCount,
                        total = status.requiredResourceCount,
                        isComplete = status.isComplete,
                    )
                    if (status.isComplete) refreshRegions()
                }

                override fun onError(error: OfflineRegionError) {
                    _progress.value = null
                }

                override fun mapboxTileCountLimitExceeded(limit: Long) {
                    _progress.value = null
                }
            })
            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        }
    }

    /** Hapus region offline dari penyimpanan perangkat. */
    suspend fun deleteRegion(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val region = findRegion(manager(), id)
                ?: error("Region tidak ditemukan.")
            suspendCancellableCoroutine<Unit> { cont ->
                region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() {
                        cont.resume(Unit)
                    }

                    override fun onError(e: String) {
                        cont.resumeWithException(IllegalStateException(e.ifBlank { "Gagal menghapus region." }))
                    }
                })
            }
            refreshRegions()
        }
    }

    private suspend fun listRegions(mgr: OfflineManager): List<OfflineRegionInfo> =
        suspendCancellableCoroutine { cont ->
            mgr.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    cont.resume(
                        offlineRegions.orEmpty().map { region ->
                            OfflineRegionInfo(region.id, regionNameOf(region))
                        },
                    )
                }

                override fun onError(error: String) {
                    cont.resumeWithException(IllegalStateException(error.ifBlank { "Gagal membaca region offline." }))
                }
            })
        }

    private suspend fun listRegions(): List<OfflineRegionInfo> = listRegions(manager())

    private suspend fun findRegion(mgr: OfflineManager, id: Long): OfflineRegion? =
        suspendCancellableCoroutine { cont ->
            mgr.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    cont.resume(offlineRegions?.firstOrNull { it.id == id })
                }

                override fun onError(error: String) {
                    cont.resumeWithException(IllegalStateException(error.ifBlank { "Gagal membaca region offline." }))
                }
            })
        }

    private fun regionNameOf(region: OfflineRegion): String = runCatching {
        JSONObject(String(region.metadata, Charsets.UTF_8)).optString("name", "region-${region.id}")
    }.getOrDefault("region-${region.id}")
}
