package com.minetallest.mino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.R
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors

/**
 * Spec §6.2 — NamePrompt: one field (max 40), Save, no skip. Nothing else is reachable
 * until a display name exists (acceptance #2). Firebase Google binding is a documented
 * deviation for a future pass.
 */
@Composable
fun NamePromptScreen(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var name by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            Text(
                text = "Welcome to Mino",
                color = colors.textHi,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
            )
            Text(
                text = "A private, local-first AI assistant.\nWhat should Mino call you?",
                color = colors.textMid,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= Rules.NAME_MAX) name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Your display name", color = colors.textLow) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) viewModel.saveDisplayName(name)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textHi,
                    unfocusedTextColor = colors.textHi,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.lineStrong,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = colors.accent,
                ),
            )
            Button(
                onClick = { viewModel.saveDisplayName(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryButton,
                    contentColor = Color.White,
                    disabledContainerColor = colors.bgElevated,
                    disabledContentColor = colors.textLow,
                ),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Text(
                text = "Your chats stay on this device. No account needed.",
                color = colors.textLow,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}
