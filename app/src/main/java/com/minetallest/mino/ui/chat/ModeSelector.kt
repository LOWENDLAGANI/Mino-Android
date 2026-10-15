package com.minetallest.mino.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.ModeId
import com.minetallest.mino.domain.Models

private fun modeIcon(id: ModeId) = when (id) {
    ModeId.AUTO -> Icons.Filled.AutoAwesome
    ModeId.CODE -> Icons.Filled.Code
    ModeId.SELF -> Icons.Filled.Spa
}

// Strict Palette Constants
private val TextPrimary = Color(0xFFF4F4F5)
private val TextSecondary = Color(0xFFA1A1AA)
private val CardSurface = Color(0xFF1E1E22)
private val SelectedContainer = Color(0xFF27272A)

/**
 * Model Selector Pill Component with grounded human-feel animation (fadeIn + slideInVertically).
 */
@Composable
fun ModeSelector(viewModel: ChatViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val active = Models.info(ui.mode)
    val availability = ui.availability

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SelectedContainer)
                .clickable { open = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modeIcon(active.id),
                contentDescription = "Mode",
                tint = TextPrimary,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = active.display,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 5.dp),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(200), initialOffsetY = { it / 20 }),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(150), targetOffsetY = { it / 20 }),
        ) {
            androidx.compose.ui.window.Popup(
                onDismissRequest = { open = false },
                alignment = Alignment.TopStart,
            ) {
                Surface(
                    modifier = Modifier.padding(8.dp).width(300.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurface,
                    shadowElevation = 12.dp,
                ) {
                    Column(Modifier.padding(8.dp)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SelectedContainer else Color.Transparent)
            .clickable(enabled = available) { onSelect() }
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SelectedContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(modeIcon(mode.id), contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mode.display,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                if (!available) {
                    Icon(Icons.Filled.Lock, contentDescription = "Unavailable", tint = TextSecondary, modifier = Modifier.size(12.dp))
                }
            }
            Text(text = mode.tagline, color = TextSecondary, fontSize = 11.sp)
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}
