package id.sela.sos

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import id.sela.designsystem.PastelCard
import id.sela.designsystem.PastelSwitch
import id.sela.designsystem.PastelTextButton
import id.sela.designsystem.SelaButton
import id.sela.designsystem.SelaButtonStyle
import id.sela.designsystem.SelaMascot
import id.sela.designsystem.SelaMascotExpression
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.SelaPastelShadows
import id.sela.designsystem.SelaPastelShapes
import id.sela.designsystem.pastelShadow
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
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = SelaPastel.dangerDeep,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = { Text("Kirim SOS dalam $countdown detik", color = SelaPastel.ink) },
            text = {
                Text(
                    "Pesan darurat + lokasi akan dibuka di WhatsApp ke $contact. " +
                        "Tekan BATAL bila tidak sengaja.",
                    color = SelaPastel.inkSoft,
                )
            },
            confirmButton = {
                SelaButton(
                    text = "Kirim sekarang",
                    onClick = {
                        showConfirm = false
                        sendSos()
                    },
                    style = SelaButtonStyle.Soft,
                    accent = SelaPastel.danger,
                    onAccent = SelaPastel.dangerDeep,
                )
            },
            dismissButton = {
                PastelTextButton(text = "BATAL", onClick = { showConfirm = false }, destructive = true)
            },
            shape = SelaPastelShapes.xl,
            containerColor = Color.White,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaPastel.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelaMascot(
                expression = SelaMascotExpression.Happy,
                diameter = 56.dp,
                bodyColor = SelaPastel.danger.copy(alpha = 0.7f),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SOS Darurat", style = MaterialTheme.typography.headlineMedium, color = SelaPastel.ink)
                Text(
                    "Kirim lokasi + pesan darurat ke kontak terdekat lewat WhatsApp. " +
                        "Bukan SMS — butuh internet & WhatsApp terpasang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }
        }

        // Tombol SOS — lingkaran clay merah pastel, besar dan jelas.
        Surface(
            onClick = { showConfirm = true },
            shape = CircleShape,
            color = SelaPastel.danger,
            contentColor = Color.White,
            modifier = Modifier
                .size(180.dp)
                .pastelShadow(
                    elevation = SelaPastelShadows.floating,
                    shape = CircleShape,
                    color = SelaPastel.dangerDeep,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("SOS", style = MaterialTheme.typography.displayMedium, color = Color.White)
            }
        }

        PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
            Text("Kontak darurat (WhatsApp)", style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
            if (editingContact) {
                OutlinedTextField(
                    value = contactDraft,
                    onValueChange = { contactDraft = it },
                    label = { Text("Nomor HP, mis. 08123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = SelaPastelShapes.field,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SelaPastel.blueDeep,
                        unfocusedBorderColor = SelaPastel.hairline,
                        focusedLabelColor = SelaPastel.blueDeep,
                        unfocusedLabelColor = SelaPastel.inkSoft,
                        cursorColor = SelaPastel.blueDeep,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelaButton(
                        text = "Simpan",
                        onClick = {
                            viewModel.saveContact(contactDraft)
                            editingContact = false
                            error = null
                        },
                        style = SelaButtonStyle.Primary,
                    )
                    PastelTextButton(text = "Batal", onClick = { editingContact = false })
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (contact.isBlank()) "Belum diisi" else contact,
                        color = SelaPastel.ink,
                    )
                    PastelTextButton(
                        text = "Ubah",
                        onClick = {
                            contactDraft = contact
                            editingContact = true
                        },
                    )
                }
            }
        }

        PastelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SOS goyang 3x", style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
                    Text(
                        "Goyang HP 3x untuk buka dialog SOS",
                        style = MaterialTheme.typography.bodySmall,
                        color = SelaPastel.inkSoft,
                    )
                }
                PastelSwitch(checked = shakeEnabled, onToggle = { shakeEnabled = it })
            }
        }

        SelaButton(
            text = "Hubungi 112 (Darurat)",
            onClick = { runCatching { SosManager.call112(context) } },
            style = SelaButtonStyle.Soft,
            accent = SelaPastel.danger,
            onAccent = SelaPastel.dangerDeep,
            icon = Icons.Filled.Warning,
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Text(it, color = SelaPastel.dangerDeep, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
