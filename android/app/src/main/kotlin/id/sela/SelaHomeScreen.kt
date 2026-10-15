package id.sela

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.*
import id.sela.feature.map.MapUiState
import id.sela.model.ConnectionStatus

/**
 * Real SELA home dashboard, backed by the SAME MapViewModel used on the map.
 * No fabricated bus ETAs, mock vehicles, distance, or location claims.
 */
@Composable
fun SelaHomeScreen(
    state: MapUiState,
    onOpenMap: () -> Unit,
    onPlanTrip: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAssistant: () -> Unit,
    onSelectRoute: (String) -> Unit,
    onSelectStop: (String) -> Unit,
) {
    val isLive = state.connectionStatus == ConnectionStatus.LIVE
    val activeBusCount = state.vehicles.count { !it.isStale }
    SelaAuroraBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(19.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SELA", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black, color = SelaPastel.ink, letterSpacing = 3.sp)
                        Text("TEMAN TRANSIT JAKARTA", style = MaterialTheme.typography.labelSmall,
                            color = SelaPastel.inkSoft, letterSpacing = .7.sp)
                    }
                    SelaLivePill(connected = isLive,
                        label = when (state.connectionStatus) {
                            ConnectionStatus.LIVE -> "Bus live"
                            ConnectionStatus.CONNECTING -> "Menghubungkan"
                            ConnectionStatus.RECONNECTING -> "Menyambung ulang"
                            ConnectionStatus.OFFLINE -> "Offline"
                            ConnectionStatus.ERROR -> "Koneksi bermasalah"
                        })
                }
            }
            item {
                SelaHeroPanel(
                    title = "Ke mana kita\nmelaju hari ini?",
                    subtitle = if (isLive && activeBusCount > 0) {
                        "$activeBusCount bus sedang terpantau. Aku siap bantu cari jalan yang pas."
                    } else {
                        "Rute dan halte lokal sudah siap. Yuk, pilih tujuanmu."
                    },
                    cta = if (isLive) "Lihat bus live" else "Jelajahi peta",
                    onClick = onOpenMap,
                    mascotExpression = when {
                        isLive && activeBusCount > 0 -> SelaMascotExpression.Welcome
                        state.connectionStatus == ConnectionStatus.ERROR -> SelaMascotExpression.Error
                        state.connectionStatus == ConnectionStatus.OFFLINE -> SelaMascotExpression.Offline
                        else -> SelaMascotExpression.Welcome
                    },
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelaMetricTile(label = "Bus aktif", value = if (isLive) activeBusCount.toString() else "—",
                        detail = if (isLive) "terpantau sekarang" else "menunggu data live",
                        icon = Icons.Default.DirectionsBus, tint = Color(0xFFDDF2E9), onClick = onOpenMap,
                        modifier = Modifier.weight(1f))
                    SelaMetricTile(label = "Rute kota", value = state.routes.size.toString(),
                        detail = "rute dari data lokal", icon = Icons.Default.Route,
                        tint = Color(0xFFFFE9CD), onClick = onPlanTrip, modifier = Modifier.weight(1f))
                }
            }
            item {
                SelaV2SectionTitle("Mau mulai dari mana?", "PILIH LANGKAH BERIKUTNYA")
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelaQuickAction("Cari rute", "Temukan jalanmu", Icons.Default.AltRoute,
                        Color(0xFFE3DBFF), onPlanTrip, Modifier.weight(1f))
                    SelaQuickAction("Tanya Sela", "Asisten perjalanan", Icons.Default.ChatBubbleOutline,
                        Color(0xFFFFE1E9), onOpenAssistant, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SelaV2SectionTitle("Rute tersedia", "DATA DARI PERANGKAT")
                    TextButton(onClick = onPlanTrip) { Text("Lihat semua →", color = SelaPastel.lavenderDeep) }
                }
            }
            if (state.routes.isEmpty()) {
                item {
                    SelaTransitLine("↗", "Rute belum tersedia", "Coba muat ulang data rute",
                        onClick = onOpenMap)
                }
            } else {
                items(state.routes.take(3), key = { it.id }) { route ->
                    SelaTransitLine(
                        badge = route.shortName.ifBlank { "BUS" },
                        title = route.longName.ifBlank { "Rute ${route.shortName}" },
                        subtitle = "Lihat rute pada peta",
                        onClick = { onSelectRoute(route.id) },
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SelaV2SectionTitle("Kenali halte Jakarta")
                    TextButton(onClick = onOpenMap) { Text("Peta →", color = SelaPastel.lavenderDeep) }
                }
            }
            if (state.stops.isEmpty()) {
                item { Text("Belum ada data halte lokal.", color = SelaPastel.inkSoft) }
            } else {
                items(state.stops.take(2), key = { "stop-${it.id}" }) { stop ->
                    SelaTransitLine("H", stop.name, "Lihat halte di peta",
                        onClick = { onSelectStop(stop.id) }, tint = SelaPastel.mintSoft)
                }
            }
            if (state.data.favorites.isNotEmpty()) {
                item { SelaV2SectionTitle("Tersimpan untukmu", "FAVORIT") }
                val favoriteRoutes = state.routes.filter { route -> state.data.favorites.any { it.kind == "route" && it.id == route.id } }
                val favoriteStops = state.stops.filter { stop -> state.data.favorites.any { it.kind == "stop" && it.id == stop.id } }
                items(favoriteRoutes.take(2), key = { "favorite-route-${it.id}" }) { route ->
                    SelaTransitLine(
                        badge = route.shortName.ifBlank { "BUS" },
                        title = route.longName.ifBlank { "Rute ${route.shortName}" },
                        subtitle = "Rute favorit · buka peta",
                        onClick = { onSelectRoute(route.id) },
                        tint = SelaPastel.lavenderSoft,
                    )
                }
                items(favoriteStops.take(2), key = { "favorite-stop-${it.id}" }) { stop ->
                    SelaTransitLine(
                        badge = "H", title = stop.name,
                        subtitle = "Halte favorit · buka peta",
                        onClick = { onSelectStop(stop.id) },
                        tint = SelaPastel.mintSoft,
                    )
                }
            }
            item {
                TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Lihat riwayat perjalanan")
                }
            }
        }
    }
}

@Composable
private fun SelaQuickAction(
    title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, color = color, modifier = modifier.heightIn(min = 125.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(icon, null, tint = SelaPastel.ink, modifier = Modifier.size(25.dp))
            Text(title, color = SelaPastel.ink, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = SelaPastel.inkSoft, style = MaterialTheme.typography.bodySmall,
                maxLines = 2)
        }
    }
}
