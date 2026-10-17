package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosSheet
import com.minetallest.mino.ui.theme.IosSheetHandle
import com.minetallest.mino.ui.theme.LocalMinoColors

/** A new developer note, surfaced once per reader. */
@Composable
fun NotesPrompt(viewModel: ChatViewModel, onOpenNotes: () -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val newest = ui.config.notes.firstOrNull { it.id in ui.newNoteIds } ?: return

    IosSheet(onDismiss = { viewModel.markNotesSeen(ui.newNoteIds) }) {
        IosSheetHandle()
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "News",
                color = colors.textHi,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Done",
                color = colors.accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.markNotesSeen(ui.newNoteIds) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Text(
            text = newest.summary.ifBlank { newest.body },
            color = colors.text,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable {
                    viewModel.markNotesSeen(ui.newNoteIds)
                    onOpenNotes()
                },
        )
    }
}
