package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.domain.MemoryGuard
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors

import androidx.compose.foundation.layout.widthIn

/** Spec §6.8 — "Remember this" dialog with the guard's amber rejection notice. */
@Composable
fun RememberDialog(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var text by remember { mutableStateOf("") }
    var rejection by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { viewModel.setRememberDialog(false) },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.bgRaised)
                .clickable { }
                .padding(20.dp),
        ) {
            Text("What should Mino remember?", color = colors.textHi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "One short sentence about you. Mino uses it in every conversation until you remove it.",
                color = colors.textMid,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            BasicTextField(
                value = text,
                onValueChange = {
                    if (it.length <= Refusals.MEMORY_TEXT_LIMIT) {
                        text = it
                        rejection = null
                    }
                },
                textStyle = TextStyle(color = colors.textHi, fontSize = 14.sp, lineHeight = 20.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bgElevated)
                    .height(86.dp)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("I work mostly in TypeScript and React", color = colors.textLow, fontSize = 13.sp)
                    }
                    inner()
                },
            )
            rejection?.let { reason ->
                Text(
                    text = reason,
                    color = colors.amber,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cancel",
                    color = colors.textMid,
                    fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(22.dp)).clickable { viewModel.setRememberDialog(false) }.padding(horizontal = 14.dp, vertical = 9.dp),
                )
                Box(Modifier.weight(1f))
                Text(
                    "Remember",
                    color = Color(0xFF10231A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFA9D8BB), Color(0xFF1F4A33))))
                        .clickable {
                            when (val result = viewModel.remember(text)) {
                                is MemoryGuard.Result.Ok -> {
                                    viewModel.notice("Mino will remember that", ChatViewModel.Notice.Tone.SUCCESS)
                                    viewModel.setRememberDialog(false)
                                }
                                is MemoryGuard.Result.Rejected -> rejection = result.reason
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}
