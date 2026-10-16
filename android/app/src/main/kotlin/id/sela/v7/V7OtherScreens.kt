package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.SelaV7

/** Item menu generik V7. */
data class V7MenuItem(
    val label: String,
    val icon: ImageVector,
    val iconBg: Color,
    val trailing: String? = null,
    val onClick: () -> Unit = {},
)

@Composable
private fun V7MenuRow(item: V7MenuItem) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { item.onClick() }
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = SelaV7.ink,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (item.label == "Keluar") SelaV7.danger else SelaV7.ink,
                modifier = Modifier.weight(1f),
            )
            item.trailing?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, fontSize = 14.sp, color = SelaV7.inkFaint)
            }
            if (item.label != "Keluar") {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SelaV7.inkFaint, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SelaV7.hairline, modifier = Modifier.padding(start = 42.dp))
    }
}

/**
 * Lainnya V7 — sesuai 10_LAINNYA_ACUAN.png.
 * Daftar tautan + maskot mengintip di bawah.
 */
@Composable
fun V7OtherScreen(
    onNavigate: (String) -> Unit,
) {
    val items = listOf(
        V7MenuItem("Rute Favorit", Icons.Default.Favorite, Color(0xFFFFD080)) { onNavigate("favorit") },
        V7MenuItem("Notifikasi", Icons.Default.Notifications, Color(0xFFF5A8B8)) { onNavigate("notifikasi") },
        V7MenuItem("Unduhan Offline", Icons.Default.Download, Color(0xFFFFD080)) { onNavigate("peta-offline") },
        V7MenuItem("Peta Offline", Icons.Default.Map, Color(0xFF7FB8F0)) { onNavigate("peta-offline") },
        V7MenuItem("Info Layanan", Icons.Default.Info, Color(0xFF8FDFB2)) { },
        V7MenuItem("Bantuan", Icons.Default.Help, Color(0xFFF5A8B8)) { },
        V7MenuItem("Tentang SELA", Icons.Default.Info, Color(0xFF91A6FF)) { },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        ) {
            item {
                Text(
                    text = "Lainnya",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = SelaV7.ink,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(items) { V7MenuRow(it) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * Pengaturan V7 — sesuai 11_PENGATURAN_ACUAN.png.
 * Halaman terpisah tanpa bottom nav (diakses dari Lainnya/Profil/Drawer).
 */
@Composable
fun V7SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val items = listOf(
        V7MenuItem("Akun", Icons.Default.Person, Color(0xFFD8D8D8)) { onNavigate("profile") },
        V7MenuItem("Notifikasi", Icons.Default.Notifications, Color(0xFFD8D8D8)) { },
        V7MenuItem("Tampilan", Icons.Default.Palette, Color(0xFFD8D8D8), trailing = "Terang") { },
        V7MenuItem("Bahasa", Icons.Default.Language, Color(0xFFD8D8D8), trailing = "Indonesia") { },
        V7MenuItem("Unduhan & Penyimpanan", Icons.Default.Download, Color(0xFFD8D8D8)) { onNavigate("peta-offline") },
        V7MenuItem("Privasi & Keamanan", Icons.Default.Lock, Color(0xFFD8D8D8)) { },
        V7MenuItem("Tentang", Icons.Default.Info, Color(0xFFD8D8D8)) { },
        V7MenuItem("Keluar", Icons.Default.Logout, Color(0xFFE8696B)) { onLogout() },
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = SelaV7.ink)
            }
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SelaV7.ink,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(items) { V7MenuRow(it) }
        }
    }
}

/**
 * Profil V7 — sesuai 12_PROFIL_ACUAN.png.
 * Avatar maskot, nama, email, tombol Kelola Akun Google,
 * menu favorit/riwayat/playlist/unduhan, statistik.
 */
@Composable
fun V7ProfileScreen(
    onBack: () -> Unit,
    userName: String = "Pengguna SELA",
    userEmail: String = "pengguna@gmail.com",
    onNavigate: (String) -> Unit = {},
) {
    val menuItems = listOf(
        V7MenuItem("Rute Favorit", Icons.Default.Favorite, Color(0xFFF5A8B8)) { onNavigate("favorit") },
        V7MenuItem("Riwayat Perjalanan", Icons.Default.History, Color(0xFFFFD080)) { onNavigate("history") },
        V7MenuItem("Playlist Saya", Icons.Default.MusicNote, Color(0xFFB8A8F0)) { onNavigate("music") },
        V7MenuItem("Unduhan Offline", Icons.Default.Download, Color(0xFF91A6FF)) { onNavigate("peta-offline") },
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = SelaV7.ink)
                }
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SelaV7.surfaceDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = SelaV7.ink,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SelaV7.ink,
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = SelaV7.inkSoft,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SelaV7.surface,
                shadowElevation = 0.dp,
                modifier = Modifier.clickable { },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Black,
                        color = SelaV7.ink,
                    )
                    Text(
                        text = "Kelola Akun Google",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SelaV7.ink,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        items(menuItems) { V7MenuRow(it) }
        item {
            Spacer(Modifier.height(16.dp))
            // Statistik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                V7Stat("12", "Rute Disimpan")
                V7Stat("28", "Perjalanan")
                V7Stat("4", "Playlist")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun V7Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = SelaV7.ink,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SelaV7.inkSoft,
        )
    }
}

/**
 * Side Menu / Drawer V7 — sesuai 13_SIDE_MENU_ACUAN.png.
 * Panel sempit kanan: logo SELA, 6 halaman, toggle Mode Gelap.
 */
@Composable
fun V7DrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    darkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(SelaV7.background)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SELA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = SelaV7.ink,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SelaV7.ink)
            }
        }
        Spacer(Modifier.height(16.dp))

        val pages = listOf(
            Triple("home", "Beranda", Icons.Default.Home),
            Triple("map", "Peta", Icons.Default.Map),
            Triple("trip-planner", "Rute", Icons.Default.Route),
            Triple("music", "Musik", Icons.Default.MusicNote),
            Triple("konduktor", "Tanya", Icons.Default.Chat),
            Triple("lainnya", "Lainnya", Icons.Default.MoreHoriz),
        )
        pages.forEach { (route, label, icon) ->
            val active = currentRoute == route
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) SelaV7.limeSoft else Color.Transparent)
                    .clickable { onNavigate(route); onClose() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, tint = SelaV7.ink, modifier = Modifier.size(22.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = SelaV7.ink,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Toggle Mode Gelap
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SelaV7.surface,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = SelaV7.ink)
                    Text("Mode Gelap", style = MaterialTheme.typography.bodyMedium, color = SelaV7.ink)
                }
                Switch(
                    checked = darkMode,
                    onCheckedChange = onDarkModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SelaV7.lime,
                        checkedTrackColor = SelaV7.limeSoft,
                    ),
                )
            }
        }
    }
}
