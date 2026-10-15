package id.sela.ar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.model.EtaPredictor
import id.sela.model.Stop
import id.sela.model.TransitData
import id.sela.model.TransitRepository
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel untuk layar AR kompas (fitur #7, #88).
 */
@HiltViewModel
class ArViewModel @Inject constructor(
    private val repository: TransitRepository,
) : ViewModel() {
    val data: StateFlow<TransitData> = repository.data

    /** Halte terdekat dari posisi pengguna. */
    fun nearestStop(userLat: Double, userLon: Double): Stop? =
        data.value.stops.minByOrNull { EtaPredictor.haversineM(userLat, userLon, it.lat, it.lon) }

    /** Halte berdasarkan ID (untuk mode navigasi ke halte tujuan). */
    fun stopById(stopId: String): Stop? = data.value.stops.find { it.id == stopId }

    /** ETA tercepat (menit) bus ke [stop], null bila tidak ada. */
    fun fastestEtaMinutes(stop: Stop): Double? =
        EtaPredictor.estimate(stop, data.value.vehicles, limit = 1)
            .firstOrNull()?.etaMinutes

    companion object {
        /**
         * Bearing (derajat, 0=utara) dari posisi pengguna ke halte.
         * Rumus bearing great-circle standar.
         */
        fun bearingTo(userLat: Double, userLon: Double, stopLat: Double, stopLon: Double): Double {
            val lat1 = Math.toRadians(userLat)
            val lat2 = Math.toRadians(stopLat)
            val dLon = Math.toRadians(stopLon - userLon)
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        }

        /** Sudut relatif panah = (bearing halte − arah hadap HP + 360) % 360. */
        fun relativeAngle(bearingToTarget: Double, azimuth: Double): Double =
            (bearingToTarget - azimuth + 360.0) % 360.0
    }
}
