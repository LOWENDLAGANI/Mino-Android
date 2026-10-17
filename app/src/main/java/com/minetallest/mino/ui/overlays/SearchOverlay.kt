package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.domain.LocalSearch
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Search overlay: search bar at top, "Recents" header, chat list with bubble icons and selected state. */
@Composable
fun SearchOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { viewModel.searchAll(query) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val activeId = ui.activeChatId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ---- Search bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.fill)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.textMid,
                    modifier = Modifier.size(18.dp),
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search chats", color = colors.textMid, fontSize = 17.sp)
                        }
                        inner()
                    },
                )
            }
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close search",
                tint = colors.textMid,
                modifier = Modifier
                    .size(20.dp)
                    .padding(start = 8.dp)
                    .clickable { viewModel.setSearchOpen(false) },
            )
        }

        // ---- Recents header ----
        Text(
            text = "Recents",
            color = colors.textMid,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        if (query.trim().length >= LocalSearch.MIN_QUERY) {
            // ---- Search results ----
            if (hits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No results",
                        color = colors.textMid,
                        fontSize = 15.sp,
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(hits, key = { it.chat.id }) { hit ->
                    SearchResultRow(
                        chat = hit.chat,
                        snippet = hit.snippet,
                        selected = hit.chat.id == activeId,
                        colors = colors,
                        onClick = {
                            viewModel.selectChat(hit.chat.id)
                            viewModel.setSearchOpen(false)
                        },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        } else {
            // ---- Recent chats list ----
            val chats = ui.chats
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No chats",
                        color = colors.textMid,
                        fontSize = 15.sp,
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(chats, key = { it.id }) { chat ->
                    SearchResultRow(
                        chat = chat,
                        snippet = null,
                        selected = chat.id == activeId,
                        colors = colors,
                        onClick = {
                            viewModel.selectChat(chat.id)
                            viewModel.setSearchOpen(false)
                        },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    chat: com.minetallest.mino.domain.Chat,
    snippet: String?,
    selected: Boolean,
    colors: LocalMinoColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.bgRaised else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Chat,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.textMid,
            modifier = Modifier.size(18.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = chat.title,
                color = if (selected) colors.textHi else colors.text,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (snippet != null) {
                Text(
                    text = snippet,
                    color = colors.textMid,
                    fontSize = 14.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = Rules.timeAgo(System.currentTimeMillis(), chat.updatedAt),
            color = colors.textMid,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
