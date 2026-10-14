package id.sela.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import id.sela.designsystem.*
import id.sela.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** "≈ 3 mnt" / "tiba" untuk ETA di bawah 1 menit. */
private fun formatEta(minutes: Double): String = when {
    minutes < 1.0 -> "tiba"
    else -> "≈ ${minutes.roundToInt()} mnt"
}

// Timestamp feed yang rusak tidak boleh merusak sheet atau menyiratkan kesegaran.
private fun readableObservation(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM yyyy · HH.mm.ss 'WIB'", Locale.forLanguageTag("id-ID"))
        .withZone(ZoneId.of("Asia/Jakarta"))
        .format(Instant.parse(value))
}.getOrDefault("Waktu pembaruan tidak tersedia")

// ---------------------------------------------------------------------------
// Kepala & tombol detail
// ---------------------------------------------------------------------------

@Composable
private fun DetailHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        SelaSecondaryButton(
            text = "Hapus dari favorit",
            onClick = onClick,
            icon = Icons.Default.Favorite,
            modifier = modifier
                .fillMaxWidth()
                .semantics { stateDescription = "Tersimpan di favorit" },
        )
    } else {
        SelaPrimaryButton(
            text = "Simpan ke favorit",
            onClick = onClick,
            icon = Icons.Default.FavoriteBorder,
            modifier = modifier
                .fillMaxWidth()
                .semantics { stateDescription = "Belum disimpan" },
        )
    }
}

/** Kartu pemberitahuan di dalam sheet — netral atau peringatan. */
@Composable
private fun SheetNotice(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Info,
    warning: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bg = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = SelaTokens.cardSmall,
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Timeline ETA — komponen #72
// ---------------------------------------------------------------------------

/**
 * Satu baris kedatangan pada timeline: node berdenyut + lencana rute +
 * angka ETA besar tabular.
 */
@Composable
private fun ArrivalTimelineRow(
    estimate: ArrivalEstimate,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Rel timeline
        Box(
            Modifier
                .width(20.dp)
                .fillMaxHeight()
                .drawBehind {
                    val cx = size.width / 2
                    if (!isFirst) drawLine(lineColor, Offset(cx, 0f), Offset(cx, size.height * 0.32f), strokeWidth = 3f)
                    if (!isLast) drawLine(lineColor, Offset(cx, size.height * 0.68f), Offset(cx, size.height), strokeWidth = 3f)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isFirst) {
                SelaPulseDot(active = true, modifier = Modifier.size(12.dp))
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.outline, modifier = Modifier.size(8.dp)) {}
            }
        }
        // Isi
        Row(
            Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelaRouteBadge(estimate.routeId)
                    Text("Bus ${estimate.vehicleId}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "≈ ${estimate.distanceMeters.roundToInt()} m dari halte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatEta(estimate.etaMinutes),
                style = if (isFirst) EtaNumerals else EtaNumeralsSmall,
                color = if (isFirst) SelaPalette.volt else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sheet: halte
// ---------------------------------------------------------------------------

/**
 * Ekstra opsional untuk [StopDetailSheet] yang diisi modul app
 * (feature:map tidak boleh bergantung ke modul app).
 */
data class StopSheetExtras(
    /** Tampilkan kartu wisata (fitur #58, mode turis aktif). */
    val showTourist: Boolean = false,
    /** Konten kartu wisata dari modul app. */
    val touristContent: @Composable () -> Unit = {},
    /** Ajak bareng / konvoi (fitur #71). */
    val onKonvoi: (ArrivalEstimate) -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopDetailSheet(
    stop: Stop,
    arrivals: List<ArrivalEstimate> = emptyList(),
    favorite: Boolean,
    onFavorite: () -> Unit,
    /** Bagikan ETA tercepat (fitur #12). */
    onShare: (ArrivalEstimate) -> Unit = {},
    extras: StopSheetExtras = StopSheetExtras(),
) {
    SelaSheetBody {
        DetailHeader(
            stop.name,
            stop.code?.takeIf { it.isNotBlank() }?.let { "Halte · $it" } ?: "Halte",
        )

        // Estimasi kedatangan — pahlawan layar ini
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SelaSectionTitle("Bus berikutnya")
            if (arrivals.isEmpty()) {
                SheetNotice(
                    "Belum ada estimasi",
                    "Tidak ada bus live dari rute halte ini yang terpantau saat ini. Lihat posisi terakhir bus di peta.",
                    Icons.Default.Schedule,
                )
            } else {
                Column {
                    arrivals.forEachIndexed { index, a ->
                        ArrivalTimelineRow(a, isFirst = index == 0, isLast = index == arrivals.lastIndex)
                    }
                }
                Text(
                    "Estimasi lokal dari posisi bus terakhir di perangkat ini — bukan jadwal resmi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Bagikan ETA tercepat (fitur #12) + ajak bareng (fitur #71)
                Row(
                    Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelaGhostButton(
                        text = "Ajak bareng",
                        onClick = { extras.onKonvoi(arrivals.first()) },
                    )
                    SelaGhostButton(
                        text = "Bagikan",
                        onClick = { onShare(arrivals.first()) },
                    )
                }
            }
        }

        // Mode turis (#58) — kartu info wisata
        if (extras.showTourist) {
            extras.touristContent()
        }

        // Rute di halte ini
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelaSectionTitle("Rute di halte ini")
            if (stop.routeIds.isEmpty()) {
                Text(
                    "Informasi rute belum tersedia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stop.routeIds.forEach { SelaRouteBadge(it) }
                }
            }
        }

        FavoriteButton(favorite, onFavorite)
    }
}

// ---------------------------------------------------------------------------
// Sheet: rute
// ---------------------------------------------------------------------------

@Composable
fun RouteDetailSheet(
    route: Route,
    favorite: Boolean,
    loading: Boolean,
    shapeAvailable: Boolean,
    onFavorite: () -> Unit,
    onShowMap: () -> Unit,
) {
    SelaSheetBody {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailHeader(
                route.longName.ifBlank { "Rute ${route.shortName}" },
                route.displayServiceType.label(),
                modifier = Modifier.weight(1f),
            )
            SelaRouteBadge(route.shortName, route.displayServiceType.color())
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelaSectionTitle("Jalur perjalanan")
            when {
                loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SelaPalette.volt,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        "Memuat jalur rute…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                shapeAvailable -> SheetNotice("Jalur tersedia", "Lihat lintasan rute di peta.", Icons.Default.Route)
                else -> SheetNotice(
                    "Jalur belum tersedia",
                    "Peta tetap dapat dibuka. Lintasan rute belum dapat ditampilkan.",
                    Icons.Default.Map,
                )
            }
        }

        SelaPrimaryButton("Lihat peta", onShowMap, icon = Icons.Default.Map, modifier = Modifier.fillMaxWidth())
        FavoriteButton(favorite, onFavorite)
    }
}

// ---------------------------------------------------------------------------
// Sheet: kendaraan
// ---------------------------------------------------------------------------

@Composable
fun VehicleDetailSheet(vehicle: Vehicle, route: Route?, onRoute: (String) -> Unit) {
    val observedAt = remember(vehicle.observedAt) { readableObservation(vehicle.observedAt) }
    val speed = vehicle.speedKmh?.takeIf { it.isFinite() && it >= 0 }
    SelaSheetBody {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailHeader(
                "Bus ${vehicle.id}",
                route?.longName?.takeIf { it.isNotBlank() } ?: "Detail rute belum tersedia",
                modifier = Modifier.weight(1f),
            )
            SelaRouteBadge(route?.shortName ?: vehicle.routeId, route?.displayServiceType?.color() ?: MaterialTheme.colorScheme.primary)
        }

        SheetNotice(
            title = if (vehicle.isStale) "Posisi belum diperbarui" else "Posisi terakhir diterima",
            message = if (vehicle.isStale) {
                "Data lama, bukan posisi langsung. Bus mungkin sudah berpindah."
            } else {
                "Posisi mengikuti pembaruan data, bukan lokasi terus-menerus."
            },
            icon = if (vehicle.isStale) Icons.Default.History else Icons.Default.LocationOn,
            warning = vehicle.isStale,
        )

        SelaCard {
            SelaSectionTitle("Detail bus")
            SelaKeyValueRow("Pembaruan terakhir", observedAt)
            SelaKeyValueRow("Kecepatan terakhir", speed?.let { "${it.toInt()} km/jam" } ?: "Belum tersedia")
            SelaKeyValueRow(
                "Status",
                if (vehicle.isStale) "Tidak aktif" else "Live",
            )
        }

        if (route != null) {
            SelaSecondaryButton(
                "Lihat rute ${route.shortName}",
                { onRoute(route.id) },
                icon = Icons.Default.Route,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Daftar favorit
// ---------------------------------------------------------------------------

@Composable
fun ColumnScope.FavoritesList(data: TransitData, onSelect: (String, String) -> Unit, onRemove: (String, String) -> Unit) {
    SelaSheetHeader(
        title = "Favorit",
        subtitle = "Halte dan rute pilihan, tersimpan di perangkat.",
    )
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (data.favorites.isEmpty()) {
            item(key = "favorites-empty") {
                SelaCard {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = SelaPalette.volt,
                        modifier = Modifier.size(32.dp),
                    )
                    Text("Simpan perjalananmu", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Buka detail halte atau rute, lalu pilih Simpan ke favorit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(data.favorites.sortedWith(compareBy({ it.kind }, { it.id })), key = { "${it.kind}:${it.id}" }) { item ->
            val isStop = item.kind == "stop"
            val stop = if (isStop) data.stops.find { it.id == item.id } else null
            val route = if (!isStop) data.routes.find { it.id == item.id } else null
            val title = stop?.name ?: route?.longName?.takeIf { it.isNotBlank() }
                ?: "${if (isStop) "Halte" else "Rute"} ${item.id}"
            SelaCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isStop) Icons.Default.DirectionsBus else Icons.Default.Route,
                        contentDescription = null,
                        tint = SelaPalette.volt,
                    )
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                if (route != null) SelaRouteBadge(route.shortName, route.displayServiceType.color())
                Text(
                    if (stop == null && route == null) "Detail belum tersedia di perangkat."
                    else if (isStop) "Halte tersimpan"
                    else "${route!!.displayServiceType.label()} · Rute tersimpan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelaSecondaryButton(
                        if (isStop) "Buka halte" else "Buka rute",
                        { onSelect(item.kind, item.id) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRemove(item.id, item.kind) },
                        modifier = Modifier.size(SelaTokens.touchTarget),
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus $title dari favorit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
