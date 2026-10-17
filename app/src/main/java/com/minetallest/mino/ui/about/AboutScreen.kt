package com.minetallest.mino.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.BuildConfig
import com.minetallest.mino.R
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.IosRow
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch

/** About page; the logo carries the ten-tap admin trigger. */
@Composable
fun AboutScreen(onBack: () -> Unit, onAdmin: () -> Unit) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val container = remember { (context.applicationContext as com.minetallest.mino.MinoApp).container }
    val scope = rememberCoroutineScope()
    var taps by remember { mutableLongStateOf(0L) }
    var lastTap by remember { mutableLongStateOf(0L) }
    var adminState by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().widthIn(max = 680.dp)) {
            IosNavBar(title = "About", onBack = onBack)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                val interaction = remember { MutableInteractionSource() }
                Image(
                    painter = painterResource(R.drawable.mino_logo),
                    contentDescription = "Mino",
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(interactionSource = interaction, indication = null) {
                            val now = System.currentTimeMillis()
                            taps = if (now - lastTap < 2200) taps + 1 else 1
                            lastTap = now
                            if (taps >= 10) {
                                taps = 0
                                adminState = "Checking"
                                scope.launch {
                                    val signedIn = container.identity.ensureSignedIn()
                                    val plan = container.api.plan()
                                    if (signedIn && plan?.isAdmin == true) {
                                        adminState = null
                                        onAdmin()
                                    } else {
                                        adminState = "Not admin"
                                    }
                                }
                            }
                        },
                )
            }

            adminState?.let {
                Text(
                    text = it,
                    color = colors.amber,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }

            IosGroup {
                IosRow(title = "Created by", value = "Minetallest")
                IosDivider()
                IosRow(title = "Version", value = BuildConfig.VERSION_NAME)
            }

            Text(
                text = "Mino",
                color = colors.textMid,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
