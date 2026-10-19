package com.minetallest.mino.ui.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.minetallest.mino.domain.Chat
import com.minetallest.mino.ui.about.AboutScreen
import com.minetallest.mino.ui.admin.AdminScreen
import com.minetallest.mino.ui.donate.DonateScreen
import com.minetallest.mino.ui.notes.NotesScreen
import com.minetallest.mino.ui.overlays.GalleryOverlay
import com.minetallest.mino.ui.overlays.NotesPrompt
import com.minetallest.mino.ui.overlays.RememberDialog
import com.minetallest.mino.ui.overlays.SearchOverlay
import com.minetallest.mino.ui.overlays.SettingsScreen
import com.minetallest.mino.ui.overlays.TrashOverlay
import com.minetallest.mino.ui.plus.PlusScreen
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.LocalMinoColors
import com.minetallest.mino.util.SharedContentHandler

/** Main shell: sidebar drawer · header · banners · thread · composer · overlays. */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val nav = rememberNavController()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = "chat") {
        composable(
            route = "share_preview",
            deepLinks = listOf(
                navDeepLink { action = Intent.ACTION_SEND; mimeType = "text/plain" },
                navDeepLink { action = Intent.ACTION_SEND; mimeType = "image/*" },
                navDeepLink { action = Intent.ACTION_SEND_MULTIPLE; mimeType = "image/*" },
                navDeepLink { action = Intent.ACTION_SEND_MULTIPLE; mimeType = "*/*" },
            )
        ) {
            SharePreviewScreen(viewModel = viewModel, onDismiss = { nav.popBackStack() })
        }
        composable("chat") {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 700.dp
                if (isWideScreen) {
                    Row(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                                .background(colors.sidebarBackground),
                        ) {
                            Sidebar(viewModel = viewModel, onNavigate = { route -> nav.navigate(route) })
                        }
                        Box(Modifier.width(0.5.dp).fillMaxHeight().background(colors.line))
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            ChatPage(viewModel = viewModel, showMenuButton = false)
                        }
                    }
                } else {
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    LaunchedEffect(ui.drawerOpen) {
                        if (ui.drawerOpen) drawerState.open() else drawerState.close()
                    }
                    LaunchedEffect(drawerState.isOpen) {
                        if (drawerState.isOpen != ui.drawerOpen) viewModel.setDrawer(drawerState.isOpen)
                    }
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = colors.sidebarBackground,
                                drawerContentColor = colors.text,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                            ) {
                                Sidebar(viewModel = viewModel, onNavigate = { route -> nav.navigate(route) })
                            }
                        },
                    ) {
                        ChatPage(viewModel = viewModel, showMenuButton = true)
                    }
                }
            }
        }
        composable("plus") {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                PlusScreen(viewModel = viewModel, onBack = { nav.popBackStack() })
            }
        }
        composable("donate") {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                DonateScreen(onBack = { nav.popBackStack() })
            }
        }
        composable("about") {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                AboutScreen(
                    onBack = { nav.popBackStack() },
                    onAdmin = { nav.navigate("admin") },
                )
            }
        }
        composable("notes") {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                NotesScreen(onBack = { nav.popBackStack() })
            }
        }
        composable("admin") {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                AdminScreen(viewModel = viewModel, onBack = { nav.popBackStack() })
            }
        }
        composable("settings") {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                SettingsScreen(viewModel = viewModel, onBack = { nav.popBackStack() })
            }
        }
    }

    if (ui.searchOpen) SearchOverlay(viewModel = viewModel)
    if (ui.trashOpen) TrashOverlay(viewModel = viewModel)
    if (ui.galleryOpen) GalleryOverlay(viewModel = viewModel)
    if (ui.rememberDialogOpen) RememberDialog(viewModel = viewModel)
    if (ui.newNoteIds.isNotEmpty() && ui.displayName != null && ui.searchOpen.not()) {
        NotesPrompt(viewModel = viewModel, onOpenNotes = { nav.navigate("notes") })
    }
}

@Composable
private fun ChatPage(viewModel: ChatViewModel, showMenuButton: Boolean = true) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = colors.pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ChatHeader(viewModel, showMenuButton = showMenuButton) },
        bottomBar = { Composer(viewModel) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.pageBackground)
                .padding(padding),
        ) {
            val announcement = ui.config.announcement
            if (announcement.isNotBlank()) {
                Banner(
                    text = announcement,
                    background = colors.accentSoft,
                    textColor = colors.accent,
                    onDismiss = null,
                )
            }
            if (ui.modelNotice) {
                Banner(
                    text = com.minetallest.mino.domain.Refusals.MODEL_CHANGED,
                    background = colors.amberSoft,
                    textColor = colors.amber,
                    onDismiss = null,
                )
            }
            NoticeStrip(viewModel)
            Thread(viewModel)
        }
    }

    if (ui.sharePreviewOpen) {
        SharePreviewSheet(viewModel = viewModel, onDismiss = { viewModel.dismissSharePreview() })
    }
}

/** Bottom sheet showing shared content preview with chat selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharePreviewSheet(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Get the pending shared content from the viewModel
    val pendingContent = (viewModel as? ChatViewModel)?.let { vm ->
        // Access the pending content through reflection-like approach
        // We'll use the ui state to determine what to show
        null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.menuBackground,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.lineStrong) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Share to Mino",
                    color = colors.textHi,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Dismiss", tint = colors.text, modifier = Modifier.size(20.dp))
                }
            }

            IosDivider()

            Spacer(Modifier.height(16.dp))

            // Chat selection
            Text(
                "Send to chat",
                color = colors.textMid,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    ChatSelectionRow(
                        title = "New chat",
                        subtitle = null,
                        selected = !ui.activeChatId?.let { _ -> true } ?: true,
                        onClick = { viewModel.sendPendingShare() },
                        colors = colors,
                    )
                }

                items(ui.chats.take(10)) { chat ->
                    ChatSelectionRow(
                        title = chat.title,
                        subtitle = null,
                        selected = false,
                        onClick = { viewModel.sendShareToChat(chat.id) },
                        colors = colors,
                    )
                }

                if (ui.chats.size > 10) {
                    item {
                        Text(
                            "+ ${ui.chats.size - 10} more chats",
                            color = colors.textMid,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "Send",
                    color = colors.onAccent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.sendPendingShare() }
                        .background(colors.accent)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatSelectionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    colors: LocalMinoColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Send,
                "Chat",
                tint = if (selected) colors.accent else colors.textMid,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = title,
            color = colors.text,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
