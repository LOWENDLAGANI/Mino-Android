package com.minetallest.mino.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.minetallest.mino.net.DevNote
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Spec §10.12 — News from developers: public config notes, newest first. */
@Composable
fun NotesScreen(onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    var opened by remember { mutableStateOf<String?>(null) }

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
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onBack() }.padding(6.dp),
            )
            Text("  News From Developers", color = colors.textHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val container = remember { (context.applicationContext as com.minetallest.mino.MinoApp).container }
        var notes by remember { mutableStateOf<com.minetallest.mino.net.AppConfig?>(null) }
        androidx.compose.runtime.LaunchedEffect(Unit) { notes = container.config.read() }

        val list = notes?.notes.orEmpty()
        if (list.isEmpty()) {
            Text(
                text = "No news right now. When the developers post something, it lands here.",
                color = colors.textLow,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        list.forEach { note ->
            NoteCard(
                note = note,
                expanded = opened == note.id,
                onToggle = { opened = if (opened == note.id) null else note.id },
            )
        }
    }
}
}

@Composable
private fun NoteCard(note: DevNote, expanded: Boolean, onToggle: () -> Unit) {
    val colors = LocalMinoColors.current
    val toneColor = when (note.tone) {
        "important" -> colors.amber
        "info" -> colors.azure
        "success" -> colors.accent
        else -> colors.textMid
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.bgRaised)
            .clickable { onToggle() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.badge.isNotBlank()) {
                Text(
                    text = note.badge,
                    color = toneColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
            }
            Text(
                text = note.date,
                color = colors.textLow,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.textLow,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = note.summary.ifBlank { note.body.take(120) },
            color = colors.textHi,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (note.byline.isNotBlank()) {
            Text(text = note.byline, color = colors.textLow, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (expanded && note.body.isNotBlank()) {
            Text(
                text = note.body,
                color = colors.textMid,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
