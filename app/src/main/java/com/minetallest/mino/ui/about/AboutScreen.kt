package com.minetallest.mino.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.R
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn

/**
 * Spec §10.13 + §12 — About page; the logo carries the ten-tap admin trigger.
 * A non-admin is refused by the rules, never by the client.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, onAdmin: () -> Unit) {
    val colors = LocalMinoColors.current
    val context = LocalContext.current
    val container = remember { (context.applicationContext as com.minetallest.mino.MinoApp).container }
    val scope = rememberCoroutineScope()
    var taps by remember { mutableLongStateOf(0L) }
    var lastTap by remember { mutableLongStateOf(0L) }
    var adminState by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onBack() }.padding(6.dp),
            )
            Text("  About", color = colors.textHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val interaction = remember { MutableInteractionSource() }
            Image(
                painter = painterResource(R.drawable.mino_logo),
                contentDescription = "Mino logo",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(interactionSource = interaction, indication = null) {
                        val now = System.currentTimeMillis()
                        taps = if (now - lastTap < 2200) taps + 1 else 1
                        lastTap = now
                        if (taps >= 10) {
                            taps = 0
                            adminState = "Checking identity…"
                            scope.launch {
                                val signedIn = container.identity.ensureSignedIn()
                                val plan = container.api.plan()
                                if (signedIn && plan?.isAdmin == true) {
                                    adminState = null
                                    onAdmin()
                                } else {
                                    adminState = "Refused. The rules decide who the admin is — " +
                                        "sign in with the admin account on this device first."
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
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }

        AboutField("Created by", "Minetallest")
        AboutField("AI name", "Mino")
        AboutField("Privacy", "Local Storage On Your Device")
        AboutField("Date created", "24/9/2026")
        AboutField("Target", "Gonna release it for public use")

        Text(
            text = "Mino is a Ai Assistant that is created by Minetallest that have multiple ai model and fallback for your convenience.",
            color = colors.textMid,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}
}

@Composable
private fun AboutField(label: String, value: String) {
    val colors = LocalMinoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        Text(label.uppercase(), color = colors.textLow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Text(value, color = colors.text, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp))
    }
}
