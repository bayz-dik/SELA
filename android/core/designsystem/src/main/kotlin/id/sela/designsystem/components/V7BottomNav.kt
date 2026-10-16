package id.sela.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaV7

/** Satu destinasi bottom nav. */
data class V7NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** 5 destinasi utama — Beranda, Peta, Musik, Tanya, Lainnya. */
val V7Destinations = listOf(
    V7NavDestination("home", "Beranda", Icons.Default.Home),
    V7NavDestination("map", "Peta", Icons.Default.Map),
    V7NavDestination("music", "Musik", Icons.Default.LibraryMusic),
    V7NavDestination("konduktor", "Tanya", Icons.Default.Chat),
    V7NavDestination("lainnya", "Lainnya", Icons.Default.MoreHoriz),
)

/**
 * Bottom navigation x Threads — dark mode minimal.
 * Bar hitam + hairline divider di atas, 5 ikon tanpa label.
 * Tab aktif: putih; non-aktif: abu. NOL shadow.
 */
@Composable
fun V7BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = SelaV7.hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SelaV7.background)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            V7Destinations.forEach { dest ->
                val active = currentRoute == dest.route ||
                    (dest.route == "lainnya" && currentRoute in listOf("settings", "history", "peta-offline"))
                Box(
                    modifier = Modifier
                        .clickable(role = Role.Tab) { onNavigate(dest.route) }
                        .semantics { selected = active }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        dest.icon,
                        contentDescription = dest.label,
                        modifier = Modifier.size(26.dp),
                        tint = if (active) SelaV7.ink else SelaV7.inkFaint,
                    )
                }
            }
        }
    }
}
