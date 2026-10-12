package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
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
import com.minetallest.mino.domain.Variants
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue

/** Spec §10.4 — Trash: 30 days on-device, restore or delete permanently. */
@Composable
fun TrashOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.65f))
            .clickable { viewModel.setTrashOpen(false) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.menuBackground)
                .clickable { }
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trash", color = colors.textHi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = colors.textMid,
                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).clickable { viewModel.setTrashOpen(false) },
                )
            }
            Text(
                "Deleted chats stay here for 30 days on this device.",
                color = colors.textLow,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            if (ui.trash.isEmpty()) {
                Text("Nothing here.", color = colors.textLow, fontSize = 12.sp, modifier = Modifier.padding(vertical = 18.dp))
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(ui.trash, key = { it.id }) { entry ->
                    val daysLeft =
                        (((entry.deletedAt + Variants.TRASH_RETENTION_MS) - now) / 86_400_000L).coerceAtLeast(0)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, color = colors.text, fontSize = 13.sp, maxLines = 1)
                            Text(
                                "${daysLeft}d left · ${entry.messages.size} messages",
                                color = colors.textLow,
                                fontSize = 10.sp,
                            )
                        }
                        Icon(
                            Icons.Filled.RestoreFromTrash,
                            contentDescription = "Restore",
                            tint = colors.accent,
                            modifier = Modifier.padding(end = 12.dp).size(16.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                viewModel.restoreChat(entry.id)
                                viewModel.setTrashOpen(false)
                            },
                        )
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = "Delete permanently",
                            tint = colors.danger,
                            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                viewModel.forgetTrash(entry.id)
                            },
                        )
                    }
                }
            }
        }
    }
}
