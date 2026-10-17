package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Icon
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
import com.minetallest.mino.domain.Variants
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosSheet
import com.minetallest.mino.ui.theme.IosSheetHandle
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Trash: restore or delete permanently before the 30-day retention ends. */
@Composable
fun TrashOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    IosSheet(onDismiss = { viewModel.setTrashOpen(false) }) {
        IosSheetHandle()
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Trash",
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
                    .clickable { viewModel.setTrashOpen(false) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        if (ui.trash.isEmpty()) {
            Text(
                "No chats",
                color = colors.textMid,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }

        LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
            items(ui.trash, key = { it.id }) { entry ->
                val daysLeft =
                    (((entry.deletedAt + Variants.TRASH_RETENTION_MS) - now) / 86_400_000L).coerceAtLeast(0)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, color = colors.text, fontSize = 16.sp, maxLines = 1)
                        Text(
                            "${entry.messages.size} messages · ${daysLeft}d left",
                            color = colors.textMid,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(
                        Icons.Filled.RestoreFromTrash,
                        contentDescription = "Restore",
                        tint = colors.accent,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable {
                                viewModel.restoreChat(entry.id)
                                viewModel.setTrashOpen(false)
                            },
                    )
                    Icon(
                        Icons.Filled.DeleteForever,
                        contentDescription = "Delete permanently",
                        tint = colors.danger,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.forgetTrash(entry.id) },
                    )
                }
            }
        }
    }
}
