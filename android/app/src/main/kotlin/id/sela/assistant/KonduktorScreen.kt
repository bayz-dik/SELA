package id.sela.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.sela.designsystem.PastelNoticeBanner
import id.sela.designsystem.PastelNoticeTone
import id.sela.designsystem.PastelTextButton
import id.sela.designsystem.SelaButton
import id.sela.designsystem.SelaButtonStyle
import id.sela.designsystem.SelaMascot
import id.sela.designsystem.SelaMascotExpression
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.SelaPastelGradients
import id.sela.designsystem.SelaPastelShadows
import id.sela.designsystem.SelaPastelShapes
import id.sela.designsystem.auroraBackground
import id.sela.designsystem.pastelShadow
import kotlinx.coroutines.launch

/** Satu baris chat di layar kondektur. */
data class ChatMsg(val text: String, val fromUser: Boolean)

/** Gelembung chat pastel — user = aurora kanan, Sela = putih kiri + maskot. */
@Composable
private fun ChatBubble(msg: ChatMsg) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!msg.fromUser) {
            SelaMascot(
                expression = SelaMascotExpression.Happy,
                diameter = 36.dp,
                bodyColor = SelaPastel.mint,
                animate = false,
            )
            Spacer(Modifier.width(8.dp))
        }
        val bubbleShape = if (msg.fromUser) {
            RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
        } else {
            RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .pastelShadow(elevation = SelaPastelShadows.soft, shape = bubbleShape)
                .clip(bubbleShape)
                .background(
                    if (msg.fromUser) SelaPastelGradients.auroraBlueLavender
                    else Brush.linearGradient(listOf(Color.White, Color.White)),
                )
                .padding(14.dp),
        ) {
            Text(
                if (msg.fromUser) msg.text else "🚏 ${msg.text}",
                style = MaterialTheme.typography.bodyMedium,
                color = SelaPastel.ink,
            )
        }
    }
}

/** Gelembung "Sela lagi mikir" — maskot Thinking berdenyut. */
@Composable
private fun ThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelaMascot(
            expression = SelaMascotExpression.Thinking,
            diameter = 44.dp,
            bodyColor = SelaPastel.lavender,
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color = Color.White.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            modifier = Modifier.pastelShadow(
                elevation = SelaPastelShadows.soft,
                shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            ),
        ) {
            Text(
                "Sela lagi mikir…",
                style = MaterialTheme.typography.bodyMedium,
                color = SelaPastel.inkSoft,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

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

    fun sendOffline() {
        val text = draft.trim()
        if (text.isEmpty()) return
        messages = messages + ChatMsg(text, fromUser = true) +
            ChatMsg(KonduktorAi.reply(text, ctx), fromUser = false)
        draft = ""
    }

    fun askAi() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .auroraBackground()
            .padding(16.dp),
    ) {
        // Kepala — maskot + judul
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelaMascot(
                expression = SelaMascotExpression.Happy,
                diameter = 56.dp,
                bodyColor = SelaPastel.mint,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Kondektur SELA", style = MaterialTheme.typography.headlineSmall, color = SelaPastel.ink)
                Text(
                    "Mode offline selalu jalan. Login ChatGPT untuk jawaban AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }
        }

        // Pemilih model AI ala ChatGPT — langsung di atas chat
        if (authState is ChatGptAuth.AuthState.LoggedIn) {
            ExposedDropdownMenuBox(
                expanded = modelMenuOpen,
                onExpandedChange = { modelMenuOpen = !modelMenuOpen },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Model AI") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuOpen)
                    },
                    shape = SelaPastelShapes.field,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SelaPastel.lavenderDeep,
                        unfocusedBorderColor = SelaPastel.hairline,
                        focusedLabelColor = SelaPastel.lavenderDeep,
                        unfocusedLabelColor = SelaPastel.inkSoft,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = modelMenuOpen,
                    onDismissRequest = { modelMenuOpen = false },
                ) {
                    SelaAiClient.MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, color = SelaPastel.ink) },
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
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (sending) {
                item { ThinkingBubble() }
            }
        }

        // Kolom input pastel + tombol kirim aurora
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tanya soal bus / halte...") },
                singleLine = true,
                shape = SelaPastelShapes.pill,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SelaPastel.blueDeep,
                    unfocusedBorderColor = SelaPastel.hairline,
                    cursorColor = SelaPastel.blueDeep,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )
            Surface(
                onClick = ::sendOffline,
                shape = CircleShape,
                modifier = Modifier
                    .size(54.dp)
                    .pastelShadow(elevation = SelaPastelShadows.card, shape = CircleShape),
            ) {
                Box(
                    modifier = Modifier.background(SelaPastelGradients.auroraBlueLavender),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Kirim",
                        tint = SelaPastel.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        when (val s = authState) {
            is ChatGptAuth.AuthState.LoggedIn -> {
                // Tombol "Tanya Sela" — AI via ChatGPT langsung dari HP
                SelaButton(
                    text = if (sending) "Sela mikir..." else "🤖 Tanya Sela",
                    onClick = ::askAi,
                    style = SelaButtonStyle.Primary,
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    PastelTextButton(
                        text = "Keluar dari ChatGPT",
                        onClick = {
                            ChatGptAuth.logout()
                            messages = messages + ChatMsg(
                                "Kamu keluar dari ChatGPT. Mode offline tetap jalan.",
                                fromUser = false,
                            )
                        },
                    )
                }
            }
            is ChatGptAuth.AuthState.WaitingBrowser -> {
                PastelNoticeBanner(
                    text = "Selesaikan login di browser, lalu kembali ke sini ya.",
                    icon = Icons.Default.Info,
                    tone = PastelNoticeTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
            is ChatGptAuth.AuthState.Failed -> {
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.dangerDeep,
                    modifier = Modifier.padding(top = 12.dp),
                )
                SelaButton(
                    text = "Coba login lagi",
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
                    style = SelaButtonStyle.Soft,
                    accent = SelaPastel.danger,
                    onAccent = SelaPastel.dangerDeep,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            is ChatGptAuth.AuthState.LoggedOut -> {
                SelaButton(
                    text = "🔑 Login ChatGPT untuk AI",
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
                    style = SelaButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
