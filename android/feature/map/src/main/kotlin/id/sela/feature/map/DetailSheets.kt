package id.sela.feature.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Angka hitung mundur ETA pastel — tabular, tidak bergeser saat berubah. */
private val EtaNumeralsPastel = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
)
private val EtaNumeralsSmallPastel = EtaNumeralsPastel.copy(fontSize = 22.sp, lineHeight = 28.sp)

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
            color = SelaPastel.ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SelaPastel.inkSoft,
        )
    }
}

@Composable
private fun FavoriteButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        SelaButton(
            text = "Hapus dari favorit",
            onClick = onClick,
            style = SelaButtonStyle.Soft,
            accent = SelaPastel.pink,
            onAccent = SelaPastel.pinkDeep,
            icon = Icons.Default.Favorite,
            modifier = modifier
                .fillMaxWidth()
                .semantics { stateDescription = "Tersimpan di favorit" },
        )
    } else {
        SelaButton(
            text = "Simpan ke favorit",
            onClick = onClick,
            style = SelaButtonStyle.Primary,
            icon = Icons.Default.FavoriteBorder,
            modifier = modifier
                .fillMaxWidth()
                .semantics { stateDescription = "Belum disimpan" },
        )
    }
}

/** Kartu pemberitahuan di dalam sheet — netral atau peringatan, pastel. */
@Composable
private fun SheetNotice(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Info,
    warning: Boolean = false,
    modifier: Modifier = Modifier,
) {
    PastelCard(
        tint = if (warning) SelaPastel.dangerSoft else SelaPastel.blueSoft,
        shadowTint = if (warning) SelaPastel.dangerDeep else SelaPastel.blueDeep,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (warning) SelaPastel.dangerDeep else SelaPastel.blueDeep,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.inkSoft)
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
    val lineColor = SelaPastel.hairline
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
                PastelPulseDot(active = true, color = SelaPastel.mintDeep, modifier = Modifier.size(12.dp))
            } else {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = SelaPastel.hairline,
                    modifier = Modifier.size(8.dp),
                ) {}
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
                    PastelRouteBadge(estimate.routeId)
                    Text("Bus ${estimate.vehicleId}", style = MaterialTheme.typography.bodyMedium, color = SelaPastel.ink)
                }
                Text(
                    "≈ ${estimate.distanceMeters.roundToInt()} m dari halte",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }
            Text(
                formatEta(estimate.etaMinutes),
                style = if (isFirst) EtaNumeralsPastel else EtaNumeralsSmallPastel,
                color = if (isFirst) SelaPastel.mintDeep else SelaPastel.ink,
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
    PastelSheetBody {
        DetailHeader(
            stop.name,
            stop.code?.takeIf { it.isNotBlank() }?.let { "Halte · $it" } ?: "Halte",
        )

        // Estimasi kedatangan — pahlawan layar ini
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PastelSectionTitle("Bus berikutnya")
            if (arrivals.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SelaMascot(
                        expression = SelaMascotExpression.WaitingBus,
                        diameter = 52.dp,
                        bodyColor = SelaPastel.lavender,
                        contentDescription = "Maskot SELA sedang menunggu bus",
                    )
                    Text(
                        "Aku belum melihat armada terkini untuk halte ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
                        modifier = Modifier.weight(1f),
                    )
                }
                SheetNotice(
                    "Belum ada estimasi",
                    "Tidak ada bus live dari rute halte ini yang terpantau saat ini. Lihat posisi terakhir bus di peta.",
                    Icons.Default.Schedule,
                )
            } else {
                if (arrivals.first().etaMinutes <= 3.0) {
                    PastelCard(tint = SelaPastel.mintSoft, elevation = 0.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SelaMascot(
                                expression = SelaMascotExpression.BusApproaching,
                                diameter = 58.dp,
                                bodyColor = SelaPastel.mint,
                                contentDescription = "Maskot SELA senang melihat bus diperkirakan dekat",
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Bus diperkirakan dekat",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SelaPastel.ink,
                                )
                                Text(
                                    "Sekitar ${formatEta(arrivals.first().etaMinutes)} dari posisi terakhir. Bukan jadwal resmi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SelaPastel.inkSoft,
                                )
                            }
                        }
                    }
                }
                Column {
                    arrivals.forEachIndexed { index, a ->
                        ArrivalTimelineRow(a, isFirst = index == 0, isLast = index == arrivals.lastIndex)
                    }
                }
                Text(
                    "Estimasi lokal dari posisi bus terakhir di perangkat ini — bukan jadwal resmi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
                // Bagikan ETA tercepat (fitur #12) + ajak bareng (fitur #71)
                Row(
                    Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PastelTextButton(
                        text = "Ajak bareng",
                        onClick = { extras.onKonvoi(arrivals.first()) },
                    )
                    PastelTextButton(
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
            PastelSectionTitle("Rute di halte ini")
            if (stop.routeIds.isEmpty()) {
                Text(
                    "Informasi rute belum tersedia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaPastel.inkSoft,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stop.routeIds.forEach { PastelRouteBadge(it) }
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
    val tint = route.displayServiceType.pastelColor()
    val deep = route.displayServiceType.pastelDeepColor()
    PastelSheetBody {
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
            PastelRouteBadge(route.shortName, tint = tint, deep = deep)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PastelSectionTitle("Jalur perjalanan")
            when {
                loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SelaPastel.mintDeep,
                        trackColor = SelaPastel.mintSoft,
                    )
                    Text(
                        "Memuat jalur rute…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
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

        SelaButton(
            text = "Lihat peta",
            onClick = onShowMap,
            style = SelaButtonStyle.Primary,
            icon = Icons.Default.Map,
            modifier = Modifier.fillMaxWidth(),
        )
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
    val type = route?.displayServiceType
    PastelSheetBody {
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
            PastelRouteBadge(
                route?.shortName ?: vehicle.routeId,
                tint = type?.pastelColor() ?: SelaPastel.blue,
                deep = type?.pastelDeepColor() ?: SelaPastel.blueDeep,
            )
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

        PastelCard(tint = SelaPastel.cream) {
            PastelSectionTitle("Detail bus")
            PastelKeyValueRow("Pembaruan terakhir", observedAt)
            PastelKeyValueRow("Kecepatan terakhir", speed?.let { "${it.toInt()} km/jam" } ?: "Belum tersedia")
            PastelKeyValueRow(
                "Status",
                if (vehicle.isStale) "Tidak aktif" else "Live",
            )
        }

        if (route != null) {
            SelaButton(
                text = "Lihat rute ${route.shortName}",
                onClick = { onRoute(route.id) },
                style = SelaButtonStyle.Soft,
                accent = type?.pastelColor() ?: SelaPastel.blue,
                onAccent = type?.pastelDeepColor() ?: SelaPastel.blueDeep,
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
    PastelSheetHeader(
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
                PastelCard(tint = SelaPastel.pinkSoft) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = SelaPastel.pinkDeep,
                        modifier = Modifier.size(32.dp),
                    )
                    Text("Simpan perjalananmu", style = MaterialTheme.typography.titleMedium, color = SelaPastel.ink)
                    Text(
                        "Buka detail halte atau rute, lalu pilih Simpan ke favorit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
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
            val type = route?.displayServiceType
            PastelCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isStop) Icons.Default.DirectionsBus else Icons.Default.Route,
                        contentDescription = null,
                        tint = type?.pastelDeepColor() ?: SelaPastel.blueDeep,
                    )
                    Text(title, style = MaterialTheme.typography.titleMedium, color = SelaPastel.ink, modifier = Modifier.weight(1f))
                }
                if (route != null) PastelRouteBadge(
                    route.shortName,
                    tint = type?.pastelColor() ?: SelaPastel.blue,
                    deep = type?.pastelDeepColor() ?: SelaPastel.blueDeep,
                )
                Text(
                    if (stop == null && route == null) "Detail belum tersedia di perangkat."
                    else if (isStop) "Halte tersimpan"
                    else "${route!!.displayServiceType.label()} · Rute tersimpan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaPastel.inkSoft,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelaButton(
                        text = if (isStop) "Buka halte" else "Buka rute",
                        onClick = { onSelect(item.kind, item.id) },
                        style = SelaButtonStyle.Soft,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRemove(item.id, item.kind) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus $title dari favorit",
                            tint = SelaPastel.inkSoft,
                        )
                    }
                }
            }
        }
    }
}
