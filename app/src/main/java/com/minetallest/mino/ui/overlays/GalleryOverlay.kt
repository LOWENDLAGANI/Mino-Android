package com.minetallest.mino.ui.overlays

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import androidx.compose.foundation.layout.size

/** Spec §10.5 — Gallery: every image made or sent on this device, newest first. */
@Composable
fun GalleryOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val items by produceState<List<ChatViewModel.GalleryItem>>(initialValue = emptyList()) {
        value = viewModel.galleryItems()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.65f))
            .clickable { viewModel.setGalleryOpen(false) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.menuBackground)
                .clickable { }
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gallery", color = colors.textHi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = colors.textMid,
                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).clickable { viewModel.setGalleryOpen(false) },
                )
            }
            if (items.isEmpty()) {
                Text(
                    "No images yet on this device.",
                    color = colors.textLow,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 420.dp).padding(top = 10.dp),
                ) {
                    items(items, key = { it.messageId + it.url.hashCode() }) { item ->
                        val bitmap = remember {
                            try {
                                BitmapFactory.decodeByteArray(
                                    Base64.decode(item.url.substringAfter("base64,"), Base64.DEFAULT),
                                    0,
                                    Base64.decode(item.url.substringAfter("base64,"), Base64.DEFAULT).size,
                                )
                            } catch (e: Throwable) {
                                null
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectChat(item.chatId)
                                    viewModel.setGalleryOpen(false)
                                },
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = item.prompt,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
