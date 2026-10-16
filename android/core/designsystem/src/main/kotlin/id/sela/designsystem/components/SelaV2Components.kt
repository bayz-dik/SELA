package id.sela.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** SELA V2: signature sunrise haze; decorative, never covers touch targets. */
@Composable
fun SelaAuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(
        Brush.verticalGradient(listOf(Color(0xFFE9EEFF), Color(0xFFF7F2FF), SelaPastel.background),
            startY = 0f, endY = 1600f)
    ), content = content)
}

/** Expressive hero using the original Compose mascot, no asset or network requirement. */
@Composable
fun SelaHeroPanel(
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mascotExpression: SelaMascotExpression = SelaMascotExpression.Welcome,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFCEE2FF), Color(0xFFE6D8FF), Color(0xFFF8DCEB))))
    ) {
        // Small light blooms make the surface feel softer than a Material card.
        Box(Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-54).dp)
            .size(215.dp).background(Color.White.copy(alpha = .22f), CircleShape))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-55).dp, y = 50.dp)
            .size(160.dp).background(Color.White.copy(alpha = .17f), CircleShape))
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = .64f), shape = RoundedCornerShape(100.dp)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null,
                            tint = SelaPastel.lavenderDeep, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("TEMAN JALANMU", color = SelaPastel.ink, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = SelaPastel.ink, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(subtitle, color = Color(0xFF4D5478), style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp)
                }
                SelaMascot(
                    expression = mascotExpression,
                    diameter = 110.dp,
                    bodyColor = Color(0xFFAAEBCF),
                    contentDescription = "Teman perjalanan SELA",
                )
            }
            Spacer(Modifier.height(17.dp))
            Surface(onClick = onClick, color = SelaPastel.ink,
                contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(horizontal = 17.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(cta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SelaV2SectionTitle(title: String, eyebrow: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (eyebrow != null) Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, color = SelaPastel.lavenderDeep)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
            color = SelaPastel.ink)
    }
}

/** Both numbers and status text must come from live view state, never hard-coded demo values. */
@Composable
fun SelaMetricTile(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 160.dp),
        color = tint, shape = RoundedCornerShape(29.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = SelaPastel.inkSoft, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
                Surface(color = Color.White.copy(alpha = .72f), shape = CircleShape) {
                    Icon(icon, contentDescription = null, tint = SelaPastel.ink,
                        modifier = Modifier.padding(9.dp).size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(targetState = value, transitionSpec = {
                androidx.compose.animation.fadeIn(tween(220)) togetherWith
                    androidx.compose.animation.fadeOut(tween(150))
            }, label = "metric") { newValue ->
                Text(newValue, style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold, color = SelaPastel.ink)
            }
            Text(detail, style = MaterialTheme.typography.labelSmall,
                color = SelaPastel.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SelaTransitLine(
    badge: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SelaPastel.blueSoft,
) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(),
        color = Color.White, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF1EAF7))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(color = tint, shape = RoundedCornerShape(15.dp)) {
                Box(Modifier.sizeIn(minWidth = 54.dp, minHeight = 52.dp).padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center) {
                    Text(badge.take(7), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold, color = SelaPastel.ink,
                        maxLines = 1)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = SelaPastel.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null,
                modifier = Modifier.size(19.dp), tint = SelaPastel.lavenderDeep)
        }
    }
}

@Composable
fun SelaLivePill(connected: Boolean, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = if (connected) Color(0xFFDDF7E8) else Color(0xFFFFEBD9),
        shape = RoundedCornerShape(50.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(7.dp).background(if (connected) SelaPastel.mintDeep else SelaPastel.peachDeep, CircleShape))
            Text(label, style = MaterialTheme.typography.labelSmall, color = SelaPastel.ink,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

data class SelaDockDestination(val route: String, val label: String, val icon: ImageVector)

/**
 * Dock navigasi bawah SELA — pill mengambang, 6 destinasi.
 * Item aktif: pill penuh; item lain: ikon + label kecil.
 * Warna 100% dari token [SelaPastel].
 */
@Composable
fun SelaFloatingDock(
    destinations: List<SelaDockDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        color = SelaPastel.surface.copy(alpha = .98f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, SelaPastel.hairline),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val active = currentRoute == destination.route
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) SelaPastel.lavenderSoft else Color.Transparent)
                        .clickable(role = Role.Tab) { onNavigate(destination.route) }
                        .semantics { selected = active }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        modifier = Modifier.size(if (active) 23.dp else 21.dp),
                        tint = if (active) SelaPastel.lavenderDeep else SelaPastel.inkSoft,
                    )
                    Text(
                        destination.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) SelaPastel.ink else SelaPastel.inkFaint,
                    )
                }
            }
        }
    }
}

/** Latar halaman V2 — gradasi pastel halus dari token [SelaPastel]. */
fun Modifier.selaAuroraSurface(): Modifier = this.background(
    Brush.verticalGradient(
        listOf(SelaPastel.lavenderSoft, SelaPastel.pinkSoft, SelaPastel.background),
        startY = 0f,
        endY = 1550f,
    ),
)
