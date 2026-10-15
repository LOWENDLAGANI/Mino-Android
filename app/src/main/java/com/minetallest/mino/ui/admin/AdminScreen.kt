package com.minetallest.mino.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.MinoApp
import com.minetallest.mino.net.AppConfig
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch

/**
 * Spec §12 — the admin console, reachable only after the ten-tap gate on /about
 * (and only after the server rules confirm `isAdmin`).
 *
 * Nine sections: Diagnostics · People · Runtime controls · Banned · Redeem codes ·
 * Notes · Model health · Push · Wipes. Every write is sent with this device's own
 * token so the rules make the final call — the client never decides who admin is.
 */
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
    var identityHint by remember { mutableStateOf<String?>(null) }

    val sections = listOf(
        "Diagnostics", "People", "Controls", "Banned",
        "Codes", "Notes", "Models", "Push", "Wipes",
    )

    fun report(message: String?) {
        busy = null
        status = message
    }

    LaunchedEffect(Unit) {
        identityHint = container.identity.uid()
        val result = admin.readConfig()
        if (result != null) {
            isAdmin = result.first
            result.second?.let { config = it }
        } else {
            config = ui.config
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() }
                    .padding(6.dp),
            )
            Text("  Admin console", color = colors.textHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            text = when (isAdmin) {
                true -> "Verified by the rules: this device is the admin."
                false -> "Refused by the rules. " +
                    "Show my email: ${identityHint ?: "unidentified on this device"}"
                null -> "Checking identity…"
            },
            color = if (isAdmin == false) colors.amber else colors.textMid,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.bgRaised)
                .horizontalScroll(rememberScrollState())
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            sections.forEachIndexed { index, label ->
                Text(
                    text = label,
                    color = if (section == index) colors.onAccent else colors.textMid,
                    fontSize = 11.sp,
                    fontWeight = if (section == index) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (section == index) colors.accent else colors.background)
                        .clickable { section = index }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                )
            }
        }

        busy?.let {
            Text(it, color = colors.amber, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        status?.let {
            Text(it, color = colors.mint, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }

        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
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
