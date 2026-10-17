package com.minetallest.mino.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Main shell: sidebar drawer · header · banners · thread · composer · overlays. */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val nav = rememberNavController()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = "chat") {
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
}
