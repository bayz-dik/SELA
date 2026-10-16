package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.sela.designsystem.SelaV7
import id.sela.feature.settings.TripPlannerViewModel
import id.sela.model.TripPlan

/**
 * Cari Rute V7 — sesuai 06_CARI_RUTE_ACUAN.png.
 * Form Dari/Ke, pilihan moda (bus/jalan/kombinasi), "Berangkat sekarang",
 * daftar alternatif rute dari GTFS aktual (bukan angka demo).
 */
@Composable
fun V7RouteSearchScreen(
    onBack: () -> Unit,
    onSelectPlan: (TripPlan) -> Unit,
    viewModel: TripPlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = SelaV7.ink)
            }
        }

        // Form Dari/Ke
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = SelaV7.surface,
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                V7StopField(
                    label = "Dari",
                    value = state.stops.find { it.id == state.originId }?.name ?: "Blok M",
                    onClick = { /* pilih halte asal */ },
                )
                Divider(color = SelaV7.hairline, modifier = Modifier.padding(vertical = 8.dp))
                V7StopField(
                    label = "Ke",
                    value = state.stops.find { it.id == state.destId }?.name ?: "Kota Tua",
                    onClick = { /* pilih halte tujuan */ },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Pilihan moda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val modes = listOf(
                Icons.Default.DirectionsBus to "Bus",
                Icons.Default.DirectionsWalk to "Jalan",
                Icons.Default.TransferWithinAStation to "Kombinasi",
            )
            var selected by remember { mutableIntStateOf(0) }
            modes.forEachIndexed { i, (icon, label) ->
                FilterChip(
                    selected = selected == i,
                    onClick = { selected = i },
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SelaV7.ink,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Berangkat sekarang",
            style = MaterialTheme.typography.bodyMedium,
            color = SelaV7.inkSoft,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Daftar rute — data aktual atau empty state
        if (state.plans.isEmpty()) {
            V7EmptyState(
                message = if (state.searched)
                    "Tidak ada rute ditemukan. Coba halte lain."
                else
                    "Pilih halte asal dan tujuan untuk melihat rute.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.plans) { plan ->
                    V7RouteCard(plan = plan, onClick = { onSelectPlan(plan) })
                }
            }
        }
    }
}

@Composable
private fun V7StopField(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (label == "Dari") Icons.Default.TripOrigin else Icons.Default.Place,
            contentDescription = null,
            tint = SelaV7.inkFaint,
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SelaV7.inkFaint,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaV7.ink,
            )
        }
    }
}

@Composable
private fun V7RouteCard(plan: TripPlan, onClick: () -> Unit) {
    // Label jujur: rencana struktural dari GTFS, bukan estimasi waktu
    val transferText = if (plan.transfers == 0) "Tanpa transit" else "${plan.transfers}x transit"
    val routeNames = plan.legs.map { it.route.shortName ?: it.route.longName }.distinct().take(2).joinToString(" → ")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = SelaV7.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = routeNames.ifBlank { "Rute" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SelaV7.ink,
                )
                Text(
                    text = "Naik ${plan.legs.size} bus · $transferText",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.inkSoft,
                )
                Text(
                    text = "Rencana struktural — bukan jadwal keberangkatan",
                    style = MaterialTheme.typography.labelSmall,
                    color = SelaV7.inkFaint,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint = SelaV7.inkFaint,
            )
        }
    }
}

/**
 * Detail Rute V7 — sesuai 07_DETAIL_RUTE_ACUAN.png.
 * Peta mini di atas, timeline perjalanan di bawah,
 * tombol "Simpan" dan "Mulai Navigasi".
 */
@Composable
fun V7RouteDetailScreen(
    plan: TripPlan,
    onBack: () -> Unit,
    onStartNavigation: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        // Area peta (diisi MapLibre mini oleh caller atau placeholder)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .background(SelaV7.periSoft),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(SelaV7.surface),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = SelaV7.ink)
            }
            // TODO: MapLibre mini map dengan polyline rute
        }

        // Timeline di bawah
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = SelaV7.surface,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Detail Rute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SelaV7.ink,
                )
                val routeNames = plan.legs.map { it.route.shortName ?: it.route.longName }.distinct().joinToString(" → ")
                Text(
                    text = routeNames.ifBlank { "Rute" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = SelaV7.ink,
                )
                Text(
                    text = if (plan.transfers == 0) "Tanpa transit" else "${plan.transfers}x transit · rencana struktural",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.inkSoft,
                )
                Spacer(Modifier.height(16.dp))

                // Timeline legs
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(plan.legs) { leg ->
                        V7TimelineLeg(
                            icon = Icons.Default.DirectionsBus,
                            title = "Naik ${leg.route.shortName ?: leg.route.longName}",
                            subtitle = "${leg.fromStop.name} → ${leg.toStop.name}",
                        )
                    }
                }

                // Tombol aksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan")
                    }
                    Button(
                        onClick = onStartNavigation,
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SelaV7.ink,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    ) {
                        Text("Mulai Navigasi", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun V7TimelineLeg(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(SelaV7.limeSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SelaV7.ink, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaV7.ink,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.inkSoft,
                )
            }
        }
    }
}

@Composable
fun V7EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = SelaV7.inkFaint,
            modifier = Modifier.padding(32.dp),
        )
    }
}
