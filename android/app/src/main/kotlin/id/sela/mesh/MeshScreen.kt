package id.sela.mesh

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Berbagi ETA Sekitar", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Cari HP teman di dekatmu, lalu kirim info halte favoritmu. " +
                "Butuh Bluetooth + lokasi aktif. Hanya 1-hop, tidak diteruskan ke HP lain.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { searching = true }, enabled = !searching) {
                Text("Cari HP sekitar")
            }
            OutlinedButton(
                onClick = {
                    searching = false
                    mesh.stop()
                    peers = emptyList()
                    status = "Berhenti."
                },
                enabled = searching,
            ) {
                Text("Berhenti")
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)

        Text("HP ditemukan (${peers.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(peers) { name ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Kirim halte favorit: ${favorites.firstOrNull() ?: "belum ada favorit"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val endpointId = mesh.endpointIdFor(name) ?: return@Button
                                val fav = favorites.firstOrNull() ?: "belum-ada-favorit"
                                val snap = MeshSnapshot(
                                    stopId = fav,
                                    stopName = fav,
                                    etaMinutes = -1, // tanpa data ETA sungguhan
                                    timestamp = System.currentTimeMillis(),
                                )
                                mesh.sendSnapshot(endpointId, snap.toJson())
                            },
                        ) {
                            Text("Kirim ETA favoritku")
                        }
                    }
                }
            }
        }

        Text("Diterima (${received.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(received) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(msg, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
