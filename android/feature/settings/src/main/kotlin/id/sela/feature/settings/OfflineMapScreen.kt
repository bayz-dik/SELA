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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.designsystem.*
import id.sela.network.DownloadProgress
import id.sela.network.OfflineMapManager
import id.sela.network.OfflineRegionInfo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OfflineMapUiState(
    val regions: List<OfflineRegionInfo> = emptyList(),
    val progress: DownloadProgress? = null,
    val message: String? = null,
    val busy: Boolean = false,
)

@HiltViewModel
class OfflineMapViewModel @Inject constructor(
    private val manager: OfflineMapManager,
) : ViewModel() {
    private val mutable = MutableStateFlow(OfflineMapUiState())
    val state: StateFlow<OfflineMapUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                manager.regions.collect { regions ->
                    mutable.update { it.copy(regions = regions) }
                }
            }
            launch {
                manager.downloadProgress.collect { progress ->
                    mutable.update {
                        it.copy(
                            progress = progress,
                            busy = progress != null && !progress.isComplete,
                        )
                    }
                }
            }
        }
        manager.refreshRegions()
    }

    fun download() = viewModelScope.launch {
        mutable.update { it.copy(busy = true, message = null) }
        val result = manager.downloadJakarta()
        mutable.update {
            it.copy(
                busy = false,
                message = result.exceptionOrNull()?.message ?: "Unduhan peta Jakarta dimulai.",
            )
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        val result = manager.deleteRegion(id)
        mutable.update {
            it.copy(message = result.exceptionOrNull()?.message ?: "Region dihapus.")
        }
    }

    fun dismissMessage() { mutable.update { it.copy(message = null) } }
}

private fun formatPercent(progress: DownloadProgress): String =
    if (progress.total <= 0) "…"
    else "${(progress.completed * 100 / progress.total).coerceIn(0, 100)}%"

/**
 * Peta offline (fitur #13).
 *
 * Unduh tile peta Jakarta ke perangkat. Butuh internet saat mengunduh
 * (bisa ratusan MB, zoom 10–16); setelah selesai peta tetap terbuka
 * tanpa internet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapScreen(
    viewModel: OfflineMapViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val progress = state.progress
    val downloading = progress != null && !progress.isComplete

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peta offline") },
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

                SelaCard {
                    SelaSectionTitle("Jakarta")
                    Text(
                        "Unduh tile peta Jakarta (zoom 10–16) ke perangkat. " +
                            "Butuh internet saat mengunduh; setelah itu peta tetap terbuka tanpa internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (downloading) {
                        val p = state.progress!!
                        LinearProgressIndicator(
                            progress = { if (p.total > 0) p.completed / p.total.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = SelaPalette.volt,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            "Mengunduh… ${formatPercent(p)} (${p.completed}/${p.total} sumber daya)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        SelaPrimaryButton(
                            text = if (state.regions.any { it.name == OfflineMapManager.JAKARTA_REGION_NAME })
                                "Sudah tersimpan" else "Unduh peta Jakarta",
                            onClick = viewModel::download,
                            enabled = !state.busy && state.regions.none { it.name == OfflineMapManager.JAKARTA_REGION_NAME },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                SelaCard {
                    SelaSectionTitle("Tersimpan di perangkat")
                    if (state.regions.isEmpty()) {
                        Text(
                            "Belum ada peta offline. Unduh peta Jakarta di atas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.regions.forEach { region ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = null,
                                            tint = SelaPalette.volt,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Text(
                                            region.name.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.semantics { heading() },
                                        )
                                    }
                                    SelaGhostButton(
                                        text = "Hapus",
                                        onClick = { viewModel.delete(region.id) },
                                        destructive = true,
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
