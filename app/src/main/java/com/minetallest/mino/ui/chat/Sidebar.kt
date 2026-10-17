package com.minetallest.mino.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.Chat
import com.minetallest.mino.domain.ChatFolder
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosIconBadge
import com.minetallest.mino.ui.theme.IosRow
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.ui.theme.MinoColors

/** iOS-style sidebar: search, destinations, folders and the recent chat list. */
@Composable
fun Sidebar(viewModel: ChatViewModel, onNavigate: (String) -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var folderFilter by remember { mutableStateOf<String?>(null) }
    var menuChatId by remember { mutableStateOf<String?>(null) }
    var renamingChatId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sidebarBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Chats",
                color = colors.textHi,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.startNewChat() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "New chat", tint = colors.accent, modifier = Modifier.size(22.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.fill)
                .clickable { viewModel.setSearchOpen(true) }
                .padding(horizontal = 9.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textMid, modifier = Modifier.size(18.dp))
            Text(
                text = "Search",
                color = colors.textMid,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        IosGroup {
            IosRow(
                title = "New Chat",
                icon = Icons.Filled.Add,
                iconTint = colors.accent,
                onClick = { viewModel.startNewChat() },
            )
            IosDivider(startIndent = 57.dp)
            IosRow(
                title = "Gallery",
                icon = Icons.Filled.Image,
                iconTint = MinoColors.SystemPurple,
                onClick = { viewModel.setGalleryOpen(true) },
            )
            IosDivider(startIndent = 57.dp)
            IosRow(
                title = "Trash",
                icon = Icons.Filled.Delete,
                iconTint = colors.danger,
                value = if (ui.trash.isNotEmpty()) ui.trash.size.toString() else null,
                onClick = { viewModel.setTrashOpen(true) },
            )
            IosDivider(startIndent = 57.dp)
            IosRow(
                title = "Mino Plus",
                icon = Icons.Filled.AccountBalanceWallet,
                iconTint = colors.accent,
                value = ui.dew?.balance?.let {
                    "%.2f".format(it).trimEnd('0').trimEnd('.')
                } ?: "Plus",
                onClick = { onNavigate("plus") },
            )
            IosDivider(startIndent = 57.dp)
            IosRow(
                title = "Support",
                icon = Icons.Filled.Favorite,
                iconTint = Color(0xFFFF2D55),
                onClick = { onNavigate("donate") },
            )
            IosDivider(startIndent = 57.dp)
            IosRow(
                title = "News",
                icon = Icons.Filled.Campaign,
                iconTint = colors.amber,
                onClick = { onNavigate("notes") },
            )
            IosDivider(startIndent = 57.dp)
            IosRow(
                title = "About",
                icon = Icons.Filled.Info,
                iconTint = colors.textMid,
                chevron = true,
                onClick = { onNavigate("about") },
            )
        }

        FolderSection(
            folders = ui.folders,
            activeFolderId = folderFilter,
            onSelectFolder = { folderFilter = it },
            onCreateFolder = { viewModel.createFolder(it) },
            onRenameFolder = { id, name -> viewModel.renameFolder(id, name) },
            onDeleteFolder = { viewModel.deleteFolder(it) },
        )

        val visibleChats = ui.chats.filter { chat -> folderFilter == null || chat.folder == folderFilter }
        val pinned = visibleChats.filter { it.pinned }
        val others = visibleChats.filterNot { it.pinned }

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (pinned.isNotEmpty()) {
                item { IosSectionHeader("Pinned") }
                items(pinned, key = { it.id }) { chat ->
                    ChatRow(
                        chat = chat,
                        selected = chat.id == ui.activeChatId,
                        renaming = renamingChatId == chat.id,
                        renameText = renameText,
                        menuOpen = menuChatId == chat.id,
                        folders = ui.folders,
                        onSelect = { viewModel.selectChat(chat.id) },
                        onRenameChange = { renameText = it },
                        onRenameDone = {
                            viewModel.renameChat(chat.id, renameText)
                            renamingChatId = null
                        },
                        onMenuOpen = { menuChatId = chat.id },
                        onMenuClose = { menuChatId = null },
                        onRenameStart = {
                            renameText = chat.title
                            renamingChatId = chat.id
                            menuChatId = null
                        },
                        onTogglePin = { viewModel.togglePin(chat.id, !chat.pinned) },
                        onMoveToFolder = { folderId -> viewModel.moveChatToFolder(chat.id, folderId) },
                        onDelete = { viewModel.deleteChat(chat.id) },
                    )
                }
            }
            item { IosSectionHeader(if (folderFilter != null) "Chats" else "Recents") }
            if (others.isEmpty() && pinned.isEmpty()) {
                item {
                    Text(
                        text = "No chats",
                        color = colors.textMid,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            items(others, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    selected = chat.id == ui.activeChatId,
                    renaming = renamingChatId == chat.id,
                    renameText = renameText,
                    menuOpen = menuChatId == chat.id,
                    folders = ui.folders,
                    onSelect = { viewModel.selectChat(chat.id) },
                    onRenameChange = { renameText = it },
                    onRenameDone = {
                        viewModel.renameChat(chat.id, renameText)
                        renamingChatId = null
                    },
                    onMenuOpen = { menuChatId = chat.id },
                    onMenuClose = { menuChatId = null },
                    onRenameStart = {
                        renameText = chat.title
                        renamingChatId = chat.id
                        menuChatId = null
                    },
                    onTogglePin = { viewModel.togglePin(chat.id, !chat.pinned) },
                    onMoveToFolder = { folderId -> viewModel.moveChatToFolder(chat.id, folderId) },
                    onDelete = { viewModel.deleteChat(chat.id) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        DrawerFooter(
            name = ui.displayName,
            temporary = ui.temporary,
            onTemporary = { viewModel.toggleTemporary() },
            onSettings = { onNavigate("settings") },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    chat: Chat,
    selected: Boolean,
    renaming: Boolean,
    renameText: String,
    menuOpen: Boolean,
    folders: List<ChatFolder>,
    onSelect: () -> Unit,
    onRenameChange: (String) -> Unit,
    onRenameDone: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuClose: () -> Unit,
    onRenameStart: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveToFolder: (String?) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalMinoColors.current

    if (renaming) {
        BasicTextField(
            value = renameText,
            onValueChange = onRenameChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onRenameDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.bgRaised)
                .padding(horizontal = 12.dp, vertical = 11.dp),
        )
        return
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) colors.bgRaised else androidx.compose.ui.graphics.Color.Transparent)
                .combinedClickable(onClick = onSelect, onLongClick = onMenuOpen)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chat.pinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = colors.textMid,
                    modifier = Modifier.size(13.dp).padding(end = 5.dp),
                )
            }
            Text(
                text = chat.title,
                color = if (selected) colors.textHi else colors.text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Rules.timeAgo(System.currentTimeMillis(), chat.updatedAt),
                color = colors.textMid,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        ChatRowMenu(
            expanded = menuOpen,
            pinned = chat.pinned,
            folders = folders,
            onDismiss = onMenuClose,
            onPin = onTogglePin,
            onMove = onMoveToFolder,
            onRename = onRenameStart,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderSection(
    folders: List<ChatFolder>,
    activeFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
) {
    val colors = LocalMinoColors.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var menuFolderId by remember { mutableStateOf<String?>(null) }
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    var folderRenameText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Folders",
            color = colors.textMid,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showCreateDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("New", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderChip(
            label = "All",
            active = activeFolderId == null,
            onDelete = null,
            onClick = { onSelectFolder(null) },
            onLongClick = {},
        )
        folders.forEach { folder ->
            val active = activeFolderId == folder.id
            Box {
                if (renamingFolderId == folder.id) {
                    BasicTextField(
                        value = folderRenameText,
                        onValueChange = { folderRenameText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.text, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (folderRenameText.isNotBlank()) onRenameFolder(folder.id, folderRenameText)
                            renamingFolderId = null
                        }),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(colors.bgRaised)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                } else {
                    FolderChip(
                        label = folder.name,
                        active = active,
                        onDelete = { onDeleteFolder(folder.id); if (active) onSelectFolder(null) },
                        onClick = { onSelectFolder(if (active) null else folder.id) },
                        onLongClick = { menuFolderId = folder.id },
                    )
                }

                DropdownMenu(expanded = menuFolderId == folder.id, onDismissRequest = { menuFolderId = null }) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = colors.text) },
                        onClick = {
                            folderRenameText = folder.name
                            renamingFolderId = folder.id
                            menuFolderId = null
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = colors.danger) },
                        onClick = {
                            onDeleteFolder(folder.id)
                            if (active) onSelectFolder(null)
                            menuFolderId = null
                        },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Folder", color = colors.textHi, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                BasicTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.text, fontSize = 17.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.fill)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (newFolderName.isEmpty()) Text("Name", color = colors.textMid, fontSize = 17.sp)
                        inner()
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder(newFolderName)
                        newFolderName = ""
                        showCreateDialog = false
                    }
                }) {
                    Text("Create", color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = colors.accent)
                }
            },
            containerColor = colors.bgRaised,
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    label: String,
    active: Boolean,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(CircleShape)
            .background(if (active) colors.accent else colors.bgRaised)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = if (active) colors.onAccent else colors.textMid,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            color = if (active) colors.onAccent else colors.text,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 5.dp, end = if (onDelete != null) 3.dp else 0.dp),
        )
        if (onDelete != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete folder",
                tint = if (active) colors.onAccent else colors.textMid,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onDelete() },
            )
        }
    }
}

@Composable
private fun DrawerFooter(
    name: String?,
    temporary: Boolean,
    onTemporary: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSettings() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (temporary) colors.amber else colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (name ?: "M").take(1).uppercase(),
                color = colors.onAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        ) {
            Text(
                text = name ?: "You",
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (temporary) {
                Text("Temporary", color = colors.amber, fontSize = 12.sp)
            }
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTemporary() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.AccessTime,
                contentDescription = "Temporary chat",
                tint = if (temporary) colors.amber else colors.textMid,
                modifier = Modifier.size(20.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSettings() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = colors.textMid, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun ChatRowMenu(
    expanded: Boolean,
    pinned: Boolean,
    folders: List<ChatFolder>,
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
            text = { Text(if (pinned) "Unpin" else "Pin", color = colors.text) },
            onClick = onPin,
            trailingIcon = { Icon(Icons.Filled.PushPin, null, tint = colors.textMid, modifier = Modifier.size(18.dp)) },
        )
        DropdownMenuItem(
            text = { Text("Move", color = colors.text) },
            onClick = { moveOpen = true },
            trailingIcon = { Icon(Icons.Filled.Folder, null, tint = colors.textMid, modifier = Modifier.size(18.dp)) },
        )
        if (moveOpen) {
            DropdownMenuItem(
                text = { Text("No folder", fontSize = 13.sp, color = colors.text) },
                onClick = { onMove(null) },
            )
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name, fontSize = 13.sp, color = colors.text) },
                    onClick = { onMove(folder.id) },
                )
            }
        }
        DropdownMenuItem(
            text = { Text("Rename", color = colors.text) },
            onClick = onRename,
        )
        DropdownMenuItem(
            text = { Text("Delete", color = colors.danger) },
            onClick = onDelete,
            trailingIcon = { Icon(Icons.Filled.Delete, null, tint = colors.danger, modifier = Modifier.size(18.dp)) },
        )
    }
}
