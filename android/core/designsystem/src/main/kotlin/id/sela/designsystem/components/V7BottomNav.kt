package id.sela.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaV7

/** Satu destinasi bottom nav V7. */
data class V7NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** 5 destinasi utama V7 — sesuai REFERENCE_CROPS. Rute via shortcut & drawer. */
val V7Destinations = listOf(
    V7NavDestination("home", "Beranda", Icons.Default.Home),
    V7NavDestination("map", "Peta", Icons.Default.Map),
    V7NavDestination("music", "Musik", Icons.Default.LibraryMusic),
    V7NavDestination("konduktor", "Tanya", Icons.Default.Chat),
    V7NavDestination("lainnya", "Lainnya", Icons.Default.MoreHoriz),
)

/**
 * Bottom navigation V7 — bar krem sederhana, 5 tab.
 * Tab aktif: ikon navy bold + label navy; non-aktif: abu.
 * Sesuai 04_HOME_ACUAN.png, 05_PETA_ACUAN.png, 08_MUSIC_ACUAN.png, dst.
 */
@Composable
fun V7BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SelaV7.background,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            V7Destinations.forEach { dest ->
                val active = currentRoute == dest.route ||
                    (dest.route == "lainnya" && currentRoute in listOf("settings", "history", "peta-offline"))
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab) { onNavigate(dest.route) }
                        .semantics { selected = active }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        dest.icon,
                        contentDescription = dest.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (active) SelaV7.ink else SelaV7.inkFaint,
                    )
                    Text(
                        dest.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) SelaV7.ink else SelaV7.inkFaint,
                    )
                }
            }
        }
    }
}
