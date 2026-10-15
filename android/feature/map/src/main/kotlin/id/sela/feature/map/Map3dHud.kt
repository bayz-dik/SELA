package id.sela.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaButton
import id.sela.designsystem.SelaButtonStyle
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.SelaPastelShadows
import id.sela.designsystem.SelaPastelShapes
import id.sela.designsystem.pastelShadow

/**
 * HUD khusus mode 3D (§9 spec): toggle 2D/3D, Free/Follow, recenter, reset kompas.
 * Toggle style gelap ↔ satelit (B20) tampil di semua mode. 2D tetap tidak berubah.
 * Visual: pil kaca pastel melayang.
 */

/** Satu tombol pil kaca HUD. */
@Composable
private fun HudGlassPill(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.pastelShadow(
            elevation = SelaPastelShadows.soft,
            shape = SelaPastelShapes.pill,
        ),
        shape = SelaPastelShapes.pill,
        color = if (accent) SelaPastel.blue.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.88f),
        contentColor = if (accent) SelaPastel.ink else SelaPastel.inkSoft,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Tombol aksi bundar kaca HUD. */
@Composable
private fun HudGlassCircle(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(52.dp)
            .pastelShadow(elevation = SelaPastelShadows.soft, shape = CircleShape),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = SelaPastel.inkSoft,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
    }
}

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
        HudGlassPill(
            text = if (isSatellite) "Gelap" else "Satelit",
            onClick = onToggleStyle,
            icon = if (isSatellite) Icons.Filled.DarkMode else Icons.Filled.Satellite,
        )
        if (!mode.is3d) {
            // Tombol masuk 3D — satu-satunya elemen HUD yang tampil di 2D.
            SelaButton(
                text = "3D",
                onClick = onEnter3d,
                style = SelaButtonStyle.Primary,
            )
        } else {
            // Mode 3D aktif: keluar + kontrol kamera.
            val following = mode == MapMode.MAP_3D_FOLLOW
            HudGlassPill(
                text = if (following) "Mengikuti" else "Bebas",
                onClick = onToggleFollow,
                icon = if (following) Icons.Filled.Navigation else Icons.Filled.MyLocation,
                accent = following,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HudGlassCircle(
                    onClick = onResetBearing,
                    icon = Icons.Filled.Explore,
                    label = "Reset kompas ke utara",
                )
                HudGlassCircle(
                    onClick = onRecenter,
                    icon = Icons.Filled.MyLocation,
                    label = "Pusatkan ke lokasiku",
                )
            }
            SelaButton(
                text = "2D",
                onClick = onExit3d,
                style = SelaButtonStyle.Primary,
            )
        }
    }
}
