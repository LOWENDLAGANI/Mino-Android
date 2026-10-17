package com.minetallest.mino.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.MinoApp
import com.minetallest.mino.net.AppConfig
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Admin console, reachable only after the ten-tap gate confirms `isAdmin`. */
@Composable
fun AdminScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val container = remember { (context.applicationContext as MinoApp).container }
    val admin = container.adminApi
    val scope = rememberCoroutineScope()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var section by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var isAdmin by remember { mutableStateOf<Boolean?>(null) }

    val sections = listOf(
        "Diagnostics", "People", "Controls", "Banned",
        "Codes", "Notes", "Models", "Push", "Wipes",
    )

    fun report(message: String?) {
        busy = null
        status = message
    }

    LaunchedEffect(Unit) {
        val result = admin.readConfig()
        if (result != null) {
            isAdmin = result.first
            result.second?.let { config = it }
        } else {
            config = ui.config
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().widthIn(max = 680.dp)) {
            IosNavBar(
                title = "Admin",
                onBack = onBack,
                trailing = {
                    Text(
                        text = when (isAdmin) {
                            true -> "Admin"
                            false -> "Denied"
                            null -> "…"
                        },
                        color = if (isAdmin == false) colors.danger else colors.textMid,
                        fontSize = 13.sp,
                    )
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            sections.forEachIndexed { index, label ->
                val selected = section == index
                Text(
                    text = label,
                    color = if (selected) colors.onAccent else colors.text,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) colors.accent else colors.bgRaised)
                        .clickable { section = index }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            busy?.let {
                Text(it, color = colors.amber, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            status?.let {
                Text(it, color = colors.mint, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 28.dp)) {
                when (section) {
                    0 -> DiagnosticsSection(viewModel, config)
                    1 -> PeopleSection(admin, scope, { busy = it }, { report(it) })
                    2 -> ControlsSection(
                        config = config,
                        scope = scope,
                        admin = admin,
                        onSaved = { next ->
                            config = next
                            viewModel.refreshServerState()
                        },
                        onBusy = { busy = it },
                        onDone = { report(it) },
                    )
                    3 -> BansSection(admin, scope, { busy = it }, { report(it) })
                    4 -> CodesSection(admin, scope, { busy = it }, { report(it) })
                    5 -> NotesSection(
                        config = config,
                        scope = scope,
                        admin = admin,
                        onSaved = { next ->
                            config = next
                            viewModel.refreshServerState()
                        },
                        onBusy = { busy = it },
                        onDone = { report(it) },
                    )
                    6 -> ModelsSection(admin, scope, { busy = it }, { report(it) })
                    7 -> PushSection(admin, scope, { busy = it }, { report(it) })
                    8 -> WipesSection(admin, scope, { busy = it }, { report(it) })
                }
            }
        }
    }
}
