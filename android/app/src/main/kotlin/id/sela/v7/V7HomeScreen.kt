package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.SelaV7
import id.sela.designsystem.components.V7Mascot
import id.sela.feature.map.MapUiState
import id.sela.model.ConnectionStatus

/**
 * Beranda V7 — sesuai 04_HOME_ACUAN.png.
 * Header maskot + sapaan, search bar, 6 shortcut melingkar (2 baris),
 * banner eksplorasi dengan maskot.
 *
 * Data bus live dari MapUiState aktual — bukan angka demo.
 */
@Composable
fun V7HomeScreen(
    state: MapUiState,
    userName: String? = null,
    onOpenMap: () -> Unit,
    onPlanTrip: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenOther: () -> Unit,
    onSearchClick: () -> Unit = onOpenMap,
) {
    val isLive = state.connectionStatus == ConnectionStatus.LIVE
    val activeBusCount = state.vehicles.count { !it.isStale }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // Header: avatar maskot + sapaan + bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SelaV7.limeSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    V7Mascot(size = 40.dp)
                }
                Column {
                    Text(
                        text = "Hai",
                        style = MaterialTheme.typography.labelMedium,
                        color = SelaV7.inkSoft,
                    )
                    Text(
                        text = if (userName != null) "Selamat datang, $userName!" else "Selamat datang!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SelaV7.ink,
                    )
                }
            }
            IconButton(onClick = { /* notifikasi */ }) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = SelaV7.ink,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Search bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .clickable { onSearchClick() },
            color = SelaV7.surface,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(28.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SelaV7.inkFaint,
                )
                Text(
                    text = "Mau ke mana hari ini?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaV7.inkFaint,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 6 shortcut melingkar — 2 baris x 3 kolom
        val shortcuts = listOf(
            Triple("Peta", Icons.Default.Map, SelaV7.shortcutMap) to onOpenMap,
            Triple("Rute", Icons.Default.Route, SelaV7.shortcutRoute) to onPlanTrip,
            Triple("Musik", Icons.Default.MusicNote, SelaV7.shortcutMusic) to onOpenMusic,
            Triple("Tanya", Icons.Default.Chat, SelaV7.shortcutAsk) to onOpenAssistant,
            Triple("Favorit", Icons.Default.Favorite, SelaV7.shortcutFav) to onOpenFavorites,
            Triple("Lainnya", Icons.Default.MoreHoriz, SelaV7.shortcutMore) to onOpenOther,
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            shortcuts.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { (info, onClick) ->
                        val (label, icon, bg) = info
                        V7Shortcut(label = label, icon = icon, bgColor = bg, onClick = onClick)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Banner eksplorasi
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SelaV7.limeSoft,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    V7Mascot(size = 64.dp)
                    Text(
                        text = "Jelajahi kota\ndengan lebih mudah",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SelaV7.ink,
                        lineHeight = 20.sp,
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Buka peta",
                    tint = SelaV7.ink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenMap() }
                        .padding(8.dp),
                )
            }
        }

        // Status live (data aktual)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isLive) "$activeBusCount bus terpantau live" else "Menghubungkan ke data live...",
            style = MaterialTheme.typography.labelMedium,
            color = SelaV7.inkFaint,
        )
    }
}

@Composable
private fun V7Shortcut(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = SelaV7.ink,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = SelaV7.ink,
        )
    }
}
