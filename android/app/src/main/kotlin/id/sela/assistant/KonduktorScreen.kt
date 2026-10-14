package id.sela.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Satu baris chat di layar kondektur. */
data class ChatMsg(val text: String, val fromUser: Boolean)

/**
 * Layar chat kondektur SELA.
 *
 * - "Kirim": jawaban rule-based offline ([KonduktorAi]) — selalu tersedia.
 * - "🤖 Tanya Sela": jawaban AI via akun ChatGPT (OAuth). Perlu login sekali
 *   dan internet. Token tersimpan terenkripsi di HP.
 *
 * @param ctx konteks opsional untuk jawaban rule-based yang lebih relevan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KonduktorScreen(ctx: KonduktorCtx = KonduktorCtx()) {
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMsg("Halo! Aku Kondektur SELA. Tanya soal bus, yuk.", fromUser = false),
            ),
        )
    }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var sending by remember { mutableStateOf(false) }
    val authState by ChatGptAuth.authState.collectAsState()
    var selectedModel by remember { mutableStateOf(SelaAiClient.DEFAULT_MODEL) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kondektur SELA", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Mode offline selalu jalan. Login ChatGPT untuk jawaban AI.",
            style = MaterialTheme.typography.bodySmall,
        )
        // Pemilih model AI ala ChatGPT — langsung di atas chat
        if (authState is ChatGptAuth.AuthState.LoggedIn) {
            ExposedDropdownMenuBox(
                expanded = modelMenuOpen,
                onExpandedChange = { modelMenuOpen = !modelMenuOpen },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Model AI") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuOpen)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = modelMenuOpen,
                    onDismissRequest = { modelMenuOpen = false },
                ) {
                    SelaAiClient.MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                selectedModel = model
                                modelMenuOpen = false
                            },
                        )
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.fromUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    },
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.fromUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(0.85f),
                    ) {
                        Text(
                            if (msg.fromUser) msg.text else "🚏 ${msg.text}",
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tanya soal bus / halte...") },
                singleLine = true,
            )
            Button(
                onClick = {
                    val text = draft.trim()
                    if (text.isEmpty()) return@Button
                    messages = messages + ChatMsg(text, fromUser = true) +
                        ChatMsg(KonduktorAi.reply(text, ctx), fromUser = false)
                    draft = ""
                },
            ) {
                Text("Kirim")
            }
        }

        when (val s = authState) {
            is ChatGptAuth.AuthState.LoggedIn -> {
                // Tombol "Tanya Sela" — AI via ChatGPT langsung dari HP
                Button(
                    onClick = {
                        val text = draft.trim()
                        if (text.isEmpty() || sending) return@Button
                        sending = true
                        scope.launch {
                            messages = messages + ChatMsg(text, fromUser = true)
                            draft = ""
                            val reply = when (val r = SelaAiClient.ask(text, selectedModel)) {
                                is SelaAiClient.AiResult.Ok -> r.text
                                is SelaAiClient.AiResult.NeedLogin ->
                                    "Sesi ChatGPT berakhir. Login ulang ya."
                                is SelaAiClient.AiResult.Error -> r.message
                            }
                            messages = messages + ChatMsg(reply, fromUser = false)
                            sending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !sending,
                ) {
                    Text(if (sending) "Sela mikir..." else "🤖 Tanya Sela")
                }
                OutlinedButton(
                    onClick = {
                        ChatGptAuth.logout()
                        messages = messages + ChatMsg(
                            "Kamu keluar dari ChatGPT. Mode offline tetap jalan.",
                            fromUser = false,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("Keluar dari ChatGPT")
                }
            }
            is ChatGptAuth.AuthState.WaitingBrowser -> {
                Text(
                    "Selesaikan login di browser, lalu kembali ke sini ya.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            is ChatGptAuth.AuthState.Failed -> {
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedButton(
                    onClick = {
                        if (!ChatGptAuth.beginLogin(context)) {
                            scope.launch {
                                messages = messages + ChatMsg(
                                    "Gagal membuka browser. Coba lagi ya.",
                                    fromUser = false,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("Coba login lagi")
                }
            }
            is ChatGptAuth.AuthState.LoggedOut -> {
                OutlinedButton(
                    onClick = {
                        if (!ChatGptAuth.beginLogin(context)) {
                            scope.launch {
                                messages = messages + ChatMsg(
                                    "Gagal membuka browser. Coba lagi ya.",
                                    fromUser = false,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("🔑 Login ChatGPT untuk AI")
                }
            }
        }
    }
}
