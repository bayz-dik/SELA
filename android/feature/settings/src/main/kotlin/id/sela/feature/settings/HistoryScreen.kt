package id.sela.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.sela.designsystem.*
import id.sela.model.CarbonFootprint
import id.sela.model.Gamification
import id.sela.model.groupHistoryByDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatHour(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale("id")).format(Date(ts))

/**
 * Layar Riwayat perjalanan (fitur #2 — UI).
 *
 * Riwayat dikelompokkan per hari ("Hari ini", "Kemarin", tanggal),
 * tiap item menampilkan nama rute, halte, dan jam. Kartu statistik
 * memakai [id.sela.model.TripStats]. Berbagi SettingsViewModel dengan
 * layar Pengaturan (state yang sama, instance terpisah).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val groups = remember(state.history) { groupHistoryByDay(state.history, System.currentTimeMillis()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat perjalanan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.message?.let { msg ->
                    SelaNoticeBanner(
                        text = msg,
                        icon = Icons.Default.Info,
                        tone = SelaNoticeTone.NEUTRAL,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Kartu statistik
                SelaCard {
                    SelaSectionTitle("Perjalananmu")
                    val stats = state.stats
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SelaStatCard(
                            value = "${stats.tripsThisWeek}x",
                            label = "Minggu ini",
                            icon = Icons.Default.DirectionsBus,
                            accent = true,
                            modifier = Modifier.weight(1f),
                        )
                        SelaStatCard(
                            value = stats.topRouteName?.take(12) ?: "—",
                            label = "Rute tersering",
                            icon = Icons.Default.Route,
                            modifier = Modifier.weight(1f),
                        )
                        SelaStatCard(
                            value = stats.topStopName?.take(12) ?: "—",
                            label = "Halte tersering",
                            icon = Icons.Default.LocationOn,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Statistik gila (#80) — estimasi kasar, untuk hiburan
                if (state.wildStats.totalTrips > 0) {
                    SelaCard {
                        SelaSectionTitle("Statistik gila")
                        Text(
                            "Semua angka estimasi kasar, bukan data resmi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val wild = state.wildStats
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SelaStatCard(
                                value = "${"%.0f".format(wild.estimatedKm)} km",
                                label = "Estimasi jarak",
                                icon = Icons.Default.Route,
                                modifier = Modifier.weight(1f),
                            )
                            SelaStatCard(
                                value = "${"%.1f".format(wild.jakartaBaliX)}x",
                                label = "Jakarta–Bali",
                                icon = Icons.Default.Public,
                                modifier = Modifier.weight(1f),
                            )
                            SelaStatCard(
                                value = "${wild.longestStreakDays} hari",
                                label = "Streak terpanjang",
                                icon = Icons.Default.LocalFireDepartment,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Badge gamifikasi (#18)
                if (state.badges.isNotEmpty()) {
                    SelaCard {
                        SelaSectionTitle("Lencana")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gamification.ALL.filter { it.id in state.badges }.forEach { badge ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(badge.name, style = MaterialTheme.typography.titleSmall)
                                        Text(badge.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Jejak karbon (#20) + Time capsule (#30)
                if (state.allHistory.isNotEmpty()) {
                    SelaCard {
                        SelaSectionTitle("Jejak karbon")
                        Text(
                            "Estimasi kasar: ${"%.1f".format(state.carbonKg)} kg CO₂ hemat vs naik motor (7,5 km × 60 g/km per perjalanan).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SelaGhostButton("Bagikan", {
                            val text = CarbonFootprint.shareText(state.carbonKg)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Bagikan jejak karbon"))
                        })
                    }
                    state.timeCapsule?.let { trip ->
                        SelaCard {
                            SelaSectionTitle("Kapsul waktu")
                            Text(
                                "Setahun lalu, jam segini kamu naik ${trip.routeName.ifBlank { trip.routeId }}${trip.stopName?.let { " dari $it" } ?: ""}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Daftar per hari
                if (groups.isEmpty()) {
                    SelaCard {
                        SelaSectionTitle("Belum ada riwayat")
                        Text(
                            "Pilih rute atau halte di peta untuk mulai mencatat perjalanan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    groups.forEach { group ->
                        SelaCard {
                            SelaSectionTitle(group.label)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                group.entries.forEach { h ->
                                    Column(Modifier.padding(vertical = 6.dp)) {
                                        Text(
                                            h.routeName.ifBlank { "Perjalanan" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                        )
                                        Text(
                                            buildString {
                                                h.stopName?.takeIf { it.isNotBlank() }?.let { append("Halte $it") }
                                                append(" · ${formatHour(h.timestamp)}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SelaGhostButton("Ekspor CSV", { viewModel.exportCsv { csv -> shareCsv(context, csv) } })
                        SelaGhostButton("Hapus riwayat", viewModel::clearHistory, destructive = true)
                    }
                    Text(
                        "Ekspor CSV hanya teks — bagikan ke aplikasi pilihanmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

/** Bagikan teks CSV riwayat via chooser sistem (fitur #116). Dipakai SettingsScreen & HistoryScreen. */
fun shareCsv(context: android.content.Context, csv: String) {
    if (csv.isBlank()) return
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Riwayat perjalanan SELA")
        putExtra(android.content.Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Bagikan riwayat CSV"))
}
