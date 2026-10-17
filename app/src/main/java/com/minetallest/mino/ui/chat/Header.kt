package com.minetallest.mino.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
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
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Minimal navigation bar: menu · Mino · temporary chat · new chat. */
@Composable
fun ChatHeader(viewModel: ChatViewModel, showMenuButton: Boolean = true) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.pageBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (showMenuButton) {
                HeaderIcon(
                    icon = Icons.Filled.Menu,
                    description = "Chats",
                    tint = colors.accent,
                    onClick = { viewModel.setDrawer(true) },
                )
            }
        }
        Text(
            text = "Mino",
            color = colors.textHi,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIcon(
                icon = Icons.Filled.AccessTime,
                description = if (ui.temporary) "Temporary chat on" else "Temporary chat",
                tint = if (ui.temporary) colors.accent else colors.text,
                onClick = { viewModel.toggleTemporary() },
            )
            HeaderIcon(
                icon = Icons.Filled.Edit,
                description = "New chat",
                tint = colors.accent,
                onClick = { viewModel.startNewChat() },
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
}

@Composable
private fun HeaderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(21.dp))
    }
}
