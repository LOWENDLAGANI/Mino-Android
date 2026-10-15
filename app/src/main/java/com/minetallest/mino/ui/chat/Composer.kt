package com.minetallest.mino.ui.chat

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.domain.Schedule
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.util.VoiceInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import java.util.Calendar

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

/** Spec §6.7 — composer: `[+ tools] [textarea] [clock] [send/stop]` in one rounded shell. */
@Composable
fun Composer(viewModel: ChatViewModel, onNavigate: ((String) -> Unit)? = null) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val composer by viewModel.composer.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var toolsOpen by remember { mutableStateOf(false) }
    var scheduleOpen by remember { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.pageBackground)
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            // Rate-limit banner (spec §6.7)
            composer.rateLimitUntil?.let { until ->
                RateLimitBanner(
                    until = until,
                    onQueue = { viewModel.queueUntilRateLimitLifts() },
                    onDismiss = { viewModel.clearRateLimit() },
                )
            }

        // Scheduled chips (spec §6.7)
        if (ui.scheduled.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
            ) {
                ui.scheduled.forEach { item ->
                    Row(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(colors.bgRaised)
                            .padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AccessTime, null, tint = colors.textLow, modifier = Modifier.size(11.dp))
                        Text(
                            text = " ${Schedule.chipLabel(System.currentTimeMillis(), item.sendAt)}",
                            color = colors.textMid,
                            fontSize = 10.sp,
                        )
                        Text(
                            text = "  " + item.content.take(24),
                            color = colors.textLow,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 110.dp),
                        )
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = colors.textMid,
                            modifier = Modifier.padding(start = 4.dp).size(11.dp).clip(CircleShape).clickable {
                                viewModel.cancelSchedule(item.id)
                            },
                        )
                    }
                }
            }
        }

        // Image-mode strip (spec §6.7)
        if (composer.imageMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.codeSoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Image, null, tint = colors.code, modifier = Modifier.size(13.dp))
                Text("  Image", color = colors.code, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Exit image mode",
                    tint = colors.code,
                    modifier = Modifier.size(13.dp).clip(CircleShape).clickable { viewModel.toggleImageMode(false) },
                )
            }
        }

        // Attachment previews
        if (composer.images.isNotEmpty() || composer.documents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                composer.images.forEach { image ->
                    val bitmap = rememberBitmapFromDataUrl(image.url)
                    Box(modifier = Modifier.padding(end = 6.dp)) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = image.name,
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        }
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { viewModel.removeImage(image.url) },
                        )
                    }
                }
                composer.documents.forEach { doc ->
                    Row(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(colors.bgElevated)
                            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Description, null, tint = colors.textMid, modifier = Modifier.size(12.dp))
                        Text(" ${doc.name}", color = colors.text, fontSize = 11.sp)
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = colors.textMid,
                            modifier = Modifier.padding(start = 4.dp).size(11.dp).clip(CircleShape).clickable {
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
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.dangerSoft)
                    .clickable { viewModel.setAttachmentError(null) }
                    .padding(8.dp),
            )
        }

        // Input Card Container (Claude Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E22))
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp))
                .padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                if (composer.text.isEmpty()) {
                    Text(
                        text = when {
                            composer.compressing -> "Compressing…"
                            composer.imageMode -> "Describe the image to create…"
                            else -> "Ask Mino anything…"
                        },
                        color = Color(0xFFA1A1AA),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                BasicTextField(
                    value = composer.text,
                    onValueChange = { viewModel.setComposerText(it) },
                    textStyle = TextStyle(color = Color(0xFFF4F4F5), fontSize = 15.sp, lineHeight = 22.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp, max = 180.dp)
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

            // Bottom Docked Controls inside Input Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Tools",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF27272A))
                            .clickable { toolsOpen = true }
                            .padding(7.dp),
                    )
                    ToolsMenu(
                        expanded = toolsOpen,
                        imageUnavailable = ui.availability?.imageAvailable == false,
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

                Spacer(Modifier.width(8.dp))

                // Docked Model Selector Pill
                ModeSelector(viewModel = viewModel)

                Spacer(Modifier.weight(1f))

                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Voice Input",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startVoice(voice, viewModel)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .padding(7.dp),
                )

                val canSend = composer.text.isNotBlank() || composer.images.isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                ui.streaming -> Color(0xFF27272A)
                                canSend -> Color(0xFFF4F4F5)
                                else -> Color(0xFF27272A)
                            },
                        )
                        .clickable(enabled = ui.streaming || canSend) {
                            if (ui.streaming) viewModel.stopStream() else viewModel.sendFromComposer()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (ui.streaming) Icons.Filled.Stop else Icons.Filled.Send,
                        contentDescription = if (ui.streaming) "Stop" else "Send",
                        tint = when {
                            ui.streaming -> Color(0xFFEF5350)
                            canSend -> Color(0xFF121212)
                            else -> Color(0xFFA1A1AA)
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
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


/** Attachment / Action Menu Bottom Sheet (Claude / ChatGPT style). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsMenu(
    expanded: Boolean,
    imageUnavailable: Boolean,
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF1E1E22),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFFA1A1AA))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ToolsRow(
                icon = Icons.Filled.CameraAlt,
                title = "Camera",
                onClick = onCamera,
            )
            ToolsRow(
                icon = Icons.Filled.Image,
                title = "Gallery",
                onClick = onGallery,
            )
            ToolsRow(
                icon = Icons.Filled.Description,
                title = "Files",
                onClick = onFiles,
            )
            ToolsRow(
                icon = Icons.Filled.Mic,
                title = "Voice Input",
                onClick = onVoice,
            )
            ToolsRow(
                icon = Icons.Filled.Psychology,
                title = "Remember This",
                onClick = onRemember,
            )
            ToolsRow(
                icon = Icons.Filled.AutoAwesome,
                title = "Create Image",
                onClick = onImageMode,
            )
            ToolsRow(
                icon = Icons.Filled.Favorite,
                title = "Support Mino",
                onClick = onDonate,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToolsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFA1A1AA),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            color = Color(0xFFF4F4F5),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp).weight(1f),
        )
    }
}

/** Spec §6.7 — send later: presets + custom picker with floor = now + 60 s. */
@Composable
private fun ScheduleMenu(
    expanded: Boolean,
    text: String,
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit,
) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.width(286.dp)) {
        Schedule.presets.forEach { preset ->
            DropdownMenuItem(
                text = { Text(preset.label, fontSize = 13.sp) },
                onClick = { onSchedule(preset.sendAt(System.currentTimeMillis())) },
            )
        }
        DropdownMenuItem(
            text = { Text("Pick date & time…", fontSize = 13.sp) },
            onClick = {
                val floor = Calendar.getInstance().apply { timeInMillis = Schedule.floor(System.currentTimeMillis()) }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val picked = Calendar.getInstance().apply {
                                    set(year, month, day, hour, minute, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                onSchedule(maxOf(picked, Schedule.floor(System.currentTimeMillis())))
                            },
                            floor.get(Calendar.HOUR_OF_DAY),
                            floor.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                    floor.get(Calendar.YEAR),
                    floor.get(Calendar.MONTH),
                    floor.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
        )
        androidx.compose.material3.Text(
            "Fires from this app while it is open — keep it open if the timing matters.",
            fontSize = 10.sp,
            color = colors.textLow,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Spec §6.7 — amber rate-limit banner with live countdown + "Queue until then". */
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
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.amberSoft)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AccessTime, null, tint = colors.amber, modifier = Modifier.size(13.dp))
        Text(
            text = "  Rate limited — you can send again in ${seconds}s.",
            color = colors.amber,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
        if (seconds > 0) {
            Text(
                text = "Queue until then",
                color = colors.amber,
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1AFBBF24))
                    .clickable { onQueue() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = colors.amber,
                modifier = Modifier.size(13.dp).clip(CircleShape).clickable { onDismiss() },
            )
        }
    }
}

/** Inline notices above the thread (spec §6.6 — notices expire in 3–4 s). */
@Composable
fun NoticeStrip(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.notices.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ui.notices.forEach { notice ->
            Text(
                text = notice.text,
                fontSize = 11.sp,
                color = when (notice.tone) {
                    ChatViewModel.Notice.Tone.SUCCESS -> colors.accent
                    ChatViewModel.Notice.Tone.AMBER -> colors.amber
                    ChatViewModel.Notice.Tone.RED -> colors.danger
                    ChatViewModel.Notice.Tone.NEUTRAL -> colors.textMid
                },
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.bgRaised)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** Banners (announcement, model-change) — spec §6.3/§6.6. */
@Composable
fun Banner(text: String, background: Color, textColor: Color, onDismiss: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = textColor,
                modifier = Modifier.padding(start = 8.dp).size(13.dp).clip(CircleShape).clickable { onDismiss() },
            )
        }
    }
}
