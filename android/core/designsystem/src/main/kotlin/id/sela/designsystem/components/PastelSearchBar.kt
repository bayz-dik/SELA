package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Bilah pencarian pastel — pil kaca putih dengan ikon kaca pembesar.
 * Melayang di atas peta; fokus mengembang lembut.
 */
@Composable
fun PastelSearchBar(
    value: String,
    onChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari halte atau rute…",
) {
    Surface(
        shape = SelaPastelShapes.pill,
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
        modifier = modifier
            .fillMaxWidth()
            .pastelShadow(elevation = SelaPastelShadows.card, shape = SelaPastelShapes.pill),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = SelaPastel.blueDeep,
                modifier = Modifier.size(22.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp)
                    .onFocusChanged { onFocus(it.isFocused) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = SelaPastel.ink),
                cursorBrush = SolidColor(SelaPastel.blueDeep),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onFocus(false) }),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SelaPastel.inkFaint,
                        )
                    }
                    inner()
                },
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Hapus pencarian",
                        tint = SelaPastel.inkSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
