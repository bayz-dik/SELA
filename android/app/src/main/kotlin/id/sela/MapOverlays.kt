package id.sela

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.sela.feature.map.MapViewModel
import id.sela.model.EtaPredictor
import id.sela.network.TransitPreferences
import id.sela.power.GoHomeFab
import id.sela.power.GoHomeHelper
import id.sela.voice.VoiceAction
import id.sela.voice.VoiceCommand
import id.sela.voice.VoiceInputButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MapOverlayEntryPoint {
    fun prefs(): TransitPreferences
}

/**
 * Tombol aksi melayang di atas peta (modul app, di luar feature:map):
 * - Suara (#10): perintah suara Bahasa Indonesia → cari rute / halte terdekat / pulang.
 * - Mini (#76): mode picture-in-picture.
 * - Pulang (#53): ETA ke halte rumah.
 */
@Composable
fun MapActionOverlays(viewModel: MapViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entry = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, MapOverlayEntryPoint::class.java)
    }

    fun handleVoice(text: String) {
        when (val action = VoiceCommand.parseCommand(text)) {
            is VoiceAction.ShowEta -> {
                val q = action.routeQuery.lowercase()
                val route = viewModel.uiState.value.routes.firstOrNull {
                    it.shortName.lowercase() == q || it.shortName.lowercase().contains(q) ||
                        it.longName.lowercase().contains(q)
                }
                if (route != null) viewModel.select("route", route.id)
                else viewModel.message("Rute \"$text\" tidak ditemukan.")
            }
            VoiceAction.NearestStop -> {
                val loc = lastLocation(context)
                val stops = viewModel.uiState.value.stops
                if (loc == null || stops.isEmpty()) {
                    viewModel.message("Lokasi belum tersedia untuk mencari halte terdekat.")
                } else {
                    val near = stops.minByOrNull {
                        EtaPredictor.haversineM(loc.first, loc.second, it.lat, it.lon)
                    }!!
                    viewModel.select("stop", near.id)
                }
            }
            VoiceAction.GoHome -> goHome(context, entry, viewModel, scope)
            VoiceAction.Unknown -> viewModel.message("Perintah tidak dikenali: \"$text\"")
        }
    }

    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoiceInputButton(onResult = ::handleVoice)
        IconButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    (context as? Activity)?.enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build(),
                    )
                }
            },
        ) {
            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Mode mini")
        }
        GoHomeFab(onClick = { goHome(context, entry, viewModel, scope) })
    }
}

private fun goHome(
    context: Context,
    entry: MapOverlayEntryPoint,
    viewModel: MapViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    scope.launch {
        val homeId = runCatching { entry.prefs().homeStopId.first() }.getOrDefault("")
        val stops = viewModel.uiState.value.stops
        val home = stops.firstOrNull { it.id == homeId }
            ?: stops.firstOrNull { s -> viewModel.uiState.value.data.favorites.any { it.kind == "stop" && it.id == s.id } }
        viewModel.message(GoHomeHelper.homeEtaText(home, viewModel.uiState.value.vehicles))
    }
}

@SuppressLint("MissingPermission")
private fun lastLocation(context: Context): Pair<Double, Double>? {
    return runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        for (p in providers) {
            val l = lm.getLastKnownLocation(p) ?: continue
            return Pair(l.latitude, l.longitude)
        }
        null
    }.getOrNull()
}
