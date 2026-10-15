package id.sela.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.sela.designsystem.PastelCard
import id.sela.designsystem.PastelEmptyState
import id.sela.designsystem.PastelSectionTitle
import id.sela.designsystem.SelaButton
import id.sela.designsystem.SelaButtonStyle
import id.sela.designsystem.SelaMascot
import id.sela.designsystem.SelaMascotExpression
import id.sela.designsystem.SelaPastel

/**
 * Layar berbagi ETA ke HP sekitar (Nearby Connections).
 *
 * Jujur soal data: snapshot yang dikirim memakai halte favorit pertama (kalau ada)
 * dengan etaMinutes = -1 yang artinya "tanpa data ETA" — penerima melihat "—".
 * ETA live sungguhan belum diambil dari repository di layar ini.
 *
 * Advertising + discovery hanya aktif selama layar ini dibuka.
 */
@Composable
fun MeshScreen(
    favorites: List<String> = emptyList(),
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val mesh = remember { MeshShare(context) }

    var searching by remember { mutableStateOf(false) }
    var peers by remember { mutableStateOf(listOf<String>()) }
    var received by remember { mutableStateOf(listOf<String>()) }
    var status by remember { mutableStateOf("Belum mencari.") }

    LaunchedEffect(searching) {
        if (!searching) return@LaunchedEffect
        mesh.startHosting(
            scope = scope,
            onPeer = { name ->
                if (name !in peers) peers = peers + name
                status = "Menemukan: $name"
            },
            onPayload = { _, json ->
                val snap = parseMeshSnapshot(json)
                val etaText = when {
                    snap == null -> "data tidak terbaca"
                    snap.etaMinutes < 0 -> "—"
                    else -> "${snap.etaMinutes} mnt"
                }
                val name = snap?.stopName ?: "halte tak dikenal"
                received = listOf("Dari HP teman: $name • ETA $etaText") + received
            },
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            mesh.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaPastel.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelaMascot(
                expression = SelaMascotExpression.Happy,
                diameter = 56.dp,
                bodyColor = SelaPastel.peach,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Berbagi ETA Sekitar", style = MaterialTheme.typography.headlineSmall, color = SelaPastel.ink)
                Text(
                    "Cari HP teman di dekatmu, lalu kirim info halte favoritmu. " +
                        "Butuh Bluetooth + lokasi aktif. Hanya 1-hop, tidak diteruskan ke HP lain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelaButton(
                text = "Cari HP sekitar",
                onClick = { searching = true },
                style = SelaButtonStyle.Primary,
                enabled = !searching,
                icon = Icons.Default.BluetoothSearching,
            )
            SelaButton(
                text = "Berhenti",
                onClick = {
                    searching = false
                    mesh.stop()
                    peers = emptyList()
                    status = "Berhenti."
                },
                style = SelaButtonStyle.Soft,
                enabled = searching,
            )
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = SelaPastel.inkSoft)

        PastelSectionTitle("HP ditemukan (${peers.size})")
        if (peers.isEmpty()) {
            PastelCard {
                PastelEmptyState(
                    if (searching) "Mencari HP di sekitar…" else "Tekan “Cari HP sekitar” untuk mulai.",
                    mascotSize = 72.dp,
                    bodyColor = SelaPastel.peach,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(peers) { name ->
                    PastelCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge, color = SelaPastel.ink)
                                Text(
                                    "Kirim halte favorit: ${favorites.firstOrNull() ?: "belum ada favorit"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SelaPastel.inkSoft,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            SelaButton(
                                text = "Kirim ETA favoritku",
                                onClick = {
                                    val endpointId = mesh.endpointIdFor(name) ?: return@SelaButton
                                    val fav = favorites.firstOrNull() ?: "belum-ada-favorit"
                                    val snap = MeshSnapshot(
                                        stopId = fav,
                                        stopName = fav,
                                        etaMinutes = -1, // tanpa data ETA sungguhan
                                        timestamp = System.currentTimeMillis(),
                                    )
                                    mesh.sendSnapshot(endpointId, snap.toJson())
                                },
                                style = SelaButtonStyle.Soft,
                                icon = Icons.Default.Send,
                            )
                        }
                    }
                }
            }
        }

        PastelSectionTitle("Diterima (${received.size})")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(received) { msg ->
                PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                    Text(msg, color = SelaPastel.ink)
                }
            }
        }
    }
}
