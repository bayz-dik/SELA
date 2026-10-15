package id.sela.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
@Composable
fun HistoryScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val groups = remember(state.history) { groupHistoryByDay(state.history, System.currentTimeMillis()) }

    Column(
        Modifier
            .fillMaxSize()
            .selaAuroraSurface()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PastelTopBar("Riwayat perjalanan", onBack, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.message?.let { msg ->
                    PastelNoticeBanner(
                        text = msg,
                        icon = Icons.Default.Info,
                        tone = PastelNoticeTone.NEUTRAL,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Kartu statistik — hero aurora + maskot + bento
                PastelCard(
                    tint = SelaPastel.lavenderSoft,
                    shadowTint = SelaPastel.lavenderDeep,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SelaMascot(
                            expression = SelaMascotExpression.Success,
                            diameter = 72.dp,
                            bodyColor = SelaPastel.mint,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PastelSectionTitle("Perjalananmu")
                            Text(
                                "Jejak bus-bus yang kamu naiki.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SelaPastel.inkSoft,
                            )
                        }
                    }
                    val stats = state.stats
                    BentoGrid(columns = 3) {
                        cell {
                            PastelStatCell(
                                value = "${stats.tripsThisWeek}x",
                                label = "Minggu ini",
                                icon = Icons.Default.DirectionsBus,
                                tint = SelaPastel.blueSoft,
                                deep = SelaPastel.blueDeep,
                            )
                        }
                        cell {
                            PastelStatCell(
                                value = stats.topRouteName?.take(10) ?: "—",
                                label = "Rute tersering",
                                icon = Icons.Default.Route,
                                tint = SelaPastel.mintSoft,
                                deep = SelaPastel.mintDeep,
                            )
                        }
                        cell {
                            PastelStatCell(
                                value = stats.topStopName?.take(10) ?: "—",
                                label = "Halte tersering",
                                icon = Icons.Default.LocationOn,
                                tint = SelaPastel.peachSoft,
                                deep = SelaPastel.peachDeep,
                            )
                        }
                    }
                }

                // Statistik gila (#80) — estimasi kasar, untuk hiburan
                if (state.wildStats.totalTrips > 0) {
                    PastelCard(tint = SelaPastel.peachSoft, shadowTint = SelaPastel.peachDeep) {
                        PastelSectionTitle("Statistik gila")
                        Text(
                            "Semua angka estimasi kasar, bukan data resmi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SelaPastel.inkSoft,
                        )
                        val wild = state.wildStats
                        BentoGrid(columns = 3) {
                            cell {
                                PastelStatCell(
                                    value = "${"%.0f".format(wild.estimatedKm)} km",
                                    label = "Estimasi jarak",
                                    icon = Icons.Default.Route,
                                    tint = SelaPastel.sunny.copy(alpha = 0.4f),
                                    deep = SelaPastel.sunnyDeep,
                                )
                            }
                            cell {
                                PastelStatCell(
                                    value = "${"%.1f".format(wild.jakartaBaliX)}x",
                                    label = "Jakarta–Bali",
                                    icon = Icons.Default.Public,
                                    tint = SelaPastel.purpleSoft,
                                    deep = SelaPastel.purpleDeep,
                                )
                            }
                            cell {
                                PastelStatCell(
                                    value = "${wild.longestStreakDays} hari",
                                    label = "Streak terpanjang",
                                    icon = Icons.Default.LocalFireDepartment,
                                    tint = SelaPastel.pinkSoft,
                                    deep = SelaPastel.pinkDeep,
                                )
                            }
                        }
                    }
                }

                // Badge gamifikasi (#18)
                if (state.badges.isNotEmpty()) {
                    PastelCard(tint = SelaPastel.sunny.copy(alpha = 0.35f), shadowTint = SelaPastel.sunnyDeep) {
                        PastelSectionTitle("Lencana")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gamification.ALL.filter { it.id in state.badges }.forEach { badge ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SelaPastel.sunny.copy(alpha = 0.6f),
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = SelaPastel.sunnyDeep,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                    Column {
                                        Text(badge.name, style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
                                        Text(badge.desc, style = MaterialTheme.typography.bodySmall, color = SelaPastel.inkSoft)
                                    }
                                }
                            }
                        }
                    }
                }

                // Jejak karbon (#20) + Time capsule (#30)
                if (state.allHistory.isNotEmpty()) {
                    PastelCard(tint = SelaPastel.mintSoft, shadowTint = SelaPastel.mintDeep) {
                        PastelSectionTitle("Jejak karbon")
                        Text(
                            "Estimasi kasar: ${"%.1f".format(state.carbonKg)} kg CO₂ hemat vs naik motor (7,5 km × 60 g/km per perjalanan).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SelaPastel.inkSoft,
                        )
                        PastelTextButton("Bagikan", {
                            val text = CarbonFootprint.shareText(state.carbonKg)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Bagikan jejak karbon"))
                        })
                    }
                    state.timeCapsule?.let { trip ->
                        PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                            PastelSectionTitle("Kapsul waktu")
                            Text(
                                "Setahun lalu, jam segini kamu naik ${trip.routeName.ifBlank { trip.routeId }}${trip.stopName?.let { " dari $it" } ?: ""}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SelaPastel.inkSoft,
                            )
                        }
                    }
                }

                // Daftar per hari
                if (groups.isEmpty()) {
                    PastelCard {
                        PastelSectionTitle("Belum ada riwayat")
                        PastelEmptyState(
                            "Pilih rute atau halte di peta untuk mulai mencatat perjalanan.",
                            mascotSize = 80.dp,
                        )
                    }
                } else {
                    groups.forEach { group ->
                        PastelCard {
                            PastelSectionTitle(group.label)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                group.entries.forEach { h ->
                                    Column(Modifier.padding(vertical = 6.dp)) {
                                        Text(
                                            h.routeName.ifBlank { "Perjalanan" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SelaPastel.ink,
                                            maxLines = 1,
                                        )
                                        Text(
                                            buildString {
                                                h.stopName?.takeIf { it.isNotBlank() }?.let { append("Halte $it") }
                                                append(" · ${formatHour(h.timestamp)}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SelaPastel.inkSoft,
                                        )
                                    }
                                    HorizontalDivider(color = SelaPastel.hairline)
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PastelTextButton("Ekspor CSV", { viewModel.exportCsv { csv -> shareCsv(context, csv) } })
                        PastelTextButton("Hapus riwayat", viewModel::clearHistory, destructive = true)
                    }
                    Text(
                        "Ekspor CSV hanya teks — bagikan ke aplikasi pilihanmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SelaPastel.inkSoft,
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
