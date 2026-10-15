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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.LocalSearch
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn

/** Spec §10.6 — palette search: titles + message text, newest first, ≤30, opens the chat. */
@Composable
fun SearchOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { viewModel.searchAll(query) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.65f))
            .clickable { viewModel.setSearchOpen(false) },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.menuBackground)
                .clickable { /* consume */ }
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = colors.textMid, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textHi, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search chats…", color = colors.textLow, fontSize = 15.sp)
                        }
                        inner()
                    },
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = colors.textMid,
                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).clickable { viewModel.setSearchOpen(false) },
                )
            }
            if (query.trim().length >= LocalSearch.MIN_QUERY) {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                    if (hits.isEmpty()) {
                        item { Text("No matches.", color = colors.textLow, fontSize = 12.sp, modifier = Modifier.padding(12.dp)) }
                    }
                    items(hits, key = { it.chat.id }) { hit ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectChat(hit.chat.id)
                                    viewModel.setSearchOpen(false)
                                }
                                .padding(10.dp),
                        ) {
                            Text(hit.chat.title, color = colors.textHi, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(hit.snippet, color = colors.textMid, fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
