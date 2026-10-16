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
import id.sela.feature.map.MapUiState
import id.sela.model.ConnectionStatus

/**
 * Beranda x Threads — dark mode.
 * Header minimal, search bar, shortcut ikon monokrom, feed + divider.
 * Sesuai Figma "SELA x Threads" 04 Beranda. Tanpa maskot, NOL shadow.
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
            .background(SelaV7.background),
    ) {
        Spacer(Modifier.height(16.dp))

        // Header: sapaan + bell (tanpa avatar maskot)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Hai,",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 14.sp,
                    color = SelaV7.inkSoft,
                )
                Text(
                    text = if (userName != null) "Selamat datang, $userName!" else "Selamat datang!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = SelaV7.ink,
                )
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

        // Search bar — surfaceDim, pill, tanpa shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SelaV7.surfaceDim)
                .clickable { onSearchClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SelaV7.inkFaint,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Mau ke mana hari ini?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    color = SelaV7.inkFaint,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 6 shortcut — ikon monokrom dalam lingkaran surfaceDim
        val shortcuts = listOf(
            Triple("Peta", Icons.Default.Map) to onOpenMap,
            Triple("Rute", Icons.Default.Route) to onPlanTrip,
            Triple("Musik", Icons.Default.MusicNote) to onOpenMusic,
            Triple("Tanya", Icons.Default.Chat) to onOpenAssistant,
            Triple("Favorit", Icons.Default.Favorite) to onOpenFavorites,
            Triple("Lainnya", Icons.Default.MoreHoriz) to onOpenOther,
        )
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            shortcuts.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { (info, onClick) ->
                        val (label, icon) = info
                        V7Shortcut(label = label, icon = icon, onClick = onClick)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(thickness = 1.dp, color = SelaV7.hairline)

        // Status live sebagai feed item (bukan banner)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SelaV7.surfaceDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = SelaV7.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bus Live",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SelaV7.ink,
                )
                Text(
                    text = if (isLive) "$activeBusCount bus terpantau" else "Menghubungkan...",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = SelaV7.inkSoft,
                )
            }
            if (isLive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SelaV7.greenOnline),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SelaV7.hairline)
    }
}

@Composable
private fun V7Shortcut(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SelaV7.surfaceDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = SelaV7.ink,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = SelaV7.ink,
        )
    }
}
