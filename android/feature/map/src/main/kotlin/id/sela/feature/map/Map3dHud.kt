package id.sela.feature.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaPalette

/**
 * HUD khusus mode 3D (§9 spec): toggle 2D/3D, Free/Follow, recenter, reset kompas.
 * Toggle style gelap ↔ satelit (B20) tampil di semua mode. 2D tetap tidak berubah.
 */
@Composable
fun Map3dHud(
    mode: MapMode,
    onEnter3d: () -> Unit,
    onExit3d: () -> Unit,
    onToggleFollow: () -> Unit,
    onRecenter: () -> Unit,
    onResetBearing: () -> Unit,
    /** B20: toggle style peta gelap ↔ satelit. */
    onToggleStyle: () -> Unit = {},
    /** B20: true bila style satelit sedang aktif. */
    isSatellite: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // B20: toggle style gelap ↔ satelit — tampil di semua mode, tombol existing tidak dihapus.
        FilledTonalButton(onClick = onToggleStyle) {
            Icon(
                if (isSatellite) Icons.Filled.DarkMode else Icons.Filled.Satellite,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(if (isSatellite) "Gelap" else "Satelit")
        }
        if (!mode.is3d) {
            // Tombol masuk 3D — satu-satunya elemen HUD yang tampil di 2D.
            Button(
                onClick = onEnter3d,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SelaPalette.volt,
                    contentColor = SelaPalette.onVolt,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text("3D", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // Mode 3D aktif: keluar + kontrol kamera.
            val following = mode == MapMode.MAP_3D_FOLLOW
            FilledTonalButton(onClick = onToggleFollow) {
                Icon(
                    if (following) Icons.Filled.Navigation else Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (following) "Mengikuti" else "Bebas")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallFloatingActionButton(
                    onClick = onResetBearing,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Filled.Explore, contentDescription = "Reset kompas ke utara")
                }
                SmallFloatingActionButton(
                    onClick = onRecenter,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Pusatkan ke lokasiku")
                }
            }
            Button(
                onClick = onExit3d,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SelaPalette.volt,
                    contentColor = SelaPalette.onVolt,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text("2D", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
