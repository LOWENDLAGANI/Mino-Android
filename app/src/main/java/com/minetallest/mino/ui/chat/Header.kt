package com.minetallest.mino.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.R

// Strict Dark Charcoal Palette
private val TextPrimary = Color(0xFFF4F4F5)
private val TextSecondary = Color(0xFFA1A1AA)
private val DarkCardSurface = Color(0xFF1E1E22)
private val SelectedContainer = Color(0xFF27272A)

/**
 * Top Header Component (Claude Mobile style).
 * Clean header with Hamburger icon on left, brand title, and Temporary Chat ghost toggle on right.
 */
@Composable
fun ChatHeader(viewModel: ChatViewModel, showMenuButton: Boolean = true) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showMenuButton) {
            IconButton(onClick = { viewModel.setDrawer(true) }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextPrimary)
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.startNewChat() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Mino",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        // Top Right Temporary Chat Ghost Toggle Pill
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (ui.temporary) SelectedContainer else Color.Transparent)
                .clickable { viewModel.toggleTemporary() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ui.temporary) Icons.Filled.AccessTimeFilled else Icons.Outlined.AccessTime,
                    contentDescription = "Temporary Chat",
                    tint = if (ui.temporary) TextPrimary else TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                if (ui.temporary) {
                    Text(
                        text = " Temp",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
