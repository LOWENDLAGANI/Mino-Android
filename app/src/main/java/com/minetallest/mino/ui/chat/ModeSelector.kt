package com.minetallest.mino.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.ModeId
import com.minetallest.mino.domain.Models
import com.minetallest.mino.ui.theme.LocalMinoColors

private fun modeIcon(id: ModeId) = when (id) {
    ModeId.AUTO -> Icons.Filled.AutoAwesome
    ModeId.CODE -> Icons.Filled.Code
    ModeId.SELF -> Icons.Filled.Spa
}

/**
 * Spec §6.3 — summary button (active mode display name + chevron) opening a 320dp
 * radiogroup panel with icon, name, tagline, bullets, check/availability dot.
 */
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
                .clip(RoundedCornerShape(22.dp))
                .background(colors.bgRaised)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modeIcon(active.id),
                contentDescription = "Mode",
                tint = Color(active.color),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = active.display,
                color = colors.textHi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 6.dp),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.textMid,
                modifier = Modifier.size(16.dp),
            )
        }

        if (open) {
            androidx.compose.ui.window.Popup(
                onDismissRequest = { open = false },
                alignment = Alignment.TopEnd,
            ) {
                Surface(
                    modifier = Modifier.padding(8.dp).width(320.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.menuBackground,
                    shadowElevation = 12.dp,
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Models.modes.forEach { mode ->
                            val available = availability?.available?.contains(mode.id) != false
                            ModeCard(
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
private fun ModeCard(
    mode: com.minetallest.mino.domain.ModeInfo,
    selected: Boolean,
    available: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.bgHover else Color.Transparent)
            .clickable(enabled = available) { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(mode.color).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(modeIcon(mode.id), contentDescription = null, tint = Color(mode.color), modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mode.display,
                    color = colors.textHi,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                if (!available) {
                    Icon(Icons.Filled.Lock, contentDescription = "Unavailable", tint = colors.textLow, modifier = Modifier.size(12.dp))
                }
            }
            Text(text = mode.tagline, color = colors.textMid, fontSize = 12.sp)
            mode.bullets.forEach { bullet ->
                Text(text = "· $bullet", color = colors.textLow, fontSize = 11.sp)
            }
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = colors.accent, modifier = Modifier.size(18.dp))
        } else if (available) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}
