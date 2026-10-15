package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SelaCompanionHeroCard(
    title: String,
    body: String,
    expression: SelaMascotExpression,
    modifier: Modifier = Modifier,
    bodyColor: Color = SelaPastel.blue,
    eyebrow: String? = null,
    stats: List<SelaHeroStat> = emptyList(),
) {
    PastelCard(
        modifier = modifier.fillMaxWidth(),
        tint = bodyColor.copy(alpha = 0.48f),
        shadowTint = bodyColor.deepShadow(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SelaMascot(
                    expression = expression,
                    diameter = 76.dp,
                    bodyColor = bodyColor,
                    contentDescription = title,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    eyebrow?.let {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = SelaPastel.blueDeep)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = SelaPastel.ink,
                    )
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.inkSoft)
                }
            }
            if (stats.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.take(3).forEach { stat ->
                        SelaHeroStatChip(stat = stat, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class SelaHeroStat(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
    val deep: Color,
)

@Composable
fun SelaHeroStatChip(stat: SelaHeroStat, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = SelaPastelShapes.md,
        color = Color.White.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = stat.tint, modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(stat.icon, contentDescription = null, tint = stat.deep, modifier = Modifier.size(17.dp))
                }
            }
            Text(stat.value, style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
            Text(stat.label, style = MaterialTheme.typography.labelSmall, color = SelaPastel.inkSoft)
        }
    }
}

@Preview
@Composable
private fun PreviewCompanionCard() {
    Surface(color = SelaPastel.background) {
        SelaCompanionHeroCard(
            modifier = Modifier.padding(16.dp).widthIn(max = 520.dp),
            eyebrow = "SELA transit companion",
            title = "Bus live lagi jalan",
            body = "Aku bantu ringkas kondisi jaringan TransJakarta biar kamu cepat ambil keputusan.",
            expression = SelaMascotExpression.BusApproaching,
            bodyColor = SelaPastel.mint,
            stats = listOf(
                SelaHeroStat("Bus live", "24", Icons.Default.DirectionsBus, SelaPastel.mintSoft, SelaPastel.mintDeep),
                SelaHeroStat("Rute", "13", Icons.Default.Route, SelaPastel.lavenderSoft, SelaPastel.lavenderDeep),
                SelaHeroStat("Halte", "220", Icons.Default.LocationOn, SelaPastel.blueSoft, SelaPastel.blueDeep),
            ),
        )
    }
}

private fun Color.deepShadow(): Color = Color(red * 0.88f, green * 0.88f, blue * 0.88f, 1f)
