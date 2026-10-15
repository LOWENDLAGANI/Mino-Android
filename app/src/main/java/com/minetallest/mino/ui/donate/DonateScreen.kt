package com.minetallest.mino.ui.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.R
import com.minetallest.mino.ui.theme.LocalMinoColors

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn

/** Spec §10.13 — Donate: DuitNow QR, caption, presets 5/10/20/50 RM. */
@Composable
fun DonateScreen(onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    var preset by remember { mutableStateOf<Int?>(null) }

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
            Text("  Support Mino", color = colors.textHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            text = "Mino runs on the kindness of its users. If it has been useful to you, " +
                "consider buying the owner a coffee.",
            color = colors.textMid,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.bgRaised)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.mino_donate_qr),
                contentDescription = "DuitNow QR",
                modifier = Modifier.width(240.dp),
            )
            Text(
                text = "Scan with your banking app",
                color = colors.textMid,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = "QR not added yet",
                color = colors.textLow,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text(
            text = "AMOUNT (RM)",
            color = colors.textLow,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 50).forEach { amount ->
                val selected = preset == amount
                Text(
                    text = "RM $amount",
                    color = if (selected) colors.accent else colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) colors.accentSoft else colors.bgRaised)
                        .clickable { preset = amount }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        Text(
            text = "Bank details are shared by Minetallest alongside the QR. Thank you for keeping Mino alive.",
            color = colors.textLow,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}
}
