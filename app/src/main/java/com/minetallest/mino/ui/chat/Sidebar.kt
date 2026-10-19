package com.minetallest.mino.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosIconBadge
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.ui.theme.MinoColors

/** iOS-style sidebar: logo + title header, new chat button, destinations and the recent chat list. */
@Composable
fun Sidebar(viewModel: ChatViewModel, onNavigate: (String) -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var menuChatId by remember { mutableStateOf<String?>(null) }
    var renamingChatId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sidebarBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        // ---- Header: logo + title + search icon + close icon ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = "Mino",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = "Mino",
                color = colors.textHi,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search chats",
                tint = colors.text,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { viewModel.setSearchOpen(true) },
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close sidebar",
                tint = colors.text,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { viewModel.setDrawer(false) },
            )
        }

        // ---- New chat button ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent)
                .clickable { viewModel.startNewChat() }
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "New chat",
                    color = colors.onAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        IosGroup {
            SidebarDest(
                title = "Gallery",
                icon = Icons.Filled.Image,
                tint = colors.text,
                onClick = { viewModel.setGalleryOpen(true) },
            )
            IosDivider(startIndent = 48.dp)
            SidebarDest(
                title = "Trash",
                icon = Icons.Filled.Delete,
                tint = colors.text,
                value = if (ui.trash.isNotEmpty()) ui.trash.size.toString() else null,
                onClick = { viewModel.setTrashOpen(true) },
            )
            IosDivider(startIndent = 48.dp)
            SidebarDest(
                title = "Mino Plus",
                icon = Icons.Filled.AccountBalanceWallet,
                tint = colors.text,
                value = ui.dew?.balance?.let {
                    "%.2f".format(it).trimEnd('0').trimEnd('.')
                } ?: "Plus",
                onClick = { onNavigate("plus") },
            )
            IosDivider(startIndent = 48.dp)
            SidebarDest(
                title = "Support",
                icon = Icons.Filled.Favorite,
                tint = colors.text,
                onClick = { onNavigate("donate") },
            )
            IosDivider(startIndent = 48.dp)
            SidebarDest(
                title = "News",
                icon = Icons.Filled.Campaign,
                tint = colors.text,
                onClick = { onNavigate("notes") },
            )
            IosDivider(startIndent = 48.dp)
            SidebarDest(
                title = "About",
                icon = Icons.Filled.Info,
                tint = colors.text,
                chevron = true,
                onClick = { onNavigate("about") },
            )
        }

        val pinned = ui.chats.filter { it.pinned }
        val others = ui.chats.filterNot { it.pinned }

        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = pullRefreshState.isRefreshing,
            onRefresh = {
                viewModel.refreshServerState()
                pullRefreshState.endRefresh()
            },
            modifier = Modifier.weight(1f),
        ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (pinned.isNotEmpty()) {
                item { IosSectionHeader("Pinned") }
                items(pinned, key = { it.id }) { chat ->
                    ChatRow(
                        chat = chat,
                        selected = chat.id == ui.activeChatId,
                        renaming = renamingChatId == chat.id,
                        renameText = renameText,
                        menuOpen = menuChatId == chat.id,
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
                        onDelete = { viewModel.deleteChat(chat.id) },
                    )
                }
            }
            item { IosSectionHeader("Recents") }
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
                    onDelete = { viewModel.deleteChat(chat.id) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
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
    onSelect: () -> Unit,
    onRenameChange: (String) -> Unit,
    onRenameDone: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuClose: () -> Unit,
    onRenameStart: () -> Unit,
    onTogglePin: () -> Unit,
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
                .background(if (selected) colors.bgRaised else Color.Transparent)
                .combinedClickable(onClick = onSelect, onLongClick = onMenuOpen)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Chat,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textMid,
                modifier = Modifier.size(18.dp).padding(end = 5.dp),
            )
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
            onDismiss = onMenuClose,
            onPin = onTogglePin,
            onRename = onRenameStart,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
private fun SidebarDest(
    title: String,
    icon: ImageVector,
    tint: Color,
    value: String? = null,
    chevron: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = colors.text,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, color = colors.textMid, fontSize = 17.sp)
            Spacer(Modifier.width(6.dp))
        }
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textLow,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ChatRowMenu(
    expanded: Boolean,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalMinoColors.current

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (pinned) "Unpin" else "Pin", color = colors.text) },
            onClick = onPin,
            trailingIcon = { Icon(Icons.Filled.PushPin, null, tint = colors.textMid, modifier = Modifier.size(18.dp)) },
        )
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
