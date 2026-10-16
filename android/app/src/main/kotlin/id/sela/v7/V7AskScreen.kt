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
import id.sela.assistant.ChatGptAuth
import id.sela.assistant.KonduktorAi
import id.sela.assistant.SelaAiClient
import id.sela.designsystem.SelaV7
import kotlinx.coroutines.launch

/** Satu pesan chat. */
data class V7ChatMessage(
    val text: String,
    val fromUser: Boolean,
)

/**
 * Tanya SELA V7 — sesuai 09_TANYA_ACUAN.png.
 * Header avatar maskot, bubble chat (krem masuk / periwinkle keluar),
 * pemilih model AI, input di bawah dengan tombol kirim.
 *
 * Terhubung ke SelaAiClient (ChatGPT via akun pengguna). Jika belum login,
 * pakai jawaban rule-based offline (KonduktorAi) yang jujur menyebut mode offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V7AskScreen() {
    var messages by remember {
        mutableStateOf(listOf(V7ChatMessage("Halo! Ada yang bisa saya bantu?", false)))
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val authState by ChatGptAuth.authState.collectAsState()
    val loggedIn = authState is ChatGptAuth.AuthState.LoggedIn

    // Pemilih model — dinamis dari akun, fallback statis
    var selectedModel by remember { mutableStateOf(SelaAiClient.DEFAULT_MODEL) }
    var modelOptions by remember { mutableStateOf(SelaAiClient.MODELS) }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var modelsLoading by remember { mutableStateOf(false) }
    LaunchedEffect(loggedIn) {
        if (loggedIn && !modelsLoading) {
            modelsLoading = true
            modelOptions = SelaAiClient.availableModels()
            if (selectedModel !in modelOptions) {
                selectedModel = modelOptions.firstOrNull() ?: SelaAiClient.DEFAULT_MODEL
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending) return
        sending = true
        input = ""
        scope.launch {
            messages = messages + V7ChatMessage(text, true)
            val reply = if (loggedIn) {
                when (val r = SelaAiClient.ask(text, selectedModel)) {
                    is SelaAiClient.AiResult.Ok -> r.text
                    is SelaAiClient.AiResult.NeedLogin ->
                        "Sesi ChatGPT berakhir. Login ulang ya."
                    is SelaAiClient.AiResult.Error -> r.message
                }
            } else {
                // Mode offline yang jujur — jawaban rule-based lokal
                KonduktorAi.reply(text) +
                    "\n\n(Mode offline — login ChatGPT untuk jawaban AI.)"
            }
            messages = messages + V7ChatMessage(reply, false)
            sending = false
        }
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
                    .background(SelaV7.surfaceDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = SelaV7.ink,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tanya SELA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SelaV7.ink,
                )
                Text(
                    text = if (loggedIn) "AI terhubung" else "Mode offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (loggedIn) SelaV7.success else SelaV7.inkSoft,
                )
            }
            // Pemilih model (hanya saat login)
            if (loggedIn) {
                ExposedDropdownMenuBox(
                    expanded = modelMenuOpen,
                    onExpandedChange = { modelMenuOpen = !modelMenuOpen },
                ) {
                    OutlinedButton(
                        onClick = { modelMenuOpen = !modelMenuOpen },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = selectedModel,
                            style = MaterialTheme.typography.labelSmall,
                            color = SelaV7.ink,
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = modelMenuOpen,
                        onDismissRequest = { modelMenuOpen = false },
                    ) {
                        modelOptions.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model, color = SelaV7.ink) },
                                onClick = {
                                    selectedModel = model
                                    modelMenuOpen = false
                                },
                            )
                        }
                    }
                }
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
            if (sending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SelaV7.surfaceDim),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = SelaV7.ink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SelaV7.limeDeep,
                            strokeWidth = 2.dp,
                        )
                    }
                }
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
                IconButton(onClick = ::send, enabled = !sending) {
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
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SelaV7.surfaceDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = SelaV7.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
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
