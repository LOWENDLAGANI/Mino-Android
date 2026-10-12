package com.minetallest.mino.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.R
import com.minetallest.mino.ui.chat.ChatScreen
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.delay

/**
 * Startup sequence, exact order (spec §6.1):
 * 1. nothing until prefs hydrate · 2. maintenance screen · 3. account loader (only while
 * resolving) · 4. name gate (no skip) · 5. main shell. Splash rides over everything,
 * once per app launch.
 */
@Composable
fun MinoRoot(viewModel: ChatViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val colors = LocalMinoColors.current
    val view = LocalView.current

    // Spec §5.4 — bars follow the appearance token.
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        window.statusBarColor = colors.systemBarColor.hashCode()
        window.navigationBarColor = colors.systemBarColor.hashCode()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = colors.lightStatusBar
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = colors.lightStatusBar
    }

    androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        when {
            !ui.hydrated -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
            ui.maintenanceMessage != null -> MaintenanceScreen(message = ui.maintenanceMessage!!)
            ui.displayName == null -> NamePromptScreen(viewModel)
            else -> ChatScreen(viewModel)
        }

        if (!ui.splashDone) {
            SplashScreen(onFinished = { viewModel.finishSplash() })
        }
    }
}

/** Spec §6.1 — full-bleed splash that blurs + dissolves, once per app launch. */
@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    val colors = LocalMinoColors.current
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "splash-alpha",
    )
    val blur by animateFloatAsState(
        targetValue = if (visible) 0f else 26f,
        animationSpec = tween(700),
        label = "splash-blur",
    )
    LaunchedEffect(Unit) {
        delay(900)
        visible = false
        delay(700)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.mino_splash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(blur.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MaintenanceScreen(message: String) {
    val colors = LocalMinoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(R.drawable.mino_logo), contentDescription = "Mino", modifier = Modifier.padding(bottom = 24.dp))
        Text(
            text = message,
            color = colors.text,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
        Text(
            text = "Created by Minetallest",
            color = colors.textLow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
