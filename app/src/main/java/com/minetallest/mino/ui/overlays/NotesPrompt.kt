package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue

/** Spec §10.12 — a new note is shown once per reader; ids marked seen when the sheet opens. */
@Composable
fun NotesPrompt(viewModel: ChatViewModel, onOpenNotes: () -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val newest = ui.config.notes.firstOrNull { it.id in ui.newNoteIds } ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
            .clickable {
                viewModel.markNotesSeen(ui.newNoteIds)
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(420.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.menuBackground)
                .clickable { }
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("News From Developers", color = colors.textHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = colors.textMid,
                    modifier = Modifier.size(15.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        viewModel.markNotesSeen(ui.newNoteIds)
                    },
                )
            }
            Text(
                text = newest.summary.ifBlank { newest.body },
                color = colors.textMid,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp).clickable {
                    viewModel.markNotesSeen(ui.newNoteIds)
                    onOpenNotes()
                },
            )
        }
    }
}
