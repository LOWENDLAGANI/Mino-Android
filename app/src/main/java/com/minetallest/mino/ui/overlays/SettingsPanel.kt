package com.minetallest.mino.ui.overlays

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.BuildConfig
import com.minetallest.mino.domain.ReasoningEffort
import com.minetallest.mino.domain.ResponseLength
import com.minetallest.mino.domain.SearchMode
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.IosDivider
import com.minetallest.mino.ui.theme.IosGroup
import com.minetallest.mino.ui.theme.IosNavBar
import com.minetallest.mino.ui.theme.IosRow
import com.minetallest.mino.ui.theme.IosSectionHeader
import com.minetallest.mino.ui.theme.IosSwitchRow
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch

/** iOS-style settings: profile, preferences, data and server. */
@Composable
fun SettingsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showLengthDialog by remember { mutableStateOf(false) }
    var showReasoningDialog by remember { mutableStateOf(false) }
    var showEraseConfirmDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportBackup()
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    viewModel.notice("Backup saved", ChatViewModel.Notice.Tone.SUCCESS)
                } catch (e: Throwable) {
                    viewModel.notice("Export failed", ChatViewModel.Notice.Tone.RED)
                }
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
            IosNavBar(title = "Settings", onBack = onBack)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            IosGroup {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (ui.displayName ?: "M").take(1).uppercase(),
                            color = colors.onAccent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(
                            text = ui.displayName ?: "You",
                            color = colors.textHi,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = ui.plan?.planId ?: "Free",
                            color = colors.textMid,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            IosSectionHeader("Preferences")
            IosGroup {
                IosSwitchRow(
                    title = "Suggest Memories",
                    icon = Icons.Filled.Psychology,
                    iconTint = colors.accent,
                    checked = ui.memorySuggestEnabled,
                    onCheckedChange = { viewModel.setMemorySuggest(it) },
                )
                IosDivider(startIndent = 57.dp)
                IosRow(
                    title = "Web Search",
                    icon = Icons.Filled.Language,
                    iconTint = colors.mint,
                    value = when (ui.searchMode) {
                        SearchMode.AUTO -> "Auto"
                        SearchMode.ALWAYS -> "On"
                        SearchMode.OFF -> "Off"
                    },
                    chevron = true,
                    onClick = { showSearchDialog = true },
                )
                IosDivider(startIndent = 57.dp)
                IosRow(
                    title = "Response Length",
                    icon = Icons.Filled.Tune,
                    iconTint = colors.amber,
                    value = when (ui.responseLength) {
                        ResponseLength.SHORT -> "Short"
                        ResponseLength.BALANCED -> "Balanced"
                        ResponseLength.DETAILED -> "Detailed"
                    },
                    chevron = true,
                    onClick = { showLengthDialog = true },
                )
                IosDivider(startIndent = 57.dp)
                IosRow(
                    title = "Reasoning",
                    icon = Icons.Filled.Speed,
                    iconTint = Color(0xFF5856D6),
                    value = when (ui.reasoningEffort) {
                        ReasoningEffort.LOW -> "Low"
                        ReasoningEffort.MEDIUM -> "Medium"
                        ReasoningEffort.HIGH -> "High"
                    },
                    chevron = true,
                    onClick = { showReasoningDialog = true },
                )
            }

            IosSectionHeader("Data")
            IosGroup {
                IosRow(
                    title = "Export Backup",
                    icon = Icons.Filled.Download,
                    iconTint = colors.accent,
                    onClick = {
                        exportLauncher.launch(com.minetallest.mino.domain.Backup.exportFileName(System.currentTimeMillis()))
                    },
                )
                IosDivider(startIndent = 57.dp)
                IosRow(
                    title = "Erase Everything",
                    icon = Icons.Filled.Delete,
                    iconTint = colors.danger,
                    destructive = true,
                    onClick = { showEraseConfirmDialog = true },
                )
            }

            IosSectionHeader("Server")
            IosGroup {
                var base by remember(ui.apiBase) { mutableStateOf(ui.apiBase) }
                Column(modifier = Modifier.padding(16.dp)) {
                    BasicTextField(
                        value = base,
                        onValueChange = { base = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.setApiBase(base) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.fill)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        decorationBox = { inner ->
                            if (base.isEmpty()) {
                                Text("Server address", color = colors.textMid, fontSize = 17.sp)
                            }
                            inner()
                        },
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                        Text(
                            text = "Save",
                            color = colors.accent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setApiBase(base) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Text(
                text = "Mino ${BuildConfig.VERSION_NAME}",
                color = colors.textMid,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (showSearchDialog) {
        OptionSelectionDialog(
            title = "Web Search",
            options = listOf(
                SearchMode.AUTO to "Auto",
                SearchMode.ALWAYS to "On",
                SearchMode.OFF to "Off",
            ),
            selected = ui.searchMode,
            onSelect = {
                viewModel.setSearchMode(it)
                showSearchDialog = false
            },
            onDismiss = { showSearchDialog = false },
        )
    }

    if (showLengthDialog) {
        OptionSelectionDialog(
            title = "Response Length",
            options = listOf(
                ResponseLength.SHORT to "Short",
                ResponseLength.BALANCED to "Balanced",
                ResponseLength.DETAILED to "Detailed",
            ),
            selected = ui.responseLength,
            onSelect = {
                viewModel.setResponseLength(it)
                showLengthDialog = false
            },
            onDismiss = { showLengthDialog = false },
        )
    }

    if (showReasoningDialog) {
        OptionSelectionDialog(
            title = "Reasoning",
            options = listOf(
                ReasoningEffort.LOW to "Low",
                ReasoningEffort.MEDIUM to "Medium",
                ReasoningEffort.HIGH to "High",
            ),
            selected = ui.reasoningEffort,
            onSelect = {
                viewModel.setReasoningEffort(it)
                showReasoningDialog = false
            },
            onDismiss = { showReasoningDialog = false },
        )
    }

    if (showEraseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEraseConfirmDialog = false },
            title = {
                Text(
                    "Erase All Data?",
                    color = colors.textHi,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeEverything()
                    showEraseConfirmDialog = false
                    onBack()
                }) {
                    Text("Erase", color = colors.danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseConfirmDialog = false }) {
                    Text("Cancel", color = colors.accent)
                }
            },
            containerColor = colors.bgRaised,
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMinoColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                color = colors.textHi,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                options.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            color = colors.text,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (option == selected) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.accent)
            }
        },
        containerColor = colors.bgRaised,
        shape = RoundedCornerShape(14.dp),
    )
}
