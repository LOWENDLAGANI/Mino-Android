package com.minetallest.mino.ui.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch

/** Dew wallet with redeem codes and the legacy plan card. */
@Composable
fun PlusScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.minetallest.mino.MinoApp).container
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var redeemState by remember { mutableStateOf<String?>(null) }

    val redeem: () -> Unit = {
        if (code.isNotBlank()) {
            scope.launch {
                redeemState = when (val result = container.redeem.redeem(code)) {
                    is com.minetallest.mino.net.RedeemSource.Result.Ok ->
                        if (result.dew != null) "Topped up"
                        else "Activated ${result.plan ?: ""}"
                    is com.minetallest.mino.net.RedeemSource.Result.Error -> result.reason
                }
                viewModel.refreshServerState()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().widthIn(max = 680.dp)) {
            IosNavBar(title = "Mino Plus", onBack = onBack)
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
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WaterDrop, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Text(
                            "Dew",
                            color = colors.textMid,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    val balance = ui.dew?.balance
                    Text(
                        text = if (balance != null) {
                            "%.2f".format(balance).trimEnd('0').trimEnd('.')
                        } else {
                            "—"
                        },
                        color = if (balance != null && balance < 10) colors.amber else colors.textHi,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            IosSectionHeader("Redeem")
            IosGroup {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp, letterSpacing = 1.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { redeem() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (code.isEmpty()) Text("Code", color = colors.textMid, fontSize = 17.sp, letterSpacing = 1.sp)
                            inner()
                        },
                    )
                    Text(
                        text = "Redeem",
                        color = if (code.isBlank()) colors.textMid else colors.onAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (code.isBlank()) colors.fill else colors.accent)
                            .clickable(enabled = code.isNotBlank()) { redeem() }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
                redeemState?.let {
                    Text(
                        text = it,
                        color = if (it.startsWith("Topped") || it.startsWith("Activated")) colors.mint else colors.amber,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            ui.plan?.let { plan ->
                if (plan.planId != null) {
                    IosSectionHeader("Plan")
                    IosGroup {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = "${plan.planId}${if (plan.paused) " · Paused" else ""}",
                                color = colors.textHi,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            if (plan.expiresAt > 0) {
                                Text(
                                    text = "Until ${com.minetallest.mino.domain.Rules.monthDay(plan.expiresAt)}",
                                    color = colors.textMid,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            if (plan.paused) {
                                Text(
                                    text = com.minetallest.mino.domain.Refusals.PAUSED_PLAN,
                                    color = colors.amber,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
