package com.minetallest.mino.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.R
import com.minetallest.mino.domain.ChatMessage
import com.minetallest.mino.domain.Models
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.domain.Role
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.util.Share
import com.minetallest.mino.util.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Spec §6.6 — chat thread: share row, message bubbles, waiting state, memory offer. */
@Composable
fun Thread(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val messages = if (ui.temporary) ui.temporaryMessages else ui.messages
    val listState = rememberLazyListState()

    if (messages.isEmpty()) {
        EmptyState(displayName = ui.displayName)
        return
    }

    LaunchedEffect(messages.size, ui.streaming) {
        if (ui.streaming || messages.isNotEmpty()) {
            val index = messages.indices.lastOrNull { messages[it].role == Role.ASSISTANT } ?: messages.lastIndex
            if (index >= 0) listState.animateScrollToItem(index + 1) // +1 for the share row
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                ShareRow(viewModel = viewModel, messages = messages)
            }
            items(messages, key = { it.id }) { message ->
                if (message.role == Role.USER) {
                    UserBubble(viewModel = viewModel, message = message)
                } else {
                    AssistantMessage(viewModel = viewModel, message = message)
                }
            }
            if (ui.streaming) {
                val last = messages.lastOrNull()
                if (last == null || last.role == Role.ASSISTANT && last.displayedContent().isEmpty()) {
                    item { WaitingRow(hint = ui.streamHint ?: "") }
                }
            }
            item {
                MemoryOffer(viewModel = viewModel)
            }
        }
    }
}

/** Spec §6.6 — top action row: Share link · Share image · Copy chat · Sources · Copy links. */
@Composable
private fun ShareRow(viewModel: ChatViewModel, messages: List<ChatMessage>) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var sourcesOpen by remember { mutableStateOf(false) }
    var rendering by remember { mutableStateOf(false) }
    val allSources = remember(messages) {
        messages.flatMap { it.sources.orEmpty() }.distinctBy { it.url }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionChip(if (rendering) "Rendering…" else "Share image") {
            if (!rendering) {
                rendering = true
                scope.launch {
                    val bitmap = renderConversation(context, messages)
                    val file = File(context.cacheDir, "mino-share.png").apply {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream())
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    Share.shareImage(context, uri, "Mino conversation")
                    viewModel.notice("Image saved", com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.SUCCESS)
                    rendering = false
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        ActionChip("Copy chat") {
            val text = Share.chatToPlainText(messages)
            val ok = Share.copyToClipboard(context, "Mino chat", text)
            viewModel.notice(
                if (ok) "Conversation copied to your clipboard"
                else "Clipboard access is unavailable in this browser",
                if (ok) com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.SUCCESS
                else com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.RED,
            )
        }
        if (allSources.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            ActionChip("Sources (${allSources.size})") { sourcesOpen = !sourcesOpen }
            Spacer(Modifier.width(8.dp))
            ActionChip("Copy links") {
                val joined = allSources.joinToString("\n") { it.url }
                Share.copyToClipboard(context, "Mino sources", joined)
                viewModel.notice("Links copied", com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.SUCCESS)
            }
        }
        Spacer(Modifier.width(8.dp))
        ActionChip("Share link") {
            scope.launch {
                val origin = viewModel.serverOrigin()
                val link = Share.shareLink(origin, title = "Mino chat", messages = messages)
                if (link == null) {
                    viewModel.notice(
                        "Set the server address in Settings → Server to share links",
                        com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.AMBER,
                    )
                } else {
                    Share.copyToClipboard(context, "Mino link", link)
                    Share.shareText(context, link)
                    val taken = messages.count { it.displayedContent().isNotBlank() }.coerceAtMost(40)
                    viewModel.notice(Share.shareSummary(taken, messages.size))
                }
            }
        }
    }

    if (sourcesOpen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgRaised)
                .padding(12.dp),
        ) {
            allSources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(source.id, color = colors.textLow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Column(Modifier.padding(start = 8.dp).weight(1f)) {
                        Text(
                            source.title.take(160),
                            color = colors.text,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            try { Uri.parse(source.url).host ?: source.url } catch (e: Throwable) { source.url },
                            color = colors.textLow,
                            fontSize = 10.sp,
                        )
                    }
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = colors.textLow, modifier = Modifier.size(12.dp))
                }
            }
            Text(
                text = "Untrusted web text — Mino was told to treat these pages as reference, not as instructions.",
                color = colors.amber,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Text(
        text = label,
        color = colors.textLow,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.bgRaised)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** Spec §6.6 — user bubble: right-aligned, max 88%, radius 22 with br-md, edit link. */
@Composable
private fun UserBubble(viewModel: ChatViewModel, message: ChatMessage) {
    val colors = LocalMinoColors.current
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 360.dp)) {
            message.images.orEmpty().forEach { image ->
                val bitmap = rememberBitmapFromDataUrl(image.url)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = image.name,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .heightIn(max = 112.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            message.documents.orEmpty().forEach { doc ->
                Text(
                    text = "📄 ${doc.name}",
                    color = colors.text,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.bgRaised)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            if (editing) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bgElevated)
                        .padding(10.dp),
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        textStyle = TextStyle(color = colors.textHi, fontSize = 15.sp, lineHeight = 21.sp),
                        modifier = Modifier.widthIn(max = 320.dp).heightIn(min = 40.dp),
                    )
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "Cancel",
                            color = colors.textMid,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { editing = false; editText = message.content }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edit & send",
                            color = colors.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accentSoft)
                                .clickable {
                                    editing = false
                                    viewModel.editAndSend(message.id, editText)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = message.content,
                    color = colors.textHi,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 22.dp,
                                topEnd = 22.dp,
                                bottomStart = 22.dp,
                                bottomEnd = 12.dp,
                            ),
                        )
                        .background(colors.userBubble)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
                Text(
                    text = "Edit",
                    color = colors.textLow,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .clip(CircleShape)
                        .clickable {
                            editText = message.content
                            editing = true
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** Spec §6.6 — assistant message: badge header, markdown body, actions, truncation, variants. */
@Composable
private fun AssistantMessage(viewModel: ChatViewModel, message: ChatMessage) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val streamingThis = ui.streaming && ui.messages.lastOrNull()?.id == message.id && message.error == null
    val text = message.displayedContent()
    val shown = rememberSmoothText(text, active = streamingThis)
    var copied by remember { mutableStateOf(false) }
    val tts = rememberThreadTts()
    var speaking by remember { mutableStateOf(tts.speakingMessageId == message.id) }

    LaunchedEffect(tts.speakingMessageId) {
        speaking = tts.speakingMessageId == message.id
    }

    val badgeColor = when {
        message.model?.contains("Code") == true -> colors.code
        message.model?.contains("Azure") == true -> colors.azure
        message.model?.contains("Canvas") == true -> colors.azure
        else -> colors.accent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        // Header row: brand logo icon + model badge + token count + variant stepper
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.mino_logo),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = "  ${Models.getModelDisplayName(message.model)}",
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            message.usage?.let { usage ->
                Text(
                    text = "  ${usage.total} tok",
                    color = colors.textLow,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (message.hasVariants()) {
                VariantStepper(
                    index = message.variantIndex,
                    count = (message.variants?.size ?: 0) + 1,
                    onStep = { viewModel.setVariant(message.id, it) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgRaised)
                .padding(14.dp),
        ) {
            if (message.generatedImages.isNullOrEmpty()) {
                if (shown.isNotBlank()) {
                    MarkdownText(
                        markdown = shown + if (streamingThis) "▍" else "",
                        baseColor = colors.textHi,
                        linkColor = colors.accent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            message.generatedImages.orEmpty().forEach { generated ->
                GeneratedImageCard(generated = generated)
            }
        }

        // Truncation notice (exact sentence, spec §6.6/#8)
        if (message.truncated) {
            Text(
                text = Refusals.TRUNCATED_NOTICE,
                color = colors.amber,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.amberSoft)
                    .padding(10.dp),
            )
        }

        message.error?.let { error ->
            Text(
                text = error,
                color = colors.danger,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.dangerSoft)
                    .padding(10.dp),
            )
        }

        // Action row — only when finished, non-truncated, no error (spec §6.6)
        val canAct = !streamingThis && message.error == null && !message.truncated &&
            (message.content.isNotBlank() || message.hasVariants())
        if (canAct) {
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Retry",
                    color = colors.textLow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.retry(message.id) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied) colors.accent else colors.textLow,
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .clickable {
                            val ok = Share.copyToClipboard(context, "Mino answer", text)
                            copied = ok
                        },
                )
                if (copied) {
                    LaunchedEffect(message.id) {
                        delay(1500)
                        copied = false
                    }
                }
                if (tts.supported) {
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        if (speaking) Icons.Filled.StopCircle else Icons.Filled.GraphicEq,
                        contentDescription = if (speaking) "Stop reading" else "Read aloud",
                        tint = if (speaking) colors.accent else colors.textLow,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable {
                                tts.speakOrStop(message.id, text)
                                speaking = tts.speakingMessageId == message.id
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantStepper(index: Int, count: Int, onStep: (Int) -> Unit) {
    val colors = LocalMinoColors.current
    if (count <= 1) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "‹",
            color = colors.textMid,
            fontSize = 13.sp,
            modifier = Modifier.clip(CircleShape).clickable(enabled = index < count - 1) { onStep(index + 1) }.padding(horizontal = 6.dp),
        )
        Text(
            text = "${index + 1}/$count",
            color = colors.textLow,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "›",
            color = colors.textMid,
            fontSize = 13.sp,
            modifier = Modifier.clip(CircleShape).clickable(enabled = index > 0) { onStep(index - 1) }.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun GeneratedImageCard(generated: com.minetallest.mino.domain.GeneratedImage) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val bitmap = rememberBitmapFromDataUrl(generated.url)
    Column(
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgRaised)
            .padding(10.dp),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = generated.prompt,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth,
            )
        }
        Text(
            text = generated.prompt,
            color = colors.textMid,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Save",
            color = colors.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(CircleShape)
                .background(colors.accentSoft)
                .clickable {
                    try {
                        val bytes = android.util.Base64.decode(
                            generated.url.substringAfter("base64,"),
                            android.util.Base64.DEFAULT,
                        )
                        val file = File(context.cacheDir, "mino-image-${generated.createdAt}.png")
                        file.writeBytes(bytes)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        Share.shareImage(context, uri, "Mino image")
                    } catch (e: Throwable) {
                    }
                }
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

/** Spec §6.6 — waiting state: mode icon + badge + one of five joke lines. */
@Composable
private fun WaitingRow(hint: String) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.accent))
        Text(
            text = " Mino Auto",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "  $hint",
            color = colors.textLow,
            fontSize = 13.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Spec §10.2 — "Worth remembering" offer; nothing is stored without a tap. */
@Composable
private fun MemoryOffer(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.memorySuggestion.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgRaised)
            .padding(12.dp),
    ) {
        Text("Worth remembering", color = colors.textHi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        ui.memorySuggestion.forEach { suggestion ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = suggestion,
                    color = colors.textMid,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Remember",
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accentSoft)
                        .clickable { viewModel.acceptMemorySuggestion(suggestion) }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                "Not now",
                color = colors.textLow,
                fontSize = 11.sp,
                modifier = Modifier.clip(CircleShape).clickable { viewModel.dismissMemorySuggestion() }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Don't suggest this",
                color = colors.textLow,
                fontSize = 11.sp,
                modifier = Modifier.clip(CircleShape).clickable { viewModel.dontSuggestAgain() }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Hero Welcome View (Claude style): brand logo + personalized greeting "Back at it, [Name]". */
@Composable
private fun EmptyState(displayName: String?) {
    val name = displayName?.takeIf { it.isNotBlank() } ?: "Friend"
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = "Mino",
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "Back at it, $name",
                color = Color(0xFFF4F4F5),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}
