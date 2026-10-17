package com.minetallest.mino.ui.overlays

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosSheet
import com.minetallest.mino.ui.theme.IosSheetHandle
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Gallery: every image made or sent on this device, newest first. */
@Composable
fun GalleryOverlay(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val items by produceState<List<ChatViewModel.GalleryItem>>(initialValue = emptyList()) {
        value = viewModel.galleryItems()
    }

    IosSheet(onDismiss = { viewModel.setGalleryOpen(false) }) {
        IosSheetHandle()
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Gallery",
                color = colors.textHi,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Done",
                color = colors.accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.setGalleryOpen(false) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        if (items.isEmpty()) {
            Text(
                "No images",
                color = colors.textMid,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 440.dp),
            ) {
                items(items, key = { it.messageId + it.url.hashCode() }) { item ->
                    val bitmap = remember {
                        try {
                            val bytes = Base64.decode(item.url.substringAfter("base64,"), Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Throwable) {
                            null
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                        } else {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(colors.fill))
                        }
                    }
                }
            }
        }
    }
}
