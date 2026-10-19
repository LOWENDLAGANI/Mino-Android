package com.minetallest.mino.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosIconBadge
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.ui.theme.MinoColors
import com.minetallest.mino.util.Haptics
import com.minetallest.mino.util.VoiceInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Message bar: attachments · text · mode · voice · send. */
@Composable
fun Composer(viewModel: ChatViewModel, onNavigate: ((String) -> Unit)? = null) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val composer by viewModel.composer.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var toolsOpen by remember { mutableStateOf(false) }
    val voice = remember { VoiceInput(context) }
    DisposableEffect(Unit) { onDispose { voice.stop() } }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            val pending = pendingCameraUri
            if (pending != null) viewModel.addImages(listOf(pending))
            pendingCameraUri = null
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.addImages(listOf(uri))
    }
    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.addDocuments(listOf(uri))
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startVoice(voice, viewModel)
        } else {
            viewModel.notice(Refusals.VOICE_FAILED, ChatViewModel.Notice.Tone.RED)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.pageBackground)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        composer.rateLimitUntil?.let { until ->
            RateLimitBanner(
                until = until,
                onQueue = { viewModel.queueUntilRateLimitLifts() },
                onDismiss = { viewModel.clearRateLimit() },
            )
        }

        if (composer.imageMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.codeSoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Image, null, tint = colors.code, modifier = Modifier.size(14.dp))
                Text(
                    "Image",
                    color = colors.code,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Exit image mode",
                    tint = colors.code,
                    modifier = Modifier.size(14.dp).clip(CircleShape).clickable { viewModel.toggleImageMode(false) },
                )
            }
        }

        if (composer.images.isNotEmpty() || composer.documents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                composer.images.forEach { image ->
                    val bitmap = rememberBitmapFromDataUrl(image.url)
                    Box(modifier = Modifier.padding(end = 6.dp)) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = image.name,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(3.dp)
                                .clickable { viewModel.removeImage(image.url) },
                        )
                    }
                }
                composer.documents.forEach { doc ->
                    Row(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(colors.fill)
                            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Description, null, tint = colors.textMid, modifier = Modifier.size(13.dp))
                        Text(
                            " ${doc.name}",
                            color = colors.text,
                            fontSize = 13.sp,
                            modifier = Modifier.widthIn(max = 160.dp),
                        )
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = colors.textMid,
                            modifier = Modifier.padding(start = 4.dp).size(12.dp).clip(CircleShape).clickable {
                                viewModel.removeDocument(doc.name)
                            },
                        )
                    }
                }
            }
        }

        composer.attachmentError?.let { error ->
            Text(
                text = error,
                color = colors.danger,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.dangerSoft)
                    .clickable { viewModel.setAttachmentError(null) }
                    .padding(10.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.composerShell)
                .border(0.5.dp, colors.line, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (composer.text.isEmpty()) {
                    Text(
                        text = when {
                            composer.compressing -> "Compressing…"
                            composer.imageMode -> "Describe an image"
                            else -> "Message"
                        },
                        color = colors.textMid,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                BasicTextField(
                    value = composer.text,
                    onValueChange = { viewModel.setComposerText(it) },
                    textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp, lineHeight = 23.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 26.dp, max = 180.dp)
                        .onKeyEvent { event ->
                            val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                            if (event.type == KeyEventType.KeyDown && isEnter && !event.isShiftPressed) {
                                focusManager.clearFocus()
                                viewModel.sendFromComposer()
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        focusManager.clearFocus()
                        viewModel.sendFromComposer()
                    }),
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.fill)
                        .clickable { toolsOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tools", tint = colors.text, modifier = Modifier.size(19.dp))
                }

                Spacer(Modifier.width(8.dp))
                ModeSelector(viewModel = viewModel)
                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startVoice(voice, viewModel)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = colors.textMid, modifier = Modifier.size(19.dp))
                }

                Spacer(Modifier.width(6.dp))

                val canSend = composer.text.isNotBlank() || composer.images.isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                ui.streaming -> colors.dangerSoft
                                canSend -> colors.accent
                                else -> colors.fill
                            },
                        )
                        .clickable(enabled = ui.streaming || canSend) {
                            Haptics.medium(LocalContext.current)
                            if (ui.streaming) viewModel.stopStream() else viewModel.sendFromComposer()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (ui.streaming) Icons.Filled.Stop else Icons.Filled.ArrowUpward,
                        contentDescription = if (ui.streaming) "Stop" else "Send",
                        tint = when {
                            ui.streaming -> colors.danger
                            canSend -> colors.onAccent
                            else -> colors.textMid
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        ToolsMenu(
            expanded = toolsOpen,
            onDismiss = { toolsOpen = false },
            onCamera = {
                toolsOpen = false
                val uri = newCameraUri(context)
                pendingCameraUri = uri
                if (uri != null) cameraLauncher.launch(uri)
            },
            onGallery = {
                toolsOpen = false
                galleryLauncher.launch("image/*")
            },
            onFiles = {
                toolsOpen = false
                filesLauncher.launch(arrayOf("text/plain", "text/markdown", "text/csv", "application/json", "text/*", "*/*"))
            },
            onDonate = {
                toolsOpen = false
                onNavigate?.invoke("donate")
            },
            onVoice = {
                toolsOpen = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startVoice(voice, viewModel)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onRemember = {
                toolsOpen = false
                viewModel.setRememberDialog(true)
            },
            onImageMode = {
                toolsOpen = false
                viewModel.toggleImageMode(true)
            },
        )
    }
}

private var pendingCameraUri: Uri? = null

private fun newCameraUri(context: android.content.Context): Uri? = try {
    val file = File.createTempFile("mino-cam-", ".jpg", context.cacheDir)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (e: Throwable) {
    null
}

private fun startVoice(voice: VoiceInput, viewModel: ChatViewModel) {
    voice.start(
        onResult = { transcript ->
            viewModel.setVoiceListening(false)
            viewModel.appendVoiceTranscript(transcript)
        },
        onError = { message ->
            viewModel.setVoiceListening(false)
            if (message != Refusals.VOICE_FAILED) {
                viewModel.notice(message, ChatViewModel.Notice.Tone.AMBER)
            }
        },
        onBeginning = { viewModel.setVoiceListening(true) },
        onEnd = { viewModel.setVoiceListening(false) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit,
    onDonate: () -> Unit,
    onVoice: () -> Unit,
    onRemember: () -> Unit,
    onImageMode: () -> Unit,
) {
    if (!expanded) return
    val colors = LocalMinoColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.menuBackground,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.lineStrong) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            ToolsRow(Icons.Filled.CameraAlt, "Camera", colors.accent, onCamera)
            IosDivider(startIndent = 52.dp)
            ToolsRow(Icons.Filled.Image, "Gallery", MinoColors.SystemPurple, onGallery)
            IosDivider(startIndent = 52.dp)
            ToolsRow(Icons.Filled.Description, "Files", colors.amber, onFiles)
            IosDivider(startIndent = 52.dp)
            ToolsRow(Icons.Filled.Mic, "Voice", colors.mint, onVoice)
            IosDivider(startIndent = 52.dp)
            ToolsRow(Icons.Filled.Psychology, "Remember", Color(0xFF5856D6), onRemember)
            IosDivider(startIndent = 52.dp)
            ToolsRow(Icons.Filled.Image, "Create Image", Color(0xFFFF2D55), onImageMode)
            IosDivider(startIndent = 52.dp)
            ToolsRow(Icons.Filled.Favorite, "Support Mino", Color(0xFFFF2D55), onDonate)
        }
    }
}

@Composable
private fun ToolsRow(icon: ImageVector, title: String, tint: Color, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IosIconBadge(icon = icon, tint = tint)
        Text(
            text = title,
            color = colors.text,
            fontSize = 17.sp,
            modifier = Modifier.padding(start = 13.dp).weight(1f),
        )
    }
}

@Composable
private fun RateLimitBanner(until: Long, onQueue: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalMinoColors.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(until) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= until) break
            delay(500)
        }
    }
    val seconds = ((until - now) / 1000).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.amberSoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AccessTime, null, tint = colors.amber, modifier = Modifier.size(14.dp))
        Text(
            text = "  Try again in ${seconds}s",
            color = colors.amber,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        if (seconds > 0) {
            Text(
                text = "Send later",
                color = colors.amber,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.bgRaised)
                    .clickable { onQueue() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = colors.amber,
                modifier = Modifier.size(14.dp).clip(CircleShape).clickable { onDismiss() },
            )
        }
    }
}

@Composable
fun NoticeStrip(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.notices.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ui.notices.forEach { notice ->
            Text(
                text = notice.text,
                fontSize = 12.sp,
                color = when (notice.tone) {
                    ChatViewModel.Notice.Tone.SUCCESS -> colors.mint
                    ChatViewModel.Notice.Tone.AMBER -> colors.amber
                    ChatViewModel.Notice.Tone.RED -> colors.danger
                    ChatViewModel.Notice.Tone.NEUTRAL -> colors.textMid
                },
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.bgRaised)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun Banner(text: String, background: Color, textColor: Color, onDismiss: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = textColor,
                modifier = Modifier.padding(start = 8.dp).size(14.dp).clip(CircleShape).clickable { onDismiss() },
            )
        }
    }
}
