package com.minetallest.mino.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.R
import com.minetallest.mino.domain.Chat
import com.minetallest.mino.domain.ChatFolder
import com.minetallest.mino.domain.Rules

// Strict Claude Style Ultra-Minimalist Palette Constants
private val DarkDrawerBg = Color(0xFF121212)       // Deep charcoal background
private val DarkCardSurface = Color(0xFF1E1E22)    // Surface container
private val SelectedContainer = Color(0xFF27272A)  // Muted selection container
private val DividerColor = Color(0xFF27272A)       // Faint border/divider stroke

// Text & Icons
private val TextPrimary = Color(0xFFF4F4F5)        // Crisp off-white primary text
private val TextSecondary = Color(0xFFA1A1AA)      // Muted cool gray secondary text & icons
private val DestructiveRed = Color(0xFFEF5350)     // Red for deletion actions

/**
 * Ultra-minimalist Claude Style Sidebar / Drawer component.
 * Strict monochrome charcoal palette, zero emojis, clean list rows, and safe bottom navigation padding.
 */
@Composable
fun Sidebar(viewModel: ChatViewModel, onNavigate: (String) -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var folderFilter by remember { mutableStateOf<String?>(null) }
    var menuChatId by remember { mutableStateOf<String?>(null) }
    var renamingChatId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkDrawerBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        DrawerHeader(viewModel = viewModel, ui = ui)

        DrawerNavigationSection(viewModel = viewModel, ui = ui, onNavigate = onNavigate)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = DividerColor,
        )

        FolderSection(
            folders = ui.folders,
            activeFolderId = folderFilter,
            onSelectFolder = { folderFilter = it },
            onCreateFolder = { viewModel.createFolder(it) },
            onRenameFolder = { id, name -> viewModel.renameFolder(id, name) },
            onDeleteFolder = { viewModel.deleteFolder(it) },
        )

        val visibleChats = ui.chats.filter { chat -> folderFilter == null || chat.folder == folderFilter }

        ChatHistoryList(
            chats = visibleChats,
            activeChatId = ui.activeChatId,
            renamingChatId = renamingChatId,
            renameText = renameText,
            menuChatId = menuChatId,
            folders = ui.folders,
            onSelectChat = { viewModel.selectChat(it) },
            onMenuOpen = { menuChatId = it },
            onMenuClose = { menuChatId = null },
            onRenameStart = { chat ->
                renameText = chat.title
                renamingChatId = chat.id
                menuChatId = null
            },
            onRenameChange = { renameText = it },
            onRenameDone = { chatId ->
                viewModel.renameChat(chatId, renameText)
                renamingChatId = null
            },
            onTogglePin = { chatId, pinned -> viewModel.togglePin(chatId, pinned) },
            onMoveToFolder = { chatId, folderId -> viewModel.moveChatToFolder(chatId, folderId) },
            onDeleteChat = { viewModel.deleteChat(it) },
            modifier = Modifier.weight(1f),
        )

        DrawerFooter(
            ui = ui,
            onNavigate = onNavigate,
            onOpenSettings = { onNavigate("settings") },
        )
    }
}

/** 1. Drawer Header: Brand name + Prominent + New Chat button + Temporary Chat toggle */
@Composable
private fun DrawerHeader(
    viewModel: ChatViewModel,
    ui: ChatViewModel.UiState,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = "Mino",
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Mino AI",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (ui.temporary) SelectedContainer else DarkCardSurface)
                    .clickable { viewModel.toggleTemporary() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (ui.temporary) "Temp On" else "Temporary",
                    color = if (ui.temporary) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Button(
            onClick = { viewModel.startNewChat() },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkCardSurface,
                contentColor = TextPrimary,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
            Text(
                text = "New Chat",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** 2. Vertical Quick Links Navigation Section */
@Composable
private fun DrawerNavigationSection(
    viewModel: ChatViewModel,
    ui: ChatViewModel.UiState,
    onNavigate: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NavigationDrawerItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_book_search), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            label = { Text("Search Conversations", color = TextSecondary, fontSize = 13.sp) },
            selected = false,
            onClick = { viewModel.setSearchOpen(true) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        )

        NavigationDrawerItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_images), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            label = { Text("Gallery & Images", color = TextSecondary, fontSize = 13.sp) },
            selected = false,
            onClick = { viewModel.setGalleryOpen(true) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        )

        NavigationDrawerItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_trash), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            label = { Text("Trash", color = TextSecondary, fontSize = 13.sp) },
            badge = {
                if (ui.trash.isNotEmpty()) {
                    Badge(containerColor = SelectedContainer, contentColor = TextPrimary) {
                        Text(ui.trash.size.toString(), fontSize = 10.sp)
                    }
                }
            },
            selected = false,
            onClick = { viewModel.setTrashOpen(true) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        )

        val dewBalance = ui.dew?.balance
        val dewText = if (dewBalance != null) {
            "${"%.3f".format(dewBalance).trimEnd('0').trimEnd('.')} Dew"
        } else {
            "Mino Plus"
        }

        NavigationDrawerItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_wallet), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            label = { Text(dewText, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
            selected = false,
            onClick = { onNavigate("plus") },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        )

        NavigationDrawerItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_coins), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            label = { Text("Support Mino", color = TextSecondary, fontSize = 13.sp) },
            selected = false,
            onClick = { onNavigate("donate") },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        )
    }
}

/** 3. Folder Filter Section */
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
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var menuFolderId by remember { mutableStateOf<String?>(null) }
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    var folderRenameText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FOLDERS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+ New",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
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
                            textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (folderRenameText.isNotBlank()) {
                                    onRenameFolder(folder.id, folderRenameText)
                                }
                                renamingFolderId = null
                            }),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SelectedContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
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

                    DropdownMenu(
                        expanded = menuFolderId == folder.id,
                        onDismissRequest = { menuFolderId = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = TextPrimary) },
                            onClick = {
                                folderRenameText = folder.name
                                renamingFolderId = folder.id
                                menuFolderId = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF5350)) },
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
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Folder", color = TextPrimary) },
            text = {
                BasicTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (newFolderName.isEmpty()) {
                            Text("Folder name", color = TextSecondary)
                        }
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
                    Text("Create", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCardSurface,
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
    Row(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(CircleShape)
            .background(if (active) SelectedContainer else DarkCardSurface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = if (active) TextPrimary else TextSecondary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp, end = if (onDelete != null) 4.dp else 0.dp),
        )
        if (onDelete != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete",
                tint = TextSecondary,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onDelete() },
            )
        }
    }
}

/** 4. Chat History List with M3 items, leading chat bubble/pin icons, and timestamps */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatHistoryList(
    chats: List<Chat>,
    activeChatId: String?,
    renamingChatId: String?,
    renameText: String,
    menuChatId: String?,
    folders: List<ChatFolder>,
    onSelectChat: (String) -> Unit,
    onMenuOpen: (String) -> Unit,
    onMenuClose: () -> Unit,
    onRenameStart: (Chat) -> Unit,
    onRenameChange: (String) -> Unit,
    onRenameDone: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onMoveToFolder: (String, String?) -> Unit,
    onDeleteChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()

    LazyColumn(modifier = modifier) {
        items(chats, key = { it.id }) { chat ->
            val isSelected = chat.id == activeChatId
            if (renamingChatId == chat.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = onRenameChange,
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onRenameDone(chat.id) }),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardSurface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            } else {
                Box {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                if (chat.pinned) Icons.Filled.PushPin else Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = null,
                                tint = if (chat.pinned) TextPrimary else if (isSelected) TextPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = {
                            Text(
                                text = chat.title,
                                color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        badge = {
                            Text(
                                text = Rules.timeAgo(now, chat.updatedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        },
                        selected = isSelected,
                        onClick = { onSelectChat(chat.id) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .combinedClickable(
                                onClick = { onSelectChat(chat.id) },
                                onLongClick = { onMenuOpen(chat.id) },
                            ),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SelectedContainer,
                            unselectedContainerColor = Color.Transparent,
                        ),
                    )

                    ChatRowMenu(
                        expanded = menuChatId == chat.id,
                        chatId = chat.id,
                        pinned = chat.pinned,
                        folders = folders,
                        onDismiss = onMenuClose,
                        onPin = { onTogglePin(chat.id, !chat.pinned) },
                        onMove = { folderId -> onMoveToFolder(chat.id, folderId) },
                        onRename = { onRenameStart(chat) },
                        onDelete = { onDeleteChat(chat.id) },
                    )
                }
            }
        }
    }
}

/** 5. Drawer Footer: Profile card anchored to bottom with Avatar, Display Name, and Settings */
@Composable
private fun DrawerFooter(
    ui: ChatViewModel.UiState,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(bottom = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardSurface)
                .clickable { onOpenSettings() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SelectedContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (ui.displayName ?: "M").take(1).uppercase(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
            ) {
                Text(
                    text = ui.displayName ?: "User",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "News",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onNavigate("notes") }
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = "About",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onNavigate("about") },
                    )
                }
            }

            IconButton(
                onClick = { onOpenSettings() },
                modifier = Modifier
                    .size(36.dp)
                    .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp),
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Contextual Menu for Chat Items */
@Composable
private fun ChatRowMenu(
    expanded: Boolean,
    chatId: String,
    pinned: Boolean,
    folders: List<ChatFolder>,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onMove: (String?) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var moveOpen by remember { mutableStateOf(false) }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (pinned) "Unpin" else "Pin", color = TextPrimary) },
            onClick = onPin,
            trailingIcon = { Icon(Icons.Outlined.PushPin, null, tint = TextSecondary) },
        )
        DropdownMenuItem(
            text = { Text("Move to folder", color = TextPrimary) },
            onClick = { moveOpen = true },
            trailingIcon = { Icon(Icons.Filled.Folder, null, tint = TextSecondary) },
        )
        if (moveOpen) {
            DropdownMenuItem(text = { Text("No folder", fontSize = 12.sp, color = TextPrimary) }, onClick = { onMove(null) })
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name, fontSize = 12.sp, color = TextPrimary) },
                    onClick = { onMove(folder.id) },
                )
            }
        }
        DropdownMenuItem(text = { Text("Rename", color = TextPrimary) }, onClick = onRename)
        DropdownMenuItem(
            text = { Text("Delete", color = DestructiveRed) },
            onClick = onDelete,
            trailingIcon = { Icon(Icons.Filled.Delete, null, tint = DestructiveRed) },
        )
    }
}
