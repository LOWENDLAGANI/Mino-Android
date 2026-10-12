package com.minetallest.mino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.minetallest.mino.ui.MinoRoot
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.MinoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = minoContainer
        setContent {
            val viewModel: ChatViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ChatViewModel(container) }
                },
            )
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            MinoTheme(dark = ui.appearance != "light") {
                MinoRoot(viewModel)
            }
        }
    }
}
