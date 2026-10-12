package com.minetallest.mino.ui.chat

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Spec §6.4/§6.5 — the fixed 292dp rail: chats, folders, utilities, footer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Sidebar(viewModel: ChatViewModel, onNavigate: (String) -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var folderFilter by remember { mutableStateOf<String?>(null) }
    var menuChatId by remember { mutableStateOf<String?>(null) }
    var renamingChatId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var moveMenuChatId by remember { mutableStateOf<String?>(null) }
    var clearArmed by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportBackup()
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.notice("Backup saved to your downloads", com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.SUCCESS)
                } catch (e: Throwable) {
                    viewModel.notice("Export failed", com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.RED)
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                } catch (e: Throwable) {
                    ""
                }
                if (text.isBlank()) {
                    viewModel.notice("Import failed: could not read the file", com.minetallest.mino.ui.chat.ChatViewModel.Notice.Tone.RED)
                } else {
                    viewModel.importBackup(text)
                }
            }
        }
    }

    LaunchedEffect(clearArmed) {
        if (clearArmed) {
            delay(4000)
            clearArmed = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sidebarBackground)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.bgRaised)
                .clickable { viewModel.startNewChat() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            Text(
                text = "New chat",
                color = colors.textHi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (ui.temporary) colors.accentSoft else Color.Transparent)
                    .clickable { viewModel.toggleTemporary() }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (ui.temporary) "On" else "Temporary chat",
                    color = if (ui.temporary) colors.accent else colors.textMid,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Toolbar (spec §6.4): Search · Gallery · Trash(badge) · Import · Settings
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SidebarIcon(Icons.Filled.Search, "Search") { viewModel.setSearchOpen(true) }
            SidebarIcon(Icons.Filled.Image, "Gallery") { viewModel.setGalleryOpen(true) }
            Box {
                SidebarIcon(Icons.Filled.Delete, "Trash") { viewModel.setTrashOpen(true) }
                if (ui.trash.isNotEmpty()) {
                    BadgeDot(ui.trash.size.toString(), colors.code, Alignment.TopEnd)
                }
            }
            SidebarIcon(Icons.Filled.Upload, "Import") { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            SidebarIcon(Icons.Filled.Settings, "Settings") { viewModel.setSettingsOpen(true) }
        }

        // Promotional rows (spec §6.4): Dew chip replaces Lunar when balances exist.
        if (ui.firebaseConfigured && ui.dew != null) {
            val dew = ui.dew!!
            val low = dew.balance < 10
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bgRaised)
                    .clickable { onNavigate("plus") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.WaterDrop, null, tint = if (low) colors.amber else colors.accent, modifier = Modifier.size(15.dp))
                Text(
                    text = " ${"%.3f".format(dew.balance).trimEnd('0').trimEnd('.')} Dew",
                    color = if (low) colors.amber else colors.textHi,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                if (low) {
                    Text("Top up", color = colors.amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bgRaised)
                    .clickable { onNavigate("plus") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = colors.mint, modifier = Modifier.size(14.dp))
                Text(
                    text = "  Mino Lunar",
                    color = colors.textHi,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onNavigate("donate") }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Favorite, null, tint = Color(0xFFE58FA5), modifier = Modifier.size(13.dp))
            Text(
                text = "  Support Mino",
                color = colors.textMid,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Folder chips (spec §6.4)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderChip(
                label = "All",
                active = folderFilter == null,
                onDelete = null,
                onClick = { folderFilter = null },
            )
            ui.folders.forEach { folder ->
                val active = folderFilter == folder.id
                FolderChip(
                    label = folder.name,
                    active = active,
                    onDelete = if (active) {
                        { viewModel.deleteFolder(folder.id); folderFilter = null }
                    } else {
                        null
                    },
                    onClick = { folderFilter = if (active) null else folder.id },
                )
            }
        }

        // List header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (ui.folders.firstOrNull { it.id == folderFilter }?.name ?: "RECENT").uppercase(),
                color = colors.textLow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
                modifier = Modifier.weight(1f),
            )
            if (ui.scheduled.isNotEmpty()) {
                Text(
                    text = "${ui.scheduled.size} queued",
                    color = colors.textLow,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                text = "+ Folder",
                color = colors.textMid,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val name = "Folder ${(ui.folders.size + 1)}"
                        viewModel.createFolder(name)
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        // Chat list (spec §6.4): pinned floated, timeAgo, hold → menu, inline rename.
        val visibleChats = ui.chats.filter { chat -> folderFilter == null || chat.folder == folderFilter }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(visibleChats, key = { it.id }) { chat ->
                val now = System.currentTimeMillis()
                if (renamingChatId == chat.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.textHi, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                viewModel.renameChat(chat.id, renameText)
                                renamingChatId = null
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.bgHover)
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                        )
                    }
                } else {
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (chat.id == ui.activeChatId) colors.bgHover else Color.Transparent,
                                )
                                .combinedClickable(
                                    onClick = { viewModel.selectChat(chat.id) },
                                    onLongClick = { menuChatId = chat.id },
                                )
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (chat.pinned) {
                                Icon(
                                    Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    tint = colors.accent,
                                    modifier = Modifier.size(11.dp).padding(end = 4.dp),
                                )
                            }
                            Text(
                                text = chat.title,
                                color = colors.text,
                                fontSize = 13.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = Rules.timeAgo(now, chat.updatedAt),
                                color = colors.textLow,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        ChatRowMenu(
                            expanded = menuChatId == chat.id,
                            chatId = chat.id,
                            pinned = chat.pinned,
                            folders = ui.folders,
                            onDismiss = { menuChatId = null },
                            onPin = {
                                viewModel.togglePin(chat.id, !chat.pinned)
                                menuChatId = null
                            },
                            onMove = { folderId ->
                                viewModel.moveChatToFolder(chat.id, folderId)
                                moveMenuChatId = null
                                menuChatId = null
                            },
                            onRename = {
                                renameText = chat.title
                                renamingChatId = chat.id
                                menuChatId = null
                            },
                            onDelete = {
                                viewModel.deleteChat(chat.id)
                                menuChatId = null
                            },
                        )
                    }
                }
            }
        }

        // Footer (spec §6.5)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterAction("Export") {
                exportLauncher.launch(Rules.exportName())
            }
            FooterAction("Import") { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            Text(
                text = if (clearArmed) "Confirm?" else "Clear",
                color = if (clearArmed) colors.danger else colors.textMid,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (clearArmed) {
                            viewModel.wipeEverything()
                            clearArmed = false
                        } else {
                            clearArmed = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFC9E6D4), Color(0xFF4A8A67), Color(0xFF1F4A33)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (ui.displayName ?: "?").take(1).uppercase(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    text = ui.displayName ?: "",
                    color = colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Row {
                    Text(
                        text = "News",
                        color = colors.textLow,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigate("notes") }
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = "About",
                        color = colors.textLow,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigate("about") }
                            .padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

private fun Rules.exportName(): String = BackupExporter.name()

object BackupExporter {
    fun name(): String = com.minetallest.mino.domain.Backup.exportFileName(System.currentTimeMillis())
}

@Composable
private fun SidebarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(colors.bgRaised)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.textMid, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun BadgeDot(text: String, color: Color, alignment: Alignment) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FolderChip(label: String, active: Boolean, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(CircleShape)
            .background(if (active) colors.accentSoft else colors.bgRaised)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = if (active) colors.accent else colors.textLow,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = label,
            color = if (active) colors.accent else colors.textMid,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, end = if (onDelete != null) 4.dp else 0.dp),
        )
        if (onDelete != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete folder",
                tint = colors.textMid,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onDelete() },
            )
        }
    }
}

@Composable
private fun FooterAction(label: String, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Text(
        text = label,
        color = colors.textMid,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Spec §6.4 — labelled menu: Pin/Unpin · Move to folder · Rename · Delete. */
@Composable
private fun ChatRowMenu(
    expanded: Boolean,
    chatId: String,
    pinned: Boolean,
    folders: List<com.minetallest.mino.domain.ChatFolder>,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onMove: (String?) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalMinoColors.current
    var moveOpen by remember { mutableStateOf(false) }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (pinned) "Unpin" else "Pin") },
            onClick = onPin,
            trailingIcon = { Icon(Icons.Outlined.PushPin, null) },
        )
        DropdownMenuItem(
            text = { Text("Move to folder") },
            onClick = { moveOpen = true },
            trailingIcon = { Icon(Icons.Filled.Folder, null) },
        )
        if (moveOpen) {
            DropdownMenuItem(text = { Text("No folder", fontSize = 12.sp) }, onClick = { onMove(null) })
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name, fontSize = 12.sp) },
                    onClick = { onMove(folder.id) },
                )
            }
        }
        DropdownMenuItem(text = { Text("Rename") }, onClick = onRename)
        DropdownMenuItem(
            text = { Text("Delete", color = colors.danger) },
            onClick = onDelete,
            trailingIcon = { Icon(Icons.Filled.Delete, null, tint = colors.danger) },
        )
    }
}
