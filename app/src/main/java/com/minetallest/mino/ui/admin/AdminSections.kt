package com.minetallest.mino.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.mino.net.AdminApi
import com.minetallest.mino.net.AppConfig
import com.minetallest.mino.net.DevNote
import com.minetallest.mino.ui.chat.ChatViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minetallest.mino.ui.theme.LocalMinoColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- shared widgets

@Composable
internal fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMinoColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgRaised)
            .padding(14.dp),
    ) {
        Text(title, color = colors.textHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
internal fun RowLine(label: String, value: String) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = colors.textMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = colors.textHi,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
internal fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.accent,
                checkedThumbColor = colors.onAccent,
            ),
        )
    }
}

@Composable
internal fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    singleLine: Boolean = true,
) {
    val colors = LocalMinoColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.line,
            focusedTextColor = colors.textHi,
            unfocusedTextColor = colors.text,
            cursorColor = colors.accent,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.textMid,
            focusedContainerColor = colors.bgElevated,
            unfocusedContainerColor = colors.bgElevated,
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
internal fun ActionButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalMinoColors.current
    Text(
        text = label,
        color = if (enabled) colors.onAccent else colors.textLow,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(top = 10.dp, end = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) colors.accent else colors.bgElevated)
            .clickable(enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

// ---------------------------------------------------------------- 1. Diagnostics

@Composable
internal fun DiagnosticsSection(viewModel: ChatViewModel, config: AppConfig?) {
    val colors = LocalMinoColors.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    SectionCard("Diagnostics") {
        RowLine("Signed in", if (ui.firebaseConfigured) "yes" else "not configured (local-only)")
        RowLine("Server", if (ui.serverConfigured) "configured" else "not configured")
        RowLine("Chats on device", ui.chats.size.toString())
        RowLine("Messages on device", ui.messages.size.toString())
        RowLine("Memories", ui.memories.size.toString())
        RowLine("Trash entries", ui.trash.size.toString())
        RowLine("Scheduled", ui.scheduled.size.toString())
        RowLine(
            "Plan",
            when {
                ui.plan?.isAdmin == true -> "admin"
                ui.plan?.planId != null -> ui.plan?.planId.orEmpty()
                ui.plan?.checked == true -> "free"
                else -> "could not check"
            },
        )
        RowLine("Dew balance", ui.dew?.balance?.let { "$it" } ?: "—")
        RowLine("Maintenance", config?.maintenanceEnabled?.let { if (it) "ON" else "off" } ?: "—")
        RowLine("Chat / image caps", "${config?.dailyChatCap ?: 0} / ${config?.dailyImageCap ?: 0}")
        RowLine(
            "Modes available",
            ui.availability?.available?.joinToString(" · ") { it.wire } ?: "unknown",
        )
        RowLine("Announcement", config?.announcement?.ifBlank { "—" } ?: "—")
    }
}

// ---------------------------------------------------------------- 2. People

@Composable
internal fun PeopleSection(
    admin: AdminApi,
    scope: CoroutineScope,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    val colors = LocalMinoColors.current
    var people by remember { mutableStateOf<List<AdminApi.Person>?>(null) }
    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AdminApi.Person?>(null) }
    var planTier by remember { mutableStateOf("mini") }
    var planDays by remember { mutableStateOf("30") }
    var banReason by remember { mutableStateOf("") }
    var banDays by remember { mutableStateOf("7") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onBusy("Loading people…")
        val loaded = admin.people()
        people = loaded
        onDone(if (loaded.isEmpty()) "No registry entries yet." else null)
    }

    SectionCard("People · admin/registry") {
        Field(value = filter, onValueChange = { filter = it }, label = "Search by name")

        people?.forEach { person ->
            if (filter.isNotBlank() && !person.name.contains(filter, true)) return@forEach
            val tier = person.plan ?: (if (person.banned) "banned" else "free")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.bgElevated)
                    .clickable { selected = if (selected == person) null else person }
                    .padding(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(person.name, color = colors.textHi, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        tier,
                        color = if (person.banned) colors.danger else colors.mint,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    "uid ${person.uid.take(12)} · seen ${person.lastSeen}",
                    color = colors.textLow,
                    fontSize = 10.sp,
                )
            }

            if (selected == person) {
                Field(value = planTier, onValueChange = { planTier = it }, label = "Tier (mini/lunar/…)")
                Field(value = planDays, onValueChange = { planDays = it }, label = "Length in days", numeric = true)
                ActionButton("Grant plan") {
                    onBusy("Granting plan…")
                    scope.launch {
                        val days = planDays.toIntOrNull() ?: 30
                        val ok = admin.setPlan(
                            person.uid,
                            planTier,
                            System.currentTimeMillis() + days * 86_400_000L,
                            "",
                        )
                        onDone(if (ok) "Plan granted to ${person.name}." else "The rules refused that write.")
                    }
                }
                ActionButton("Remove plan") {
                    onBusy("Removing plan…")
                    scope.launch {
                        val ok = admin.setPlan(person.uid, null, 0, "")
                        onDone(if (ok) "Plan removed." else "The rules refused that write.")
                    }
                }
                ActionButton(if (person.banned) "Lift ban" else "Ban") {
                    onBusy(if (person.banned) "Lifting ban…" else "Banning…")
                    scope.launch {
                        val days = banDays.toIntOrNull() ?: 7
                        val ok = admin.setBan(
                            person.uid,
                            banReason.ifBlank { "No reason given." },
                            if (person.banned) 0 else System.currentTimeMillis() + days * 86_400_000L,
                        )
                        onDone(if (ok) "Ban updated for ${person.name}." else "The rules refused that write.")
                    }
                }
                if (!person.banned) {
                    Field(value = banReason, onValueChange = { banReason = it }, label = "Ban reason")
                    Field(value = banDays, onValueChange = { banDays = it }, label = "Ban length in days", numeric = true)
                }
                ActionButton(if (person.plan != null) "Pause subscription" else "Continue subscription") {
                    onBusy("Updating subscription…")
                    scope.launch {
                        val ok = admin.setPaused(person.uid, person.plan != null)
                        onDone(if (ok) "Subscription updated." else "The rules refused that write.")
                    }
                }
            }
        }
        if (people?.isEmpty() == true) {
            Text(
                "Nobody has registered on this device's server yet.",
                color = colors.textLow,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- 3. Runtime controls

@Composable
internal fun ControlsSection(
    config: AppConfig?,
    scope: CoroutineScope,
    admin: AdminApi,
    onSaved: (AppConfig) -> Unit,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    var chat by remember(config) { mutableStateOf(config?.chatEnabled ?: true) }
    var images by remember(config) { mutableStateOf(config?.imageEnabled ?: true) }
    var search by remember(config) { mutableStateOf(config?.searchEnabled ?: true) }
    var google by remember(config) { mutableStateOf(config?.googleToolsEnabled ?: true) }
    var maintenance by remember(config) { mutableStateOf(config?.maintenanceEnabled ?: false) }
    var maintenanceMessage by remember(config) { mutableStateOf(config?.maintenanceMessage ?: "") }
    var announcement by remember(config) { mutableStateOf(config?.announcement ?: "") }
    var chatCap by remember(config) { mutableStateOf((config?.dailyChatCap ?: 0).toString()) }
    var imageCap by remember(config) { mutableStateOf((config?.dailyImageCap ?: 0).toString()) }

    SectionCard("Runtime controls") {
        if (config == null) {
            Text(
                "Runtime config is unreadable from this device.",
                color = LocalMinoColors.current.amber,
                fontSize = 12.sp,
            )
            return@SectionCard
        }
        ToggleLine("Chat", chat) { chat = it }
        ToggleLine("Images", images) { images = it }
        ToggleLine("Web search", search) { search = it }
        ToggleLine("Google tools", google) { google = it }
        ToggleLine("Maintenance", maintenance) { maintenance = it }
        if (maintenance) {
            Field(maintenanceMessage, { maintenanceMessage = it }, "Maintenance reason")
        }
        Field(announcement, { announcement = it }, "Announcement banner")
        Field(chatCap, { chatCap = it }, "Daily chat cap (0 = none)", numeric = true)
        Field(imageCap, { imageCap = it }, "Daily image cap (0 = none)", numeric = true)
        ActionButton("Save controls") {
            onBusy("Saving runtime controls…")
            scope.launch {
                val next = config.copy(
                    chatEnabled = chat,
                    imageEnabled = images,
                    searchEnabled = search,
                    googleToolsEnabled = google,
                    maintenanceEnabled = maintenance,
                    maintenanceMessage = maintenanceMessage,
                    announcement = announcement.take(200),
                    dailyChatCap = chatCap.toIntOrNull() ?: 0,
                    dailyImageCap = imageCap.toIntOrNull() ?: 0,
                    updatedAt = System.currentTimeMillis(),
                )
                val ok = admin.writeConfig(next.toJson())
                if (ok) {
                    onSaved(next)
                    onDone("Runtime controls saved.")
                } else {
                    onDone("The server refused that write.")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 4. Banned

@Composable
internal fun BansSection(
    admin: AdminApi,
    scope: CoroutineScope,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    val colors = LocalMinoColors.current
    var bans by remember { mutableStateOf<List<AdminApi.Ban>?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onBusy("Loading bans…")
        bans = admin.bans()
        onDone(null)
    }

    SectionCard("Banned") {
        bans?.forEach { ban ->
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("uid ${ban.uid.take(12)}", color = colors.textHi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(ban.reason, color = colors.textMid, fontSize = 12.sp)
                Text(
                    "by ${ban.by.ifBlank { "?" }} · until ${ban.until}",
                    color = colors.textLow,
                    fontSize = 10.sp,
                )
                ActionButton("Lift") {
                    onBusy("Lifting ban…")
                    scope.launch {
                        val ok = admin.setBan(ban.uid, ban.reason, 0)
                        bans = admin.bans()
                        onDone(if (ok) "Ban lifted." else "The rules refused that write.")
                    }
                }
            }
        }
        if (bans?.isEmpty() == true) {
            Text("No active bans.", color = colors.textLow, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ---------------------------------------------------------------- 5. Redeem codes

@Composable
internal fun CodesSection(
    admin: AdminApi,
    scope: CoroutineScope,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    val colors = LocalMinoColors.current
    var codes by remember { mutableStateOf<List<AdminApi.Code>?>(null) }
    var word by remember { mutableStateOf("") }
    var dew by remember { mutableStateOf("100") }
    var plan by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onBusy("Loading codes…")
        codes = admin.codes()
        onDone(null)
    }

    SectionCard("Redeem codes") {
        Text(
            "New sales are Dew top-ups (presets 25 / 100 / 500 / 1000); the legacy plan shape stays available.",
            color = colors.textLow,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("25", "100", "500", "1000").forEach { preset ->
                Text(
                    preset,
                    color = colors.mint,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgElevated)
                        .clickable { dew = preset }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        Field(word, { word = it }, "Word (≥ 4 characters, upper-cased)")
        Field(dew, { dew = it }, "Dew amount", numeric = true)
        Field(plan, { plan = it }, "Legacy plan (optional)")
        Field(days, { days = it }, "Days (optional, legacy)", numeric = true)
        ActionButton("Create code") {
            onBusy("Creating code…")
            scope.launch {
                val ok = admin.createCode(
                    word,
                    dew.toDoubleOrNull(),
                    plan.trim().ifBlank { null },
                    days.toIntOrNull() ?: 0,
                )
                if (ok) {
                    codes = admin.codes()
                    word = ""
                    onDone("Code created.")
                } else {
                    onDone("Codes are at least 4 characters.")
                }
            }
        }

        codes?.forEach { code ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(code.code, color = colors.textHi, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            append(if (code.dew != null) "=${code.dew}" else code.plan ?: "")
                            if (code.days > 0) append(" · ${code.days}d")
                            append(if (code.active) " · active" else " · off")
                        },
                        color = if (code.active) colors.mint else colors.textLow,
                        fontSize = 11.sp,
                    )
                }
                if (code.active) {
                    Text(
                        "Terminate",
                        color = colors.danger,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.dangerSoft)
                            .clickable {
                                scope.launch {
                                    onBusy("Switching off ${code.code}…")
                                    val ok = admin.deactivateCode(code.code)
                                    codes = admin.codes()
                                    onDone(if (ok) "Code switched off." else "The rules refused that write.")
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
        if (codes?.isEmpty() == true) {
            Text("No codes yet.", color = colors.textLow, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ---------------------------------------------------------------- 6. Notes

@Composable
internal fun NotesSection(
    config: AppConfig?,
    scope: CoroutineScope,
    admin: AdminApi,
    onSaved: (AppConfig) -> Unit,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    val colors = LocalMinoColors.current
    var existing by remember(config) { mutableStateOf(config?.notes ?: emptyList()) }
    var badge by remember { mutableStateOf("New") }
    var tone by remember { mutableStateOf("neutral") }
    var byline by remember { mutableStateOf("Minetallest") }
    var date by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    SectionCard("Notes · dev news") {
        Field(badge, { badge = it }, "Badge text")
        Field(tone, { tone = it }, "Tone (neutral / mint / amber / red)")
        Field(byline, { byline = it }, "Byline")
        Field(date, { date = it }, "Date wording")
        Field(summary, { summary = it }, "Summary")
        Field(body, { body = it }, "Body", singleLine = false)

        ActionButton("Publish") {
            onBusy("Publishing…")
            scope.launch {
                val note = DevNote(
                    id = "note-" + System.currentTimeMillis(),
                    badge = badge,
                    tone = tone,
                    byline = byline,
                    date = date,
                    summary = summary,
                    body = body,
                )
                val nextNotes = listOf(note) + existing
                val base = config ?: AppConfig.DEFAULT
                val next = base.copy(notes = nextNotes, updatedAt = System.currentTimeMillis())
                val ok = admin.writeConfig(next.toJson())
                if (ok) {
                    existing = nextNotes
                    onSaved(next)
                    badge = ""; summary = ""; body = ""
                    onDone("Note published.")
                } else {
                    onDone("The server refused that write.")
                }
            }
        }

        existing.forEach { note ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.bgElevated)
                    .padding(10.dp),
            ) {
                Text(
                    "${note.badge} · ${note.date}",
                    color = colors.mint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (note.summary.isNotBlank()) Text(note.summary, color = colors.text, fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- 7. Model health

@Composable
internal fun ModelsSection(
    admin: AdminApi,
    scope: CoroutineScope,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    val colors = LocalMinoColors.current
    var models by remember { mutableStateOf<List<AdminApi.ModelTarget>?>(null) }
    var results by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onBusy("Loading models…")
        models = admin.models()
        onDone(null)
    }

    SectionCard("Model health") {
        Text(
            "Each Test sends a real single-model request — no fallback chain, so a healthy chain can't hide one broken model.",
            color = colors.textLow,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        models?.forEach { model ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(model.name, color = colors.textHi, fontSize = 13.sp)
                    Text(model.id, color = colors.textLow, fontSize = 10.sp)
                    results[model.id]?.let {
                        Text(it, color = colors.mint, fontSize = 11.sp)
                    }
                }
                Text(
                    "Test",
                    color = colors.onAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent)
                        .clickable {
                            scope.launch {
                                onBusy("Probing ${model.name}…")
                                val result = admin.probeModel(model.id)
                                results = results + (model.id to result)
                                onDone(null)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        if (models?.isEmpty() == true) {
            Text("No models reported by the server.", color = colors.textLow, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------- 8. Push

@Composable
internal fun PushSection(
    admin: AdminApi,
    scope: CoroutineScope,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("/") }

    SectionCard("Push · broadcast") {
        Text(
            "Sends to every persisted subscriber. The url must start with /.",
            color = LocalMinoColors.current.textLow,
            fontSize = 11.sp,
        )
        Field(title, { title = it }, "Title")
        Field(body, { body = it }, "Body", singleLine = false)
        Field(url, { url = it }, "Url (starts with /)")
        ActionButton("Broadcast") {
            onBusy("Broadcasting…")
            scope.launch {
                val message = admin.broadcast(title, body, url)
                onDone(message)
            }
        }
    }
}

// ---------------------------------------------------------------- 9. Wipes

@Composable
internal fun WipesSection(
    admin: AdminApi,
    scope: CoroutineScope,
    onBusy: (String?) -> Unit,
    onDone: (String?) -> Unit,
) {
    val colors = LocalMinoColors.current
    var uid by remember { mutableStateOf("") }

    SectionCard("Wipes") {
        Text(
            "Removes users/{uid}, admin/registry/{uid} and subscriptions/{uid} for one account. " +
                "This cannot be undone.",
            color = colors.danger,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Field(uid, { uid = it }, "uid")
        ActionButton("Wipe this account") {
            onBusy("Wiping…")
            scope.launch {
                val ok = admin.wipe(uid.trim())
                onDone(if (ok) "Wiped ${uid.trim().take(12)}." else "The rules refused that write.")
                if (ok) uid = ""
            }
        }
    }
}
