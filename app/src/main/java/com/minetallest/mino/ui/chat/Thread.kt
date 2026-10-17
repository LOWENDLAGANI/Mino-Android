package com.minetallest.mino.ui.chat

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.TextStyle
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

/** Chat thread: message bubbles, streaming state, sources and the memory offer. */
@Composable
fun Thread(viewModel: ChatViewModel) {
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
            if (index >= 0) listState.animateScrollToItem(index + 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        ) {
            item { ShareRow(viewModel = viewModel, messages = messages) }
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
                    item { WaitingRow() }
                }
            }
            item { MemoryOffer(viewModel = viewModel) }
        }
    }
}

@Composable
private fun ShareRow(viewModel: ChatViewModel, messages: List<ChatMessage>) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        ToolbarIcon(
            icon = Icons.Outlined.Image,
            description = if (rendering) "Rendering" else "Share image",
            enabled = !rendering,
        ) {
            rendering = true
            scope.launch {
                val bitmap = renderConversation(context, messages)
                val file = File(context.cacheDir, "mino-share.png").apply {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream())
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                Share.shareImage(context, uri, "Mino conversation")
                viewModel.notice("Image saved", ChatViewModel.Notice.Tone.SUCCESS)
                rendering = false
            }
        }
        ToolbarIcon(Icons.Filled.ContentCopy, "Copy chat") {
            val text = Share.chatToPlainText(messages)
            val ok = Share.copyToClipboard(context, "Mino chat", text)
            viewModel.notice(
                if (ok) "Copied" else "Couldn't copy",
                if (ok) ChatViewModel.Notice.Tone.SUCCESS else ChatViewModel.Notice.Tone.RED,
            )
        }
        if (allSources.isNotEmpty()) {
            ToolbarIcon(
                icon = Icons.Filled.Link,
                description = "Sources (${allSources.size})",
                tint = if (sourcesOpen) colors.accent else null,
            ) { sourcesOpen = !sourcesOpen }
        }
        ToolbarIcon(Icons.Filled.Share, "Share link") {
            scope.launch {
                val origin = viewModel.serverOrigin()
                val link = Share.shareLink(origin, title = "Mino chat", messages = messages)
                if (link == null) {
                    viewModel.notice("Set a server in Settings", ChatViewModel.Notice.Tone.AMBER)
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
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.bgRaised)
                .padding(12.dp),
        ) {
            allSources.forEach { source ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(source.id, color = colors.textMid, fontSize = 12.sp)
                    Column(Modifier.padding(start = 8.dp).weight(1f)) {
                        Text(
                            source.title.take(160),
                            color = colors.text,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            try { Uri.parse(source.url).host ?: source.url } catch (e: Throwable) { source.url },
                            color = colors.textMid,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(colors.bgRaised)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = tint ?: colors.textMid,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun UserBubble(viewModel: ChatViewModel, message: ChatMessage) {
    val colors = LocalMinoColors.current
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 340.dp)) {
            message.images.orEmpty().forEach { image ->
                val bitmap = rememberBitmapFromDataUrl(image.url)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = image.name,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .heightIn(max = 120.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            message.documents.orEmpty().forEach { doc ->
                Text(
                    text = doc.name,
                    color = colors.text,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.fill)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            if (editing) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.bgRaised)
                        .padding(10.dp),
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        textStyle = TextStyle(color = colors.textHi, fontSize = 16.sp, lineHeight = 22.sp),
                        modifier = Modifier.widthIn(min = 220.dp, max = 320.dp).heightIn(min = 40.dp),
                    )
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "Cancel",
                            color = colors.textMid,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { editing = false; editText = message.content }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Send",
                            color = colors.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
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
                    text = message.displayedContent(),
                    color = colors.userBubbleText,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.userBubble)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
                Text(
                    text = "Edit",
                    color = colors.textMid,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(CircleShape)
                        .clickable {
                            editText = message.content
                            editing = true
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(viewModel: ChatViewModel, message: ChatMessage) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val activeMessages = if (ui.temporary) ui.temporaryMessages else ui.messages
    val streamingThis = ui.streaming && activeMessages.lastOrNull()?.id == message.id && message.error == null
    val text = message.displayedContent()
    val shown = rememberSmoothText(text, active = streamingThis)
    var copied by remember { mutableStateOf(false) }
    val tts = rememberThreadTts()
    val ttsReady by tts.available.collectAsStateWithLifecycle()
    var speaking by remember { mutableStateOf(tts.speakingMessageId == message.id) }

    LaunchedEffect(tts.speakingMessageId) {
        speaking = tts.speakingMessageId == message.id
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 5.dp)) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(CircleShape),
            )
            Text(
                text = Models.getModelDisplayName(message.model),
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp),
            )
            message.usage?.let { usage ->
                Text(
                    text = "${usage.total} tok",
                    color = colors.textMid,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 7.dp),
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

        if (message.generatedImages.isNullOrEmpty() && shown.isNotBlank()) {
            MarkdownText(
                markdown = shown + if (streamingThis) "▍" else "",
                baseColor = colors.text,
                linkColor = colors.accent,
                modifier = Modifier.fillMaxWidth().padding(start = 27.dp),
            )
        }

        message.generatedImages.orEmpty().forEach { generated ->
            GeneratedImageCard(generated = generated)
        }

        if (message.truncated) {
            Text(
                text = Refusals.TRUNCATED_NOTICE,
                color = colors.amber,
                fontSize = 13.sp,
                lineHeight = 18.sp,
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
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.dangerSoft)
                    .padding(10.dp),
            )
        }

        val canAct = !streamingThis && message.error == null && !message.truncated &&
            (message.content.isNotBlank() || message.hasVariants())
        if (canAct) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 21.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionIcon(
                    icon = Icons.Filled.Refresh,
                    description = "Retry",
                    tint = colors.textMid,
                ) { viewModel.retry(message.id) }
                Spacer(Modifier.width(4.dp))
                ActionIcon(
                    icon = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    description = "Copy",
                    tint = if (copied) colors.accent else colors.textMid,
                ) {
                    val ok = Share.copyToClipboard(context, "Mino answer", text)
                    copied = ok
                }
                if (copied) {
                    LaunchedEffect(message.id) {
                        delay(1500)
                        copied = false
                    }
                }
                if (ttsReady) {
                    Spacer(Modifier.width(4.dp))
                    ActionIcon(
                        icon = if (speaking) Icons.Filled.StopCircle else Icons.Filled.GraphicEq,
                        description = if (speaking) "Stop reading" else "Read aloud",
                        tint = if (speaking) colors.accent else colors.textMid,
                    ) {
                        tts.speakOrStop(message.id, text)
                        speaking = tts.speakingMessageId == message.id
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun VariantStepper(index: Int, count: Int, onStep: (Int) -> Unit) {
    val colors = LocalMinoColors.current
    if (count <= 1) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.bgRaised)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = "‹",
            color = if (index < count - 1) colors.text else colors.textLow,
            fontSize = 16.sp,
            modifier = Modifier.clip(CircleShape).clickable(enabled = index < count - 1) { onStep(index + 1) }.padding(horizontal = 7.dp),
        )
        Text(
            text = "${index + 1} of $count",
            color = colors.textMid,
            fontSize = 12.sp,
        )
        Text(
            text = "›",
            color = if (index > 0) colors.text else colors.textLow,
            fontSize = 16.sp,
            modifier = Modifier.clip(CircleShape).clickable(enabled = index > 0) { onStep(index - 1) }.padding(horizontal = 7.dp),
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
            .padding(top = 8.dp, start = 27.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgRaised)
            .padding(6.dp),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = generated.prompt,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            Text(
                text = generated.prompt,
                color = colors.textMid,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Save",
                color = colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(CircleShape)
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun WaitingRow() {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            color = colors.textMid,
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Mino",
            color = colors.textMid,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun MemoryOffer(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.memorySuggestion.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgRaised)
            .padding(14.dp),
    ) {
        Text("Remember this?", color = colors.textHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        ui.memorySuggestion.forEach { suggestion ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = suggestion,
                    color = colors.text,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Remember",
                    color = colors.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.acceptMemorySuggestion(suggestion) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                "Not now",
                color = colors.accent,
                fontSize = 15.sp,
                modifier = Modifier.clip(CircleShape).clickable { viewModel.dismissMemorySuggestion() }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Don't ask again",
                color = colors.textMid,
                fontSize = 15.sp,
                modifier = Modifier.clip(CircleShape).clickable { viewModel.dontSuggestAgain() }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(displayName: String?) {
    val colors = LocalMinoColors.current
    val name = displayName?.takeIf { it.isNotBlank() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = "Mino",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
            )
            Text(
                text = if (name != null) "Hello, $name" else "Hello",
                color = colors.textHi,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}
