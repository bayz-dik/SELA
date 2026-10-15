package id.sela.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.designsystem.*
import id.sela.model.Leg
import id.sela.model.Route
import id.sela.model.Stop
import id.sela.model.TripPlan
import id.sela.model.TripPlanner
import id.sela.model.displayServiceType
import id.sela.network.StandaloneTransitRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlannerUiState(
    val stops: List<Stop> = emptyList(),
    val routes: List<Route> = emptyList(),
    val originId: String? = null,
    val destId: String? = null,
    val plans: List<TripPlan> = emptyList(),
    val searched: Boolean = false,
)

@HiltViewModel
class TripPlannerViewModel @Inject constructor(
    repository: StandaloneTransitRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(PlannerUiState())
    val state: StateFlow<PlannerUiState> = mutable.asStateFlow()
    private var latestStops: List<Stop> = emptyList()
    private var latestRoutes: List<Route> = emptyList()

    init {
        viewModelScope.launch {
            repository.data.collect { data ->
                latestStops = data.stops
                latestRoutes = data.routes
                mutable.update { it.copy(stops = data.stops, routes = data.routes) }
            }
        }
    }

    fun setOrigin(id: String?) = mutable.update { it.copy(originId = id, searched = false) }
    fun setDest(id: String?) = mutable.update { it.copy(destId = id, searched = false) }

    fun swap() = mutable.update {
        it.copy(originId = it.destId, destId = it.originId, searched = false)
    }

    fun plan() {
        val origin = latestStops.find { it.id == mutable.value.originId }
        val dest = latestStops.find { it.id == mutable.value.destId }
        val plans = if (origin != null && dest != null) {
            TripPlanner.planTrip(origin, dest, latestStops, latestRoutes)
        } else emptyList()
        mutable.update { it.copy(plans = plans, searched = true) }
    }
}

/** Pilih halte dengan kolom teks + saran (daftar halte bisa ribuan). */
@Composable
private fun StopPicker(
    label: String,
    stops: List<Stop>,
    selected: Stop?,
    onSelect: (Stop?) -> Unit,
) {
    // B24: state teks DIPISAH dari selection — remember TIDAK di-key dengan
    // selected?.id agar draft ketikan tidak ter-reset saat user mengoreksi teks.
    var query by remember { mutableStateOf(selected?.name.orEmpty()) }
    // Penanda: selection menjadi null karena user sedang mengetik (bukan aksi
    // luar) — dalam kasus ini draft ketikan dipertahankan.
    var typingCleared by remember { mutableStateOf(false) }
    // Sinkronkan teks hanya untuk perubahan pilihan dari luar (mis. tombol Tukar).
    LaunchedEffect(selected?.id) {
        if (selected != null || !typingCleared) {
            query = selected?.name.orEmpty()
        }
        typingCleared = false
    }
    val suggestions = remember(query, stops) {
        if (query.isBlank()) emptyList()
        else stops.filter { it.name.contains(query, ignoreCase = true) }.take(8)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (selected != null && it != selected.name) {
                    typingCleared = true
                    onSelect(null)
                }
            },
            label = { Text(label) },
            placeholder = { Text("Ketik nama halte…") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = ""; onSelect(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = SelaPastel.inkSoft)
                    }
                }
            },
            singleLine = true,
            shape = SelaPastelShapes.field,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SelaPastel.blueDeep,
                unfocusedBorderColor = SelaPastel.hairline,
                focusedLabelColor = SelaPastel.blueDeep,
                unfocusedLabelColor = SelaPastel.inkSoft,
                cursorColor = SelaPastel.blueDeep,
                focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.isNotEmpty()) {
            PastelCard(contentPadding = PaddingValues(8.dp)) {
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(suggestions, key = { it.id }) { stop ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(stop); query = stop.name }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = SelaPastel.mintDeep,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(stop.name, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: TripPlan) {
    PastelCard(
        tint = if (plan.isDirect) SelaPastel.mintSoft else SelaPastel.lavenderSoft,
        shadowTint = if (plan.isDirect) SelaPastel.mintDeep else SelaPastel.lavenderDeep,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (plan.isDirect) Icons.Default.DirectionsBus else Icons.Default.SyncAlt,
                contentDescription = null,
                tint = if (plan.isDirect) SelaPastel.mintDeep else SelaPastel.lavenderDeep,
            )
            Text(
                if (plan.isDirect) "Langsung — tanpa transit"
                else "1x transit",
                style = MaterialTheme.typography.titleSmall,
                color = SelaPastel.ink,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            plan.legs.forEachIndexed { index, leg ->
                LegRow(index = index, leg = leg)
                if (index < plan.legs.lastIndex) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = SelaPastel.inkSoft,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "Transit di halte ${leg.toStop.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SelaPastel.inkSoft,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegRow(index: Int, leg: Leg) {
    val type = leg.route.displayServiceType
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PastelRouteBadge(
            leg.route.shortName,
            tint = type.pastelColor(),
            deep = type.pastelDeepColor(),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Naik ${leg.route.shortName.ifBlank { leg.route.id }}",
                style = MaterialTheme.typography.bodyMedium,
                color = SelaPastel.ink,
            )
            Text(
                "${leg.fromStop.name} → ${leg.toStop.name}",
                style = MaterialTheme.typography.bodySmall,
                color = SelaPastel.inkSoft,
            )
        }
        if (index == 0) {
            Text(
                "Berangkat",
                style = MaterialTheme.typography.labelMedium,
                color = SelaPastel.blueDeep,
            )
        }
    }
}

/**
 * Perencana perjalanan offline (fitur #9).
 *
 * 100% dari data lokal — tanpa internet, tanpa server.
 * JUJUR: ini perutean struktural (halte↔rute), bukan jadwal:
 * tidak tahu jam keberangkatan maupun waktu tempuh.
 */
@Composable
fun TripPlannerScreen(
    viewModel: TripPlannerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val origin = state.stops.find { it.id == state.originId }
    val dest = state.stops.find { it.id == state.destId }

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
            PastelTopBar("Perencana perjalanan", onBack, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SelaCompanionHeroCard(
                    eyebrow = "Perencana perjalanan offline",
                    title = if (state.searched && state.plans.isNotEmpty()) "Aku sudah menemukan rute" else "Kita susun perjalananmu dulu",
                    body = if (state.searched && state.plans.isNotEmpty()) {
                        "Ada ${state.plans.size} opsi yang bisa kamu pakai dari ${origin?.name ?: "halte asal"} ke ${dest?.name ?: "halte tujuan"}."
                    } else {
                        "Pilih halte asal dan tujuan. Aku akan menyusun opsi rute dari GTFS lokal tanpa perlu internet."
                    },
                    expression = when {
                        state.searched && state.plans.isNotEmpty() -> SelaMascotExpression.RouteFound
                        state.searched -> SelaMascotExpression.Empty
                        else -> SelaMascotExpression.Searching
                    },
                    bodyColor = if (state.searched && state.plans.isNotEmpty()) SelaPastel.mint else SelaPastel.blue,
                    stats = listOf(
                        SelaHeroStat("Halte", state.stops.size.toString(), Icons.Default.LocationOn, SelaPastel.blueSoft, SelaPastel.blueDeep),
                        SelaHeroStat("Rute", state.routes.size.toString(), Icons.Default.Route, SelaPastel.lavenderSoft, SelaPastel.lavenderDeep),
                        SelaHeroStat("Opsi", if (state.searched) state.plans.size.toString() else "—", Icons.Default.DirectionsBus, SelaPastel.mintSoft, SelaPastel.mintDeep),
                    ),
                )
                PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                    StopPicker("Dari halte", state.stops, origin, { viewModel.setOrigin(it?.id) })
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        PastelTextButton(
                            text = "Tukar",
                            onClick = viewModel::swap,
                            enabled = origin != null || dest != null,
                        )
                    }
                    StopPicker("Ke halte", state.stops, dest, { viewModel.setDest(it?.id) })
                    SelaButton(
                        text = "Rencanakan perjalanan",
                        onClick = viewModel::plan,
                        style = SelaButtonStyle.Primary,
                        enabled = origin != null && dest != null,
                        icon = Icons.Default.Route,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Rencana struktural dari data statis di perangkat — bukan jadwal keberangkatan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SelaPastel.inkSoft,
                    )
                }

                if (state.searched) {
                    if (state.plans.isEmpty()) {
                        PastelCard {
                            PastelSectionTitle("Tidak ada rute")
                            PastelEmptyState(
                                "Tidak ditemukan kombinasi rute yang melayani kedua halte. Coba halte lain yang berdekatan.",
                                mascotExpression = SelaMascotExpression.Empty,
                                mascotSize = 80.dp,
                            )
                        }
                    } else {
                        state.plans.forEach { PlanCard(it) }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
