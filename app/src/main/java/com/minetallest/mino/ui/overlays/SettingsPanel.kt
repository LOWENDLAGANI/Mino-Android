package com.minetallest.mino.ui.overlays

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.ReasoningEffort
import com.minetallest.mino.domain.ResponseLength
import com.minetallest.mino.domain.SearchMode
import com.minetallest.mino.ui.chat.ChatViewModel
import kotlinx.coroutines.launch

// Strict Claude Style Monochrome Dark Palette
private val DarkBg = Color(0xFF121212)
private val CardSurface = Color(0xFF1E1E22)
private val DividerColor = Color(0xFF27272A)
private val TextPrimary = Color(0xFFF4F4F5)
private val TextSecondary = Color(0xFFA1A1AA)
private val DestructiveRed = Color(0xFFEF5350)

/**
 * Dedicated Settings Screen (Claude Mobile style).
 * Full page layout with back navigation, zero subtext descriptions, and no bottom cut-off.
 */
@Composable
fun SettingsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Top App Bar Header with Back Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Account Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ui.displayName ?: "Private Mino Identity",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TextSecondary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = ui.plan?.planId ?: "Free",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // 2. Preferences Group Card
                SettingsGroup(title = "PREFERENCES") {
                    SettingsSwitchRow(
                        icon = Icons.Filled.Psychology,
                        title = "Suggest Memories",
                        checked = ui.memorySuggestEnabled,
                        onCheckedChange = { viewModel.setMemorySuggest(it) },
                    )

                    HorizontalDivider(color = DividerColor)

                    SettingsValueRow(
                        icon = Icons.Filled.Language,
                        title = "Web Search",
                        value = when (ui.searchMode) {
                            SearchMode.AUTO -> "Auto"
                            SearchMode.ALWAYS -> "On"
                            SearchMode.OFF -> "Off"
                        },
                        onClick = { showSearchDialog = true },
                    )

                    HorizontalDivider(color = DividerColor)

                    SettingsValueRow(
                        icon = Icons.Filled.Tune,
                        title = "Response Length",
                        value = when (ui.responseLength) {
                            ResponseLength.SHORT -> "Short"
                            ResponseLength.BALANCED -> "Balanced"
                            ResponseLength.DETAILED -> "Detailed"
                        },
                        onClick = { showLengthDialog = true },
                    )

                    HorizontalDivider(color = DividerColor)

                    SettingsValueRow(
                        icon = Icons.Filled.Speed,
                        title = "Reasoning Effort",
                        value = when (ui.reasoningEffort) {
                            ReasoningEffort.LOW -> "Low"
                            ReasoningEffort.MEDIUM -> "Medium"
                            ReasoningEffort.HIGH -> "High"
                        },
                        onClick = { showReasoningDialog = true },
                    )
                }

                // 3. Data & Privacy Group Card
                SettingsGroup(title = "YOUR DATA") {
                    SettingsActionRow(
                        icon = Icons.Filled.Download,
                        title = "Export Backup",
                        onClick = {
                            exportLauncher.launch(com.minetallest.mino.domain.Backup.exportFileName(System.currentTimeMillis()))
                        },
                    )

                    HorizontalDivider(color = DividerColor)

                    SettingsActionRow(
                        icon = Icons.Filled.Delete,
                        title = "Erase Everything",
                        tint = DestructiveRed,
                        onClick = { showEraseConfirmDialog = true },
                    )
                }

                // 4. Server Group Card
                SettingsGroup(title = "SERVER") {
                    var base by remember(ui.apiBase) { mutableStateOf(ui.apiBase) }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Server Address", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = base,
                            onValueChange = { base = it },
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            decorationBox = { inner ->
                                if (base.isEmpty()) {
                                    Text("https://your-mino-server.example", color = TextSecondary, fontSize = 13.sp)
                                }
                                inner()
                            },
                        )
                        TextButton(
                            onClick = { viewModel.setApiBase(base) },
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                        ) {
                            Text("Save Address", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "Mino AI · Local-First",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp),
                )
            }
        }
    }

    // Selection Dialogs
    if (showSearchDialog) {
        OptionSelectionDialog(
            title = "Web Search Mode",
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
            title = "Reasoning Effort",
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
            title = { Text("Erase All Data?", color = TextPrimary) },
            text = { Text("Permanently delete all local chat history on this device.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeEverything()
                    showEraseConfirmDialog = false
                    onBack()
                }) {
                    Text("Erase", color = DestructiveRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface,
        )
    }
}

/** Grouped Section Card Helper */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CardSurface,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/** Switch Row Helper Component */
@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TextSecondary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkBg,
            ),
        )
    }
}

/** Selected Value Row Helper Component */
@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 4.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Action Row Helper Component */
@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (tint == DestructiveRed) DestructiveRed else TextSecondary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            color = tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Option Dialog Selection Helper */
@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (option == selected),
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = TextPrimary, unselectedColor = TextSecondary),
                        )
                        Text(
                            text = label,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardSurface,
    )
}
