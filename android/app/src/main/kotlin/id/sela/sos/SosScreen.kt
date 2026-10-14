package id.sela.sos

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.model.TransitRepository
import id.sela.network.SelaPrefs
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Layar SOS darurat (fitur #21, #59, #123).
 *
 * Alur: tekan tombol SOS merah → dialog hitung mundur 5 detik (bisa batal) →
 * buka WhatsApp ke kontak darurat dengan pesan + lokasi. Tidak ada pengiriman
 * otomatis diam-diam: pengguna selalu melihat dialog konfirmasi dulu.
 *
 * WIRING: daftarkan rute nav "sos" dan tombol menuju ke sini (lihat pesan induk).
 */
@HiltViewModel
class SosViewModel @Inject constructor(
    private val prefs: SelaPrefs,
    repository: TransitRepository,
) : ViewModel() {
    val emergencyContact = prefs.emergencyContact
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    /** ID rute favorit pertama — dipakai sebagai konteks "naik koridor apa" di pesan SOS. */
    val routeInfo = repository.data
        .map { data -> data.favorites.firstOrNull { it.kind == "route" }?.id.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveContact(phone: String) {
        viewModelScope.launch { prefs.setEmergencyContact(phone) }
    }
}

@Composable
fun SosScreen(viewModel: SosViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val contact by viewModel.emergencyContact.collectAsState()
    val routeInfo by viewModel.routeInfo.collectAsState()

    var showConfirm by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(5) }
    var shakeEnabled by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf(false) }
    var contactDraft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var lastLocation by remember { mutableStateOf<Location?>(null) }
    DisposableEffect(appContext) {
        val manager = appContext.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { lastLocation = it }
        lastLocation = runCatching {
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
        runCatching {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, listener)
        }
        onDispose { runCatching { manager.removeUpdates(listener) } }
    }

    // SOS goyang 3x → buka dialog konfirmasi (tidak langsung kirim).
    val shakeDetector = remember {
        ShakeDetector { showConfirm = true }
    }
    DisposableEffect(shakeEnabled) {
        if (shakeEnabled) shakeDetector.start(appContext) else shakeDetector.stop()
        onDispose { shakeDetector.stop() }
    }

    fun sendSos() {
        if (contact.isBlank()) {
            editingContact = true
            contactDraft = ""
            error = "Isi nomor WhatsApp kontak darurat dulu."
            return
        }
        val loc = lastLocation
        val message = SosManager.sosMessage(
            contactName = "",
            lat = loc?.latitude ?: 0.0,
            lon = loc?.longitude ?: 0.0,
            routeInfo = if (routeInfo.isBlank()) "" else "koridor $routeInfo",
        )
        error = runCatching {
            SosManager.sendSosViaWhatsApp(context, contact, message)
            null
        }.getOrElse { "Gagal membuka WhatsApp: ${it.message}" }
    }

    // Hitung mundur 5 detik; selesai → kirim.
    LaunchedEffect(showConfirm) {
        if (!showConfirm) return@LaunchedEffect
        countdown = 5
        while (countdown > 0 && showConfirm) {
            delay(1000L)
            countdown--
        }
        if (showConfirm) {
            showConfirm = false
            sendSos()
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Red) },
            title = { Text("Kirim SOS dalam $countdown detik") },
            text = {
                Text(
                    "Pesan darurat + lokasi akan dibuka di WhatsApp ke $contact. " +
                        "Tekan BATAL bila tidak sengaja.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    sendSos()
                }) { Text("Kirim sekarang") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("BATAL") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SOS Darurat", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Kirim lokasi + pesan darurat ke kontak terdekat lewat WhatsApp. " +
                "Bukan SMS — butuh internet & WhatsApp terpasang.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = { showConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier.size(180.dp),
        ) {
            Text("SOS", style = MaterialTheme.typography.displayMedium, color = Color.White)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kontak darurat (WhatsApp)", style = MaterialTheme.typography.titleSmall)
                if (editingContact) {
                    OutlinedTextField(
                        value = contactDraft,
                        onValueChange = { contactDraft = it },
                        label = { Text("Nomor HP, mis. 08123456789") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.saveContact(contactDraft)
                            editingContact = false
                            error = null
                        }) { Text("Simpan") }
                        OutlinedButton(onClick = { editingContact = false }) { Text("Batal") }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (contact.isBlank()) "Belum diisi" else contact)
                        TextButton(onClick = {
                            contactDraft = contact
                            editingContact = true
                        }) { Text("Ubah") }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("SOS goyang 3x", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Goyang HP 3x untuk buka dialog SOS",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = shakeEnabled, onCheckedChange = { shakeEnabled = it })
        }

        OutlinedButton(
            onClick = { runCatching { SosManager.call112(context) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hubungi 112 (Darurat)")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
