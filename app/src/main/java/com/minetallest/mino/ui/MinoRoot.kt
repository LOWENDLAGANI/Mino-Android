package com.minetallest.mino.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.R
import com.minetallest.mino.ui.chat.ChatScreen
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.delay

/**
 * Startup: prefs → maintenance → name gate → chat, with a one-shot splash on top.
 * [deepLinkChatId] comes from a reply notification tap.
 * [incomingShareIntent] handles files/text shared from other apps.
 */
@Composable
fun MinoRoot(
    viewModel: ChatViewModel,
    deepLinkChatId: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    incomingShareIntent: android.content.Intent? = null,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val colors = LocalMinoColors.current
    val view = LocalView.current
    val context = LocalContext.current

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deepLinkChatId) {
        val chatId = deepLinkChatId ?: return@LaunchedEffect
        viewModel.selectChat(chatId)
        onDeepLinkConsumed()
    }

    // Handle incoming shared content from other apps
    LaunchedEffect(incomingShareIntent) {
        incomingShareIntent?.let { intent ->
            try {
                val sharedContent = com.minetallest.mino.util.SharedContentHandler.processShareIntent(context, intent)
                viewModel.handleIncomingShare(sharedContent)
            } catch (e: Exception) {
                // Silently fail - share processing is best-effort
            }
        }
    }

    // Ask once, after onboarding, so background replies can reach the user.
    LaunchedEffect(ui.splashDone, ui.displayName) {
        if (askedForNotifications || !ui.splashDone || ui.displayName == null) return@LaunchedEffect
        askedForNotifications = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = colors.lightStatusBar
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = colors.lightStatusBar
    }

    androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        when {
            !ui.hydrated -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp)
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

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splash-alpha",
    )
    val blur by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(600),
        label = "splash-blur",
    )
    LaunchedEffect(Unit) {
        delay(900)
        visible = false
        delay(600)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color.Black),
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
        Image(
            painter = painterResource(R.drawable.mino_logo),
            contentDescription = "Mino",
            modifier = Modifier
                .size(72.dp)
                .padding(bottom = 20.dp),
        )
        Text(
            text = message,
            color = colors.text,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}
