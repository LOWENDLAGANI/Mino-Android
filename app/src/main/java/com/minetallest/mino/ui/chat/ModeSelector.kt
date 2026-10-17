package com.minetallest.mino.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.ModeId
import com.minetallest.mino.domain.Models
import com.minetallest.mino.ui.theme.LocalMinoColors

private fun modeIcon(id: ModeId): ImageVector = when (id) {
    ModeId.AUTO -> Icons.Filled.AutoAwesome
    ModeId.CODE -> Icons.Filled.Code
    ModeId.SELF -> Icons.Filled.Spa
}

@Composable
fun ModeSelector(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val active = Models.info(ui.mode)
    val availability = ui.availability

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(colors.fill)
                .clickable { open = true }
                .padding(start = 8.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modeIcon(active.id),
                contentDescription = "Mode",
                tint = colors.accent,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = active.display,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 5.dp),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.textMid,
                modifier = Modifier.size(15.dp),
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(120)) + scaleIn(tween(140), initialScale = 0.94f),
            exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.96f),
        ) {
            androidx.compose.ui.window.Popup(onDismissRequest = { open = false }, alignment = Alignment.TopStart) {
                Surface(
                    modifier = Modifier.padding(8.dp).width(240.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.menuBackground,
                    shadowElevation = 12.dp,
                ) {
                    Column(Modifier.padding(6.dp)) {
                        Models.modes.forEach { mode ->
                            val available = availability?.available?.contains(mode.id) != false
                            ModeRow(
                                mode = mode,
                                selected = mode.id == ui.mode,
                                available = available,
                                onSelect = {
                                    open = false
                                    viewModel.setMode(mode.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    mode: com.minetallest.mino.domain.ModeInfo,
    selected: Boolean,
    available: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) colors.fill else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = available) { onSelect() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(modeIcon(mode.id), contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
        }
        Text(
            text = mode.display,
            color = if (available) colors.text else colors.textMid,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f).padding(start = 9.dp),
        )
        if (!available) {
            Icon(Icons.Filled.Lock, contentDescription = "Unavailable", tint = colors.textMid, modifier = Modifier.size(13.dp))
        } else if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.accent, modifier = Modifier.size(16.dp))
        }
    }
}
