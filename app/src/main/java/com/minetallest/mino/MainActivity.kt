package com.minetallest.mino

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.minetallest.mino.ui.MinoRoot
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.MinoTheme

class MainActivity : ComponentActivity() {

    private var pendingChatId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The extra is removed once consumed, so re-reading it after a
        // configuration change is safe and keeps process-death restores working.
        pendingChatId = intent?.getStringExtra(EXTRA_CHAT_ID)

        val container = minoContainer
        setContent {
            val viewModel: ChatViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ChatViewModel(container) }
                },
            )
            MinoTheme(
                viewModel = viewModel,
                deepLinkChatId = pendingChatId,
                onDeepLinkConsumed = {
                    pendingChatId = null
                    intent?.removeExtra(EXTRA_CHAT_ID)
                },
                incomingShareIntent = intent.takeIf { intent?.action in listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingChatId = intent.getStringExtra(EXTRA_CHAT_ID)
    }

    companion object {
        /** Set by [com.minetallest.mino.notify.ReplyNotifier] when a reply is ready. */
        const val EXTRA_CHAT_ID = "mino.chat_id"
    }
}
