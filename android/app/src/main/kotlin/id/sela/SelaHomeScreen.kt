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
                    title = "Mau ke mana?",
                    subtitle = if (isLive && activeBusCount > 0) {
                        "$activeBusCount bus terpantau."
                    } else {
                        "Rute dan halte tersimpan di HP."
                    },
                    cta = if (isLive) "Lihat bus" else "Buka peta",
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
                        detail = if (isLive) "terpantau" else "belum ada data",
                        icon = Icons.Default.DirectionsBus, tint = SelaPastel.mintSoft, onClick = onOpenMap,
                        modifier = Modifier.weight(1f))
                    SelaMetricTile(label = "Rute", value = state.routes.size.toString(),
                        detail = "data lokal", icon = Icons.Default.Route,
                        tint = SelaPastel.peachSoft, onClick = onPlanTrip, modifier = Modifier.weight(1f))
                }
            }
            item {
                SelaV2SectionTitle("Mulai dari sini")
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelaQuickAction("Cari rute", "Halte asal dan tujuan", Icons.Default.AltRoute,
                        SelaPastel.lavenderSoft, onPlanTrip, Modifier.weight(1f))
                    SelaQuickAction("Tanya Sela", "Tanya soal bus", Icons.Default.ChatBubbleOutline,
                        SelaPastel.pinkSoft, onOpenAssistant, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SelaV2SectionTitle("Rute")
                    TextButton(onClick = onPlanTrip) { Text("Semua", color = SelaPastel.lavenderDeep) }
                }
            }
            if (state.routes.isEmpty()) {
                item {
                    SelaTransitLine("↗", "Belum ada rute", "Muat ulang data",
                        onClick = onOpenMap)
                }
            } else {
                items(state.routes.take(3), key = { it.id }) { route ->
                    SelaTransitLine(
                        badge = route.shortName.ifBlank { "BUS" },
                        title = route.longName.ifBlank { "Rute ${route.shortName}" },
                        subtitle = "Buka di peta",
                        onClick = { onSelectRoute(route.id) },
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SelaV2SectionTitle("Halte")
                    TextButton(onClick = onOpenMap) { Text("Peta", color = SelaPastel.lavenderDeep) }
                }
            }
            if (state.stops.isEmpty()) {
                item { Text("Belum ada data halte.", color = SelaPastel.inkSoft) }
            } else {
                items(state.stops.take(2), key = { "stop-${it.id}" }) { stop ->
                    SelaTransitLine("H", stop.name, "Buka di peta",
                        onClick = { onSelectStop(stop.id) }, tint = SelaPastel.mintSoft)
                }
            }
            if (state.data.favorites.isNotEmpty()) {
                item { SelaV2SectionTitle("Favorit") }
                val favoriteRoutes = state.routes.filter { route -> state.data.favorites.any { it.kind == "route" && it.id == route.id } }
                val favoriteStops = state.stops.filter { stop -> state.data.favorites.any { it.kind == "stop" && it.id == stop.id } }
                items(favoriteRoutes.take(2), key = { "favorite-route-${it.id}" }) { route ->
                    SelaTransitLine(
                        badge = route.shortName.ifBlank { "BUS" },
                        title = route.longName.ifBlank { "Rute ${route.shortName}" },
                        subtitle = "Buka di peta",
                        onClick = { onSelectRoute(route.id) },
                        tint = SelaPastel.lavenderSoft,
                    )
                }
                items(favoriteStops.take(2), key = { "favorite-stop-${it.id}" }) { stop ->
                    SelaTransitLine(
                        badge = "H", title = stop.name,
                        subtitle = "Buka di peta",
                        onClick = { onSelectStop(stop.id) },
                        tint = SelaPastel.mintSoft,
                    )
                }
            }
            item {
                TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Riwayat")
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
