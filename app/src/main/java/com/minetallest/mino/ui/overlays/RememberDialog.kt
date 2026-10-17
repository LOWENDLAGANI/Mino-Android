package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.domain.MemoryGuard
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosSheet
import com.minetallest.mino.ui.theme.IosSheetHandle
import com.minetallest.mino.ui.theme.LocalMinoColors

/** "Remember" sheet with the guard's validation message. */
@Composable
fun RememberDialog(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var text by remember { mutableStateOf("") }
    var rejection by remember { mutableStateOf<String?>(null) }

    IosSheet(onDismiss = { viewModel.setRememberDialog(false) }) {
        IosSheetHandle()
        Text(
            text = "Remember",
            color = colors.textHi,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        BasicTextField(
            value = text,
            onValueChange = {
                if (it.length <= Refusals.MEMORY_TEXT_LIMIT) {
                    text = it
                    rejection = null
                }
            },
            textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp, lineHeight = 23.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.fill)
                .height(88.dp)
                .padding(12.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text("Something to remember", color = colors.textMid, fontSize = 17.sp)
                }
                inner()
            },
        )
        rejection?.let { reason ->
            Text(
                text = reason,
                color = colors.amber,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(
                "Cancel",
                color = colors.accent,
                fontSize = 17.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.setRememberDialog(false) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                "Save",
                color = colors.onAccent,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable {
                        when (val result = viewModel.remember(text)) {
                            is MemoryGuard.Result.Ok -> {
                                viewModel.notice("Remembered", ChatViewModel.Notice.Tone.SUCCESS)
                                viewModel.setRememberDialog(false)
                            }
                            is MemoryGuard.Result.Rejected -> rejection = result.reason
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}
