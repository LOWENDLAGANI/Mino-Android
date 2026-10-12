package com.minetallest.mino.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.minetallest.mino.ui.overlays.SettingsPanel
import com.minetallest.mino.ui.overlays.TrashOverlay
import com.minetallest.mino.ui.plus.PlusScreen
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Main shell: sidebar drawer · header · banners · thread · composer · overlays (spec §6). */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val nav = rememberNavController()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = "chat") {
        composable("chat") {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            LaunchedEffect(ui.drawerOpen) {
                if (ui.drawerOpen) drawerState.open() else drawerState.close()
            }
            LaunchedEffect(drawerState.isOpen) {
                if (drawerState.isOpen != ui.drawerOpen) viewModel.setDrawer(drawerState.isOpen)
            }
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = ui.drawerOpen,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = colors.sidebarBackground,
                        drawerContentColor = colors.text,
                    ) {
                        Sidebar(viewModel = viewModel, onNavigate = { route -> nav.navigate(route) })
                    }
                },
            ) {
                ChatPage(viewModel = viewModel)
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
    }

    // Overlays that live above everything (spec §6 stack).
    if (ui.searchOpen) SearchOverlay(viewModel = viewModel)
    if (ui.trashOpen) TrashOverlay(viewModel = viewModel)
    if (ui.galleryOpen) GalleryOverlay(viewModel = viewModel)
    if (ui.settingsOpen) SettingsPanel(viewModel = viewModel)
    if (ui.rememberDialogOpen) RememberDialog(viewModel = viewModel)
    if (ui.newNoteIds.isNotEmpty() && ui.displayName != null && ui.searchOpen.not() && ui.settingsOpen.not()) {
        NotesPrompt(viewModel = viewModel, onOpenNotes = { nav.navigate("notes") })
    }
}

/** Spec §6.6 — banners, notices and the scrolling thread over the composer. */
@Composable
private fun ChatPage(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = colors.pageBackground,
        topBar = { ChatHeader(viewModel) },
        bottomBar = { Composer(viewModel) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
