package id.sela.model

import java.time.Instant

/** Timestamp ordering plus a local revision barrier protects in-flight HTTP snapshots from WS races. */
class VehicleReconciler {
    private data class Entry(val vehicle: Vehicle, val revision: Long)
    private val rows = linkedMapOf<String, Entry>()
    private val removed = mutableMapOf<String, Long>()
    private var revision = 0L
    @Synchronized fun snapshotBarrier(): Long = revision
    @Synchronized fun update(vehicle: Vehicle) {
        val timestamp = instant(vehicle.observedAt) ?: return
        if (!vehicle.lat.isFinite() || !vehicle.lon.isFinite() || vehicle.lat !in -90.0..90.0 || vehicle.lon !in -180.0..180.0) return
        val old = rows[vehicle.id]?.vehicle
        val previous = old?.let { instant(it.observedAt) }
        if (previous != null && timestamp < previous) return
        val accepted = if (previous == timestamp) old!!.copy(isStale = old.isStale || vehicle.isStale) else vehicle
        rows[vehicle.id] = Entry(accepted, ++revision)
        removed.remove(vehicle.id)
    }
    @Synchronized fun remove(id: String, routeId: String?) {
        // A delayed removal for a previous route must not erase a newer route's position.
        if (rows[id]?.vehicle?.let { routeId != null && it.routeId != routeId } == true) return
        rows.remove(id)
        removed[id] = ++revision
    }
    @Synchronized fun reconcile(snapshot: List<Vehicle>, barrier: Long) {
        val ids = snapshot.mapTo(hashSetOf()) { it.id }
        rows.entries.removeAll { (id, entry) -> id !in ids && entry.revision <= barrier }
        snapshot.forEach { vehicle ->
            if ((removed[vehicle.id] ?: -1) > barrier) return@forEach
            val entry = rows[vehicle.id]
            if (entry != null && entry.revision > barrier) return@forEach
            update(vehicle)
        }
        removed.entries.removeAll { it.value <= barrier }
    }
    @Synchronized fun values(now: Instant = Instant.now()): List<Vehicle> = rows.values.map { entry ->
        val vehicle = entry.vehicle
        vehicle.copy(isStale = vehicle.isStale || instant(vehicle.observedAt)?.let { !now.isBefore(it.plusSeconds(90)) } != false)
    }.sortedBy { it.id }
    private fun instant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()
}
