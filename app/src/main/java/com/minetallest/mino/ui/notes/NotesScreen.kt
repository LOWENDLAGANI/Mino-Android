package com.minetallest.mino.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.net.DevNote
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Public developer notes, newest first. */
@Composable
fun NotesScreen(onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    var opened by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().widthIn(max = 680.dp)) {
            IosNavBar(title = "News", onBack = onBack)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            val context = LocalContext.current
            val container = remember { (context.applicationContext as com.minetallest.mino.MinoApp).container }
            var notes by remember { mutableStateOf<com.minetallest.mino.net.AppConfig?>(null) }
            LaunchedEffect(Unit) { notes = container.config.read() }

            val list = notes?.notes.orEmpty()
            if (list.isEmpty()) {
                IosSectionHeader("Updates")
                IosGroup {
                    Text(
                        text = "No news",
                        color = colors.textMid,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            list.forEach { note ->
                NoteCard(
                    note = note,
                    expanded = opened == note.id,
                    onToggle = { opened = if (opened == note.id) null else note.id },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun NoteCard(note: DevNote, expanded: Boolean, onToggle: () -> Unit) {
    val colors = LocalMinoColors.current
    val toneColor = when (note.tone) {
        "important" -> colors.amber
        "info" -> colors.azure
        "success" -> colors.mint
        else -> colors.textMid
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgRaised)
            .clickable { onToggle() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.badge.isNotBlank()) {
                Text(
                    text = note.badge,
                    color = toneColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.fill)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
                Spacer(Modifier.size(6.dp))
            }
            Text(
                text = note.date,
                color = colors.textMid,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.textMid,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = note.summary.ifBlank { note.body.take(120) },
            color = colors.textHi,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (note.byline.isNotBlank()) {
            Text(text = note.byline, color = colors.textMid, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (expanded && note.body.isNotBlank()) {
            Text(
                text = note.body,
                color = colors.text,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
