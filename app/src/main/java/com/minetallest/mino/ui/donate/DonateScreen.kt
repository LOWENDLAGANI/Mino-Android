package com.minetallest.mino.ui.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.R
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.LocalMinoColors

/** Support page: DuitNow QR and amount presets. */
@Composable
fun DonateScreen(onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    var preset by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().widthIn(max = 680.dp)) {
            IosNavBar(title = "Support", onBack = onBack)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            IosGroup {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.mino_donate_qr),
                        contentDescription = "DuitNow QR",
                        modifier = Modifier
                            .width(240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
            }

            IosSectionHeader("Amount (RM)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20, 50).forEach { amount ->
                    val selected = preset == amount
                    Text(
                        text = "RM $amount",
                        color = if (selected) colors.onAccent else colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) colors.accent else colors.bgRaised)
                            .clickable { preset = amount }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
