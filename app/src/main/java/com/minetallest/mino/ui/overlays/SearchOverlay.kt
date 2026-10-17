package com.minetallest.mino.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.minetallest.mino.domain.LocalSearch
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Full-screen search: field at the top, live results below. */
@Composable
fun SearchOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { viewModel.searchAll(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
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
                Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textMid, modifier = Modifier.padding(horizontal = 2.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search", color = colors.textMid, fontSize = 17.sp)
                        inner()
                    },
                )
            }
            Text(
                text = "Cancel",
                color = colors.accent,
                fontSize = 17.sp,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.setSearchOpen(false) }
                    .padding(vertical = 6.dp),
            )
        }

        if (query.trim().length >= LocalSearch.MIN_QUERY) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (hits.isEmpty()) {
                    item {
                        Text(
                            "No results",
                            color = colors.textMid,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }
                items(hits, key = { it.chat.id }) { hit ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectChat(hit.chat.id)
                                viewModel.setSearchOpen(false)
                            }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                    ) {
                        Text(
                            hit.chat.title,
                            color = colors.textHi,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                        Text(
                            hit.snippet,
                            color = colors.textMid,
                            fontSize = 14.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
