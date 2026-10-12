package com.minetallest.mino.ui.overlays

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.domain.Memory
import com.minetallest.mino.domain.ReasoningEffort
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.domain.ResponseLength
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.domain.SearchMode
import com.minetallest.mino.ui.chat.ChatViewModel
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size

/** Spec §6.9 — Settings: all sections in one scrollable sheet. */
@Composable
fun SettingsPanel(viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(colors.bgRaised)
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 640.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = colors.textHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = colors.textMid,
                    modifier = Modifier.size(18.dp).clip(RoundedCornerShape(8.dp)).clickable { viewModel.setSettingsOpen(false) },
                )
            }

            // 1. Account (spec §6.9 #1) — hidden entirely when Firebase is unconfigured.
            if (ui.firebaseConfigured) {
                Section("Account") {
                    Text(
                        text = "Signed in as a private Mino identity. Your chats follow your account.",
                        color = colors.textMid,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                    ui.plan?.let { plan ->
                        Text(
                            text = "Plan: ${plan.planId ?: "Free"}",
                            color = colors.textLow,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // 2. Your data (spec §6.9 #2)
            Section("Your data") {
                Row {
                    SettingButton("Export backup") { exportLauncher.launch(com.minetallest.mino.domain.Backup.exportFileName(System.currentTimeMillis())) }
                    Spacer(Modifier.width(10.dp))
                    EraseButton(onErase = { viewModel.wipeEverything(); viewModel.setSettingsOpen(false) })
                }
                Text(
                    text = "Erasing clears everything on this device. Your plan is kept.",
                    color = colors.textLow,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // 3. Memories (spec §6.9 #3)
            Section("What Mino remembers") {
                if (ui.memories.isEmpty()) {
                    Text("Nothing yet. Use “Remember this” in the + menu.", color = colors.textLow, fontSize = 12.sp)
                }
                ui.memories.forEach { memory ->
                    MemoryRow(memory = memory, viewModel = viewModel)
                }
                if (ui.memories.isNotEmpty()) {
                    Text(
                        text = "Forget everything",
                        color = colors.danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.forgetEverything() }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                ToggleRow(
                    label = "Let Mino suggest memories",
                    checked = ui.memorySuggestEnabled,
                    onChange = { viewModel.setMemorySuggest(it) },
                )
            }

            // 4. Web search (spec §6.9 #4)
            Section("Web search") {
                Segmented(
                    options = listOf(
                        SearchMode.AUTO to "Auto",
                        SearchMode.ALWAYS to "On",
                        SearchMode.OFF to "Off",
                    ),
                    selected = ui.searchMode,
                    onSelect = { viewModel.setSearchMode(it) },
                )
                Text("Smart search when needed", color = colors.textLow, fontSize = 10.sp)
                if (ui.availability?.searchAvailable == false) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(Modifier.width(6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.amber))
                        Text(
                            "  Search is not configured on this server",
                            color = colors.amber,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // 5. Response length (spec §6.9 #5)
            Section("Response length") {
                RadioRow("Short", "Lead with the answer", ui.responseLength == ResponseLength.SHORT) { viewModel.setResponseLength(ResponseLength.SHORT) }
                RadioRow("Balanced", "Default amount of detail", ui.responseLength == ResponseLength.BALANCED) { viewModel.setResponseLength(ResponseLength.BALANCED) }
                RadioRow("Detailed", "Thorough with examples", ui.responseLength == ResponseLength.DETAILED) { viewModel.setResponseLength(ResponseLength.DETAILED) }
            }

            // 6. Reasoning effort (spec §11.4 — priced, not gated)
            Section("Reasoning effort") {
                EffortRow("Low", "Fastest, cheapest · fast tier", ui.reasoningEffort == ReasoningEffort.LOW) { viewModel.setReasoningEffort(ReasoningEffort.LOW) }
                EffortRow("Medium", "Thinks a little harder · pro tier", ui.reasoningEffort == ReasoningEffort.MEDIUM) { viewModel.setReasoningEffort(ReasoningEffort.MEDIUM) }
                EffortRow("High", "Slowest, most careful · pro tier", ui.reasoningEffort == ReasoningEffort.HIGH) { viewModel.setReasoningEffort(ReasoningEffort.HIGH) }
            }

            // 7. Appearance (spec §6.9 #7)
            Section("Appearance") {
                Row {
                    AppearanceCard(
                        "dark", "Dark", ui.appearance == "dark", Modifier.weight(1f),
                    ) { viewModel.setAppearance("dark") }
                    Spacer(Modifier.width(10.dp))
                    AppearanceCard(
                        "light", "Light", ui.appearance == "light", Modifier.weight(1f),
                    ) { viewModel.setAppearance("light") }
                }
            }

            // Android addition — server address (web build used its own origin).
            Section("Server") {
                var base by remember(ui.apiBase) { mutableStateOf(ui.apiBase) }
                BasicTextField(
                    value = base,
                    onValueChange = { base = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textHi, fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgElevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (base.isEmpty()) {
                            Text("https://your-mino-server.example", color = colors.textLow, fontSize = 13.sp)
                        }
                        inner()
                    },
                )
                SettingButton("Save address") { viewModel.setApiBase(base) }
                Text(
                    text = "Point this app at your Mino deployment. Leave empty for local-only use.",
                    color = colors.textLow,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Mino · Created by Minetallest",
                color = colors.textLow,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMinoColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(
            text = title.uppercase(),
            color = colors.textLow,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingButton(label: String, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Text(
        text = label,
        color = colors.textHi,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgElevated)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun EraseButton(onErase: () -> Unit) {
    val colors = LocalMinoColors.current
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            kotlinx.coroutines.delay(4000)
            armed = false
        }
    }
    Text(
        text = if (armed) "Confirm erase?" else "Erase everything",
        color = colors.danger,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dangerSoft)
            .clickable {
                if (armed) {
                    onErase()
                    armed = false
                } else {
                    armed = true
                }
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun MemoryRow(memory: Memory, viewModel: ChatViewModel) {
    val colors = LocalMinoColors.current
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(memory.text) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= Refusals.MEMORY_TEXT_LIMIT) text = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.textHi, fontSize = 12.sp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.bgElevated)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
            Text(
                "Save",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp).clip(CircleShapeShape()).clickable {
                    viewModel.remember(text)
                    editing = false
                }.padding(6.dp),
            )
        } else {
            Text(
                text = memory.text,
                color = colors.text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { editing = true }.padding(vertical = 4.dp),
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Forget",
                tint = colors.textLow,
                modifier = Modifier.padding(start = 8.dp).size(14.dp).clip(RoundedCornerShape(6.dp)).clickable {
                    viewModel.forgetMemory(memory.id)
                }.padding(2.dp),
            )
        }
    }
}

private fun CircleShapeShape() = androidx.compose.foundation.shape.CircleShape

@Composable
private fun Segmented(
    options: List<Pair<SearchMode, String>>,
    selected: SearchMode,
    onSelect: (SearchMode) -> Unit,
) {
    val colors = LocalMinoColors.current
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEach { (mode, label) ->
            val active = mode == selected
            Text(
                text = label,
                color = if (active) colors.accent else colors.textMid,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.accentSoft else colors.bgElevated)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 9.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RadioRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.bgHover else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textHi, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = colors.textLow, fontSize = 10.sp)
        }
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = colors.accent, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun EffortRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    RadioRow(title, subtitle, selected, onClick)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgElevated)
            .clickable { onChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) colors.accent else colors.bgHover),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(14.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (checked) colors.onAccent else colors.textLow),
            )
        }
    }
}

@Composable
private fun AppearanceCard(
    value: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalMinoColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.accentSoft else colors.bgElevated)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (value == "dark") Color(0xFF0B1310) else Color(0xFFFBF9F2)),
        )
        Text(
            text = label,
            color = if (selected) colors.accent else colors.textMid,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
