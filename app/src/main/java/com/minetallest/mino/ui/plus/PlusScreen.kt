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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn

/** Spec §11.6 — the Dew wallet opens /plus, with redeem and legacy plan grants below. */
@Composable
fun PlusScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.minetallest.mino.MinoApp).container
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var redeemState by remember { mutableStateOf<String?>(null) }

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
            Text(
                text = "  Mino Plus",
                color = colors.textHi,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Dew wallet panel (spec §11.6)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.bgRaised)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Text("  Dew wallet", color = colors.textHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (ui.firebaseConfigured && ui.dew != null) {
                val balance = ui.dew!!.balance
                Text(
                    text = " ${"%.3f".format(balance).trimEnd('0').trimEnd('.')} Dew",
                    color = if (balance < 10) colors.amber else colors.accent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                Text(
                    text = "Sign in with an account to keep a Dew balance.",
                    color = colors.textMid,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Text(
                text = "Chat and image uses dew." +
                    " input : 0.015 output : 0.06 per 1k." +
                    " Image generation eats up 1 dew",
                color = colors.textMid,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Top up to use more ",
                color = colors.textLow,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Redeem code box (spec §11.5)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.bgRaised)
                .padding(18.dp),
        ) {
            Text("Redeem", color = colors.textHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            BasicTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                singleLine = true,
                textStyle = TextStyle(color = colors.textHi, fontSize = 15.sp, letterSpacing = 1.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    scope.launch {
                        redeemState = when (val result = container.redeem.redeem(code)) {
                            is com.minetallest.mino.net.RedeemSource.Result.Ok ->
                                if (result.dew != null) "Your Dew balance has been topped up."
                                else "Plan activated: ${result.plan ?: "grant"}."
                            is com.minetallest.mino.net.RedeemSource.Result.Error -> result.reason
                        }
                        viewModel.refreshServerState()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bgElevated)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (code.isEmpty()) Text("TEST", color = colors.textLow, fontSize = 13.sp, letterSpacing = 1.5.sp)
                    inner()
                },
            )
            Text(
                text = "Redeem",
                color = colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.accentSoft)
                    .clickable {
                        scope.launch {
                            redeemState = when (val result = container.redeem.redeem(code)) {
                                is com.minetallest.mino.net.RedeemSource.Result.Ok ->
                                    if (result.dew != null) "Your Dew balance has been topped up."
                                    else "Plan activated: ${result.plan ?: "grant"}."
                                is com.minetallest.mino.net.RedeemSource.Result.Error -> result.reason
                            }
                            viewModel.refreshServerState()
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            )
            redeemState?.let {
                Text(
                    text = it,
                    color = if (it.startsWith("Your") || it.startsWith("Plan")) colors.accent else colors.amber,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // Legacy plan grants (spec §11.7)
        ui.plan?.let { plan ->
            if (plan.planId != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.bgRaised)
                        .padding(18.dp),
                ) {
                    Text("Your plan", color = colors.textHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${plan.planId}${if (plan.paused) " · on hold" else ""}",
                        color = colors.accent,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (plan.expiresAt > 0) {
                        Text(
                            text = "Active until ${com.minetallest.mino.domain.Rules.monthDay(plan.expiresAt)}",
                            color = colors.textMid,
                            fontSize = 12.sp,
                        )
                    }
                    if (plan.paused) {
                        Text(
                            text = com.minetallest.mino.domain.Refusals.PAUSED_PLAN,
                            color = colors.amber,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Created by Minetallest",
            color = colors.textLow,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
}
