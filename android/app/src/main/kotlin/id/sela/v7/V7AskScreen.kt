package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaV7
import id.sela.designsystem.components.V7Mascot

/** Satu pesan chat. */
data class V7ChatMessage(
    val text: String,
    val fromUser: Boolean,
)

/**
 * Tanya SELA V7 — sesuai 09_TANYA_ACUAN.png.
 * Header avatar maskot, bubble chat (krem masuk / periwinkle keluar),
 * input di bawah dengan tombol kirim.
 *
 * TODO: hubungkan ke SelaAiLogic / model AI yang sudah ada.
 */
@Composable
fun V7AskScreen(
    messages: List<V7ChatMessage> = listOf(
        V7ChatMessage("Halo! Ada yang bisa saya bantu?", false),
    ),
    onSend: (String) -> Unit = {},
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SelaV7.periSoft),
                contentAlignment = Alignment.Center,
            ) {
                V7Mascot(size = 40.dp)
            }
            Column {
                Text(
                    text = "Tanya SELA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SelaV7.ink,
                )
                Text(
                    text = "Tanya apa saja tentang transportasi, rute, atau kota ini",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.inkSoft,
                )
            }
        }

        // Daftar pesan
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { msg ->
                V7ChatBubble(message = msg)
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ketik pertanyaan...", color = SelaV7.inkFaint) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SelaV7.limeDeep,
                    unfocusedBorderColor = SelaV7.hairline,
                    focusedContainerColor = SelaV7.surface,
                    unfocusedContainerColor = SelaV7.surface,
                ),
                maxLines = 3,
            )
            Surface(
                shape = CircleShape,
                color = SelaV7.ink,
                modifier = Modifier.size(52.dp),
            ) {
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSend(input.trim())
                            input = ""
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun V7ChatBubble(message: V7ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!message.fromUser) {
            V7Mascot(size = 36.dp, modifier = Modifier.padding(end = 6.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (message.fromUser) 18.dp else 4.dp,
                bottomEnd = if (message.fromUser) 4.dp else 18.dp,
            ),
            color = if (message.fromUser) SelaV7.chatOutgoing else SelaV7.chatIncoming,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = SelaV7.ink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
