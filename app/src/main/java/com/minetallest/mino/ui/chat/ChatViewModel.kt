package com.minetallest.mino.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minetallest.mino.AppContainer
import com.minetallest.mino.data.ChatRepository
import com.minetallest.mino.domain.Chat
import com.minetallest.mino.domain.ChatEvent
import com.minetallest.mino.domain.ChatFolder
import com.minetallest.mino.domain.ChatMessage
import com.minetallest.mino.domain.DocumentAttachment
import com.minetallest.mino.domain.GeneratedImage
import com.minetallest.mino.domain.ImageAttachment
import com.minetallest.mino.domain.LocalSearch
import com.minetallest.mino.domain.Memory
import com.minetallest.mino.domain.MemoryGuard
import com.minetallest.mino.domain.ModeId
import com.minetallest.mino.domain.Models
import com.minetallest.mino.domain.ReasoningEffort
import com.minetallest.mino.domain.Refusals
import com.minetallest.mino.domain.ResponseLength
import com.minetallest.mino.domain.Role
import com.minetallest.mino.domain.Rules
import com.minetallest.mino.domain.SearchMode
import com.minetallest.mino.domain.SearchSource
import com.minetallest.mino.domain.TokenUsage
import com.minetallest.mino.domain.TrashEntry
import com.minetallest.mino.domain.Variants
import com.minetallest.mino.domain.Wire
import com.minetallest.mino.net.AppConfig
import com.minetallest.mino.net.MinoApi
import com.minetallest.mino.net.SseChatClient
import com.minetallest.mino.util.Images
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone

class ChatViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository
    private val prefs = container.prefs
    private val api = container.api
    private val sse = container.sse

    data class ComposerState(
        val text: String = "",
        val images: List<ImageAttachment> = emptyList(),
        val documents: List<DocumentAttachment> = emptyList(),
        val attachmentError: String? = null,
        val compressing: Boolean = false,
        val voiceListening: Boolean = false,
        val imageMode: Boolean = false,
        val imageGenerating: Boolean = false,
        val rateLimitUntil: Long? = null,
    )

    data class Notice(val id: Long, val text: String, val tone: Tone) {
        enum class Tone { NEUTRAL, SUCCESS, AMBER, RED }
    }

    data class UiState(
        val hydrated: Boolean = false,
        val splashDone: Boolean = false,
        val displayName: String? = null,
        val maintenanceMessage: String? = null,
        val mode: ModeId = ModeId.AUTO,
        val searchMode: SearchMode = SearchMode.AUTO,
        val responseLength: ResponseLength = ResponseLength.BALANCED,
        val reasoningEffort: ReasoningEffort = ReasoningEffort.LOW,
        val memorySuggestEnabled: Boolean = true,
        val chats: List<Chat> = emptyList(),
        val folders: List<ChatFolder> = emptyList(),
        val memories: List<Memory> = emptyList(),
        val trash: List<TrashEntry> = emptyList(),
        val activeChatId: String? = null,
        val temporary: Boolean = false,
        val temporaryMessages: List<ChatMessage> = emptyList(),
        val messages: List<ChatMessage> = emptyList(),
        val streaming: Boolean = false,
        val streamHint: String? = null,
        val modelNotice: Boolean = false,
        val availability: MinoApi.Availability? = null,
        val serverConfigured: Boolean = false,
        val apiBase: String = "",
        val config: AppConfig = AppConfig.DEFAULT,
        val plan: MinoApi.PlanInfo? = null,
        val dew: MinoApi.DewInfo? = null,
        val firebaseConfigured: Boolean = false,
        val notices: List<Notice> = emptyList(),
        val rememberDialogOpen: Boolean = false,
        val memorySuggestion: List<String> = emptyList(),
        val drawerOpen: Boolean = false,
        val searchOpen: Boolean = false,
        val trashOpen: Boolean = false,
        val galleryOpen: Boolean = false,
        val settingsOpen: Boolean = false,
        val newNoteIds: List<String> = emptyList(),
    )

    private val _ui = MutableStateFlow(UiState(firebaseConfigured = container.firebaseConfigured))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _composer = MutableStateFlow(ComposerState())
    val composer: StateFlow<ComposerState> = _composer.asStateFlow()

    private var streamHandle: SseChatClient.Handle? = null
    private var memoryRun = 0
    private val waitingJokes = listOf(
        "umm, finding ai suitable for your weird request",
        "Whatt?",
        "Almost done.. Just kidding",
        "Hacking your computer",
        "Please wait while we're staying your data",
    )

    init {
        observeEverything()
        bootstrap()
    }

    // ---------------------------------------------------------------- lifecycle

    private fun observeEverything() {
        viewModelScope.launch { repo.chats.collect { list -> _ui.update { it.copy(chats = list) } } }
        viewModelScope.launch { repo.folders.collect { list -> _ui.update { it.copy(folders = list) } } }
        viewModelScope.launch { repo.memories.collect { list -> _ui.update { it.copy(memories = list) } } }
        viewModelScope.launch { repo.trash.collect { list -> _ui.update { it.copy(trash = list) } } }
        viewModelScope.launch { prefs.selectedMode.collect { v -> _ui.update { it.copy(mode = v) } } }
        viewModelScope.launch { prefs.searchMode.collect { v -> _ui.update { it.copy(searchMode = v) } } }
        viewModelScope.launch { prefs.responseLength.collect { v -> _ui.update { it.copy(responseLength = v) } } }
        viewModelScope.launch { prefs.reasoningEffort.collect { v -> _ui.update { it.copy(reasoningEffort = v) } } }
        viewModelScope.launch { prefs.memorySuggest.collect { v -> _ui.update { it.copy(memorySuggestEnabled = v) } } }
        viewModelScope.launch { prefs.apiBase.collect { v -> _ui.update { it.copy(apiBase = v) } } }
        viewModelScope.launch {
            var lastId: String? = null
            var job: Job? = null
            _ui.collect { state ->
                val id = state.activeChatId
                if (id != lastId) {
                    lastId = id
                    job?.cancel()
                    if (id == null) {
                        _ui.update { it.copy(messages = emptyList()) }
                    } else {
                        val flow: Flow<List<ChatMessage>> = repo.messagesFor(id)
                        job = viewModelScope.launch {
                            flow.collect { messages -> _ui.update { it.copy(messages = messages) } }
                        }
                    }
                }
            }
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            repo.purgeExpiredTrash(System.currentTimeMillis())
            val name = prefs.displayName.first()
            val firstSeen = prefs.firstSeen.first()
            if (firstSeen == null) prefs.setFirstSeen(System.currentTimeMillis())
            _ui.update {
                it.copy(hydrated = true, displayName = name)
            }
            refreshServerState()
        }
    }

    /** Availability + plan + public config probes (spec §6.3, §4.4). */
    fun refreshServerState() {
        viewModelScope.launch {
            val configured = container.apiClient.isConfigured()
            val availability = api.availability()
            _ui.update { it.copy(serverConfigured = configured, availability = availability) }
            if (availability != null) {
                val current = _ui.value.mode
                val open = availability.available
                if (open.isNotEmpty() && current !in open) {
                    prefs.setSelectedMode(open.first())
                }
            }
            val config = container.config.read()
            if (config != null) {
                _ui.update { it.copy(config = config) }
                val ack = prefs.subscriptionAck.first()
                val newest = config.notes.maxOfOrNull { note -> note.id.hashCode().toLong() } ?: 0L
                if (newest > ack && config.notes.isNotEmpty()) {
                    _ui.update { it.copy(newNoteIds = config.notes.map { n -> n.id }) }
                }
            }
            if (container.firebaseConfigured) {
                container.identity.ensureSignedIn()
                _ui.update { it.copy(plan = api.plan()) }
                refreshDew()
            }
        }
    }

    private suspend fun refreshDew() {
        val balance = api.dew()
        if (balance != null) {
            _ui.update { it.copy(dew = balance) }
        } else if (!prefs.dewTrialClaimed.first()) {
            val claimed = api.claimDewTrial()
            prefs.setDewTrialClaimed()
            if (claimed != null) _ui.update { it.copy(dew = claimed) }
        }
    }

    /** Spec §6.1 — splash once per launch, then name gate with no skip. */
    fun finishSplash() = _ui.update { it.copy(splashDone = true) }

    fun saveDisplayName(raw: String) {
        val name = Rules.normalizeDisplayName(raw)
        if (name.isEmpty()) return
        viewModelScope.launch {
            prefs.setDisplayName(name)
            _ui.update { it.copy(displayName = name) }
        }
    }

    fun markNotesSeen(ids: List<String>) {
        viewModelScope.launch {
            val seen = (prefs.notesSeen.first() + ids).distinct()
            prefs.markNotesSeen(seen)
            _ui.update { it.copy(newNoteIds = emptyList()) }
        }
    }

    // ---------------------------------------------------------------- overlay state

    fun setDrawer(open: Boolean) = _ui.update { it.copy(drawerOpen = open) }
    fun setSearchOpen(open: Boolean) = _ui.update { it.copy(searchOpen = open) }
    fun setTrashOpen(open: Boolean) = _ui.update { it.copy(trashOpen = open) }
    fun setGalleryOpen(open: Boolean) = _ui.update { it.copy(galleryOpen = open) }
    fun setSettingsOpen(open: Boolean) = _ui.update { it.copy(settingsOpen = open) }
    fun setRememberDialog(open: Boolean) = _ui.update { it.copy(rememberDialogOpen = open) }
    fun dismissMemorySuggestion() = _ui.update { it.copy(memorySuggestion = emptyList()) }

    fun notice(text: String, tone: Notice.Tone = Notice.Tone.NEUTRAL) {
        val item = Notice(System.currentTimeMillis(), text, tone)
        _ui.update { it.copy(notices = it.notices + item) }
        viewModelScope.launch {
            delay(3500)
            _ui.update { state -> state.copy(notices = state.notices.filterNot { it.id == item.id }) }
        }
    }

    // ---------------------------------------------------------------- chats

    fun startNewChat() {
        viewModelScope.launch {
            stopStream()
            if (_ui.value.temporary) _ui.update { it.copy(temporaryMessages = emptyList()) }
            val chat = repo.createChat(System.currentTimeMillis())
            _ui.update {
                it.copy(activeChatId = chat.id, temporary = false, drawerOpen = false, modelNotice = false)
            }
        }
    }

    fun selectChat(chatId: String) {
        viewModelScope.launch {
            stopStream()
            if (_ui.value.temporary) _ui.update { it.copy(temporaryMessages = emptyList()) }
            _ui.update {
                it.copy(activeChatId = chatId, temporary = false, drawerOpen = false, modelNotice = false)
            }
        }
    }

    /** Temporary chat — thread lives only in memory, never Room (spec §10.7). */
    fun toggleTemporary(enabled: Boolean? = null) {
        viewModelScope.launch {
            stopStream()
            val next = enabled ?: !_ui.value.temporary
            if (next) {
                _ui.update {
                    it.copy(
                        temporary = true,
                        activeChatId = null,
                        temporaryMessages = emptyList(),
                        drawerOpen = false,
                        modelNotice = false,
                    )
                }
            } else {
                _ui.update { it.copy(temporary = false, temporaryMessages = emptyList()) }
                startNewChat()
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            if (chatId == _ui.value.activeChatId) stopStream()
            repo.deleteChat(chatId, System.currentTimeMillis())
            if (chatId == _ui.value.activeChatId) {
                _ui.update { it.copy(activeChatId = null) }
                startNewChat()
            }
        }
    }

    fun restoreChat(chatId: String) {
        viewModelScope.launch {
            val chat = repo.restoreFromTrash(chatId)
            if (chat != null) {
                notice("Chat restored", Notice.Tone.SUCCESS)
                selectChat(chat.id)
            }
        }
    }

    fun forgetTrash(chatId: String) = viewModelScope.launch { repo.forgetTrash(chatId) }

    fun togglePin(chatId: String, pinned: Boolean) = viewModelScope.launch { repo.setPinned(chatId, pinned) }

    fun renameChat(chatId: String, title: String) = viewModelScope.launch { repo.rename(chatId, title) }

    fun moveChatToFolder(chatId: String, folderId: String?) =
        viewModelScope.launch { repo.setFolder(chatId, folderId) }

    fun createFolder(name: String, fileChatId: String? = null) {
        viewModelScope.launch {
            val folder = repo.createFolder(name, System.currentTimeMillis()) ?: return@launch
            if (fileChatId != null) repo.setFolder(fileChatId, folder.id)
        }
    }

    fun deleteFolder(folderId: String) = viewModelScope.launch { repo.deleteFolder(folderId) }

    fun renameFolder(folderId: String, name: String) = viewModelScope.launch {
        repo.renameFolder(folderId, name)
    }

    // ---------------------------------------------------------------- sending

    fun sendFromComposer() {
        val state = _composer.value
        send(
            text = state.text,
            images = if (state.imageMode) emptyList() else state.images,
            documents = if (state.imageMode) emptyList() else state.documents,
            mode = _ui.value.mode,
            searchMode = _ui.value.searchMode,
            imageMode = state.imageMode,
        )
    }

    fun    send(
        text: String,
        images: List<ImageAttachment> = emptyList(),
        documents: List<DocumentAttachment> = emptyList(),
        mode: ModeId,
        searchMode: SearchMode,
        imageMode: Boolean = false,
    ) {
        val content = text.trim()
        if (content.isEmpty() && images.isEmpty()) return
        if (_ui.value.streaming) return
        _composer.update { it.copy(text = "", images = emptyList(), documents = emptyList(), attachmentError = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val temporary = _ui.value.temporary
            val chatId = if (temporary) {
                null
            } else {
                _ui.value.activeChatId ?: repo.createChat(now).id.also { id ->
                    _ui.update { it.copy(activeChatId = id) }
                }
            }
            val userMessage = ChatMessage(
                id = Rules.uid(),
                chatId = chatId ?: "temporary",
                role = Role.USER,
                content = content,
                images = images.ifEmpty { null },
                documents = documents.ifEmpty { null },
                createdAt = now,
            )
            if (temporary) {
                _ui.update { it.copy(temporaryMessages = it.temporaryMessages + userMessage) }
            } else if (chatId != null) {
                repo.appendUserMessage(chatId, userMessage, now)
            }
            if (imageMode) {
                generateImage(content)
            } else {
                streamReply(chatId, mode, searchMode, replace = null)
            }
        }
    }

    /** `POST /api/image` in image mode (spec §9.2). */
    private fun generateImage(prompt: String) {
        viewModelScope.launch {
            _composer.update { it.copy(imageGenerating = true) }
            val now = System.currentTimeMillis()
            val chatId = _ui.value.activeChatId ?: "temporary"
            when (val result = api.generateImage(prompt)) {
                is MinoApi.ImageResult.Ok -> {
                    val message = ChatMessage(
                        id = Rules.uid(),
                        chatId = chatId,
                        role = Role.ASSISTANT,
                        content = "",
                        model = "Mino Canvas",
                        generatedImages = listOf(
                            GeneratedImage(
                                url = "data:image/png;base64," +
                                    android.util.Base64.encodeToString(result.bytes, android.util.Base64.NO_WRAP),
                                prompt = prompt,
                                mime = "image/png",
                                model = "Mino Canvas",
                                createdAt = now,
                            ),
                        ),
                        createdAt = now,
                        updatedAt = now,
                    )
                    putAssistant(message)
                    if (_ui.value.activeChatId != null) repo.touch(chatId, now)
                }
                is MinoApi.ImageResult.Error -> notice(result.message, Notice.Tone.RED)
                MinoApi.ImageResult.NotConfigured ->
                    notice("Mino's image model is not set up on this server yet.", Notice.Tone.AMBER)
            }
            _composer.update { it.copy(imageGenerating = false) }
        }
    }

    private suspend fun putAssistant(message: ChatMessage) {
        if (_ui.value.temporary && message.chatId == "temporary") {
            val list = _ui.value.temporaryMessages
            val index = list.indexOfFirst { it.id == message.id }
            _ui.update {
                it.copy(
                    temporaryMessages = if (index >= 0) {
                        list.toMutableList().apply { set(index, message) }
                    } else {
                        list + message
                    },
                )
            }
        } else {
            repo.putMessage(message)
        }
    }

    private suspend fun removeAssistant(messageId: String) {
        if (_ui.value.temporary) {
            _ui.update { it.copy(temporaryMessages = it.temporaryMessages.filterNot { m -> m.id == messageId }) }
        } else {
            repo.deleteMessage(messageId)
        }
    }

    /** SSE streaming with model firewall, variants, truncation and rate-limit parsing (spec §9.1). */
    private suspend fun streamReply(
        chatId: String?,
        mode: ModeId,
        searchMode: SearchMode,
        replace: ChatMessage?,
    ) {
        val now = System.currentTimeMillis()
        val history = if (chatId == null) _ui.value.temporaryMessages else repo.messagesOnce(chatId)
        val assistantId = replace?.id ?: Rules.uid()
        val assistantChatId = chatId ?: "temporary"
        val retired = replace?.let { Variants.retire(it) }
        val placeholder = ChatMessage(
            id = assistantId,
            chatId = assistantChatId,
            role = Role.ASSISTANT,
            content = "",
            model = Models.getModelDisplayName(Models.badgeFor(mode)),
            variants = retired?.variants,
            variantIndex = 0,
            createdAt = replace?.createdAt ?: now,
        )
        putAssistant(placeholder)
        _ui.update {
            it.copy(streaming = true, streamHint = waitingJokes.random(), modelNotice = false, memorySuggestion = emptyList())
        }

        val request = Wire.buildChatRequest(
            Wire.ChatRequest(
                messages = history.filterNot { it.id == assistantId },
                mode = mode,
                searchMode = searchMode,
                reasoningEffort = _ui.value.reasoningEffort,
                responseLength = _ui.value.responseLength,
                memories = _ui.value.memories,
                timeZone = TimeZone.getDefault().id,
            ),
        )

        val answer = StringBuilder()
        var model = placeholder.model
        var error: String? = null
        var truncated = false
        var usage: TokenUsage? = null
        var sources: List<SearchSource>? = null
        var searchQuery: String? = null
        val stream = sse.start(request)
        streamHandle = stream.handle

        try {
            stream.events.collect { event ->
                when (event) {
                    is ChatEvent.Model -> {
                        val label = Models.getModelDisplayName(event.model)
                        model = label
                        if (label != Models.getModelDisplayName(placeholder.model)) {
                            _ui.update { it.copy(modelNotice = true) }
                        }
                        putAssistant(placeholder.copy(model = label))
                    }
                    is ChatEvent.Content -> {
                        answer.append(event.content)
                        putAssistant(placeholder.copy(content = answer.toString(), model = model))
                    }
                    is ChatEvent.Usage -> usage = TokenUsage(event.prompt, event.completion, event.total)
                    is ChatEvent.Truncated -> truncated = true
                    is ChatEvent.Search -> {
                        searchQuery = event.query
                        sources = event.sources
                        putAssistant(placeholder.copy(searchQuery = event.query, sources = event.sources))
                    }
                    is ChatEvent.Google -> {}
                    is ChatEvent.Error -> error = event.message
                    ChatEvent.Done -> {}
                }
            }
        } finally {
            streamHandle = null
        }

        val canceled = stream.handle.canceled
        val finalText = answer.toString()
        _ui.update { it.copy(streaming = false, streamHint = null) }

        // Aborted stream with no text → the empty bubble is deleted (spec §16).
        if (canceled && finalText.isBlank()) {
            removeAssistant(assistantId)
            return
        }

        val finalError = error ?: if (finalText.isBlank()) Refusals.EMPTY_RESPONSE else null
        val finished = placeholder.copy(
            content = finalText,
            model = model,
            truncated = truncated,
            error = finalError,
            usage = usage,
            sources = sources,
            searchQuery = searchQuery,
            updatedAt = System.currentTimeMillis(),
        )
        putAssistant(finished)
        if (chatId != null) repo.touch(chatId, System.currentTimeMillis())

        Refusals.parseWaitSeconds(finalError)?.let { seconds ->
            _composer.update { it.copy(rateLimitUntil = System.currentTimeMillis() + seconds * 1000L) }
        }

        // Mino's memory offer (spec §10.2) — nothing is ever stored without a tap.
        if (finalText.isNotBlank() && _ui.value.memorySuggestEnabled && !_ui.value.temporary) {
            val run = ++memoryRun
            val userText = history.lastOrNull { it.role == Role.USER }?.displayedContent().orEmpty()
            val suggestions = api.memorySuggestions(userText, finalText, _ui.value.memories)
            if (run == memoryRun && suggestions.isNotEmpty()) {
                _ui.update { it.copy(memorySuggestion = suggestions.take(3)) }
            }
        }
    }

    fun stopStream() {
        streamHandle?.cancel()
        streamHandle = null
        _ui.update { it.copy(streaming = false, streamHint = null) }
    }

    /** Retry (spec #9): the old answer retires into variants; the row is reused. */
    fun retry(messageId: String) {
        viewModelScope.launch {
            if (_ui.value.streaming) return@launch
            val messages = currentMessages()
            val message = messages.firstOrNull { it.id == messageId } ?: return@launch
            if (message.content.isBlank() && !message.hasVariants()) {
                val userMessage = messages.takeWhile { it.id != messageId }.lastOrNull { it.role == Role.USER }
                if (userMessage != null) {
                    deleteMessageRangeAfter(userMessage.id)
                    streamReply(chatIdOrNull(), _ui.value.mode, _ui.value.searchMode, replace = null)
                }
                return@launch
            }
            streamReply(chatIdOrNull(), _ui.value.mode, _ui.value.searchMode, replace = message)
        }
    }

    /** Edit & send: rewrite the user message, branch the reply (spec #9). */
    fun editAndSend(userMessageId: String, newText: String) {
        viewModelScope.launch {
            if (_ui.value.streaming) return@launch
            val messages = currentMessages()
            val index = messages.indexOfFirst { it.id == userMessageId }
            if (index < 0) return@launch
            val edited = messages[index].copy(
                content = newText.trim(),
                images = null,
                documents = null,
                createdAt = System.currentTimeMillis(),
            )
            if (edited.content.isEmpty()) return@launch
            messages.drop(index + 1).forEach { removeAssistant(it.id) }
            putAssistant(edited)
            if (!_ui.value.temporary) {
                _ui.value.activeChatId?.let { repo.touch(it, System.currentTimeMillis()) }
            }
            val reply = messages.getOrNull(index + 1)?.takeIf { it.role == Role.ASSISTANT }
            streamReply(chatIdOrNull(), _ui.value.mode, _ui.value.searchMode, replace = reply)
        }
    }

    /** Variant stepper (spec §6.6): position 0 = live, k = variants[k-1]. */
    fun setVariant(messageId: String, index: Int) {
        viewModelScope.launch {
            val message = currentMessages().firstOrNull { it.id == messageId } ?: return@launch
            val max = message.variants?.size ?: 0
            putAssistant(message.copy(variantIndex = index.coerceIn(0, max)))
        }
    }

    private fun currentMessages(): List<ChatMessage> =
        if (_ui.value.temporary) _ui.value.temporaryMessages else _ui.value.messages

    private fun chatIdOrNull(): String? = if (_ui.value.temporary) null else _ui.value.activeChatId

    private suspend fun deleteMessageRangeAfter(messageId: String) {
        val messages = currentMessages()
        val index = messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        messages.drop(index + 1).forEach { removeAssistant(it.id) }
    }

    // ---------------------------------------------------------------- composer

    fun setComposerText(text: String) = _composer.update { it.copy(text = text) }

    fun setAttachmentError(error: String?) = _composer.update { it.copy(attachmentError = error) }

    fun toggleImageMode(enabled: Boolean) = _composer.update { it.copy(imageMode = enabled) }

    fun removeImage(url: String) =
        _composer.update { it.copy(images = it.images.filterNot { img -> img.url == url }) }

    fun removeDocument(name: String) =
        _composer.update { it.copy(documents = it.documents.filterNot { doc -> doc.name == name }) }

    fun setVoiceListening(listening: Boolean) = _composer.update { it.copy(voiceListening = listening) }

    fun appendVoiceTranscript(transcript: String) =
        _composer.update { it.copy(text = (it.text + " " + transcript).trim()) }

    fun addImages(uris: List<Uri>) {
        val appContext = container.appContext
        viewModelScope.launch {
            val room = Images.MAX_IMAGES - _composer.value.images.size
            if (room <= 0) {
                setAttachmentError("Max ${Images.MAX_IMAGES} images per message")
                return@launch
            }
            _composer.update { it.copy(compressing = true) }
            val added = uris.take(room).mapNotNull { uri -> Images.compressToDataUrl(appContext, uri) }
            if (added.size < uris.size) setAttachmentError("Max ${Images.MAX_IMAGES} images per message")
            _composer.update { it.copy(images = it.images + added, compressing = false) }
        }
    }

    fun addDocuments(uris: List<Uri>) {
        val appContext = container.appContext
        viewModelScope.launch {
            when (val result = Images.readDocuments(appContext, uris, _composer.value.documents.size)) {
                is Images.DocResult.Ok ->
                    _composer.update { it.copy(documents = it.documents + result.documents, attachmentError = null) }
                is Images.DocResult.Error -> setAttachmentError(result.message)
            }
        }
    }

    /** "Queue until then" (spec §6.7): hold behind the wait, send when it lifts. */
    fun queueUntilRateLimitLifts() {
        val until = _composer.value.rateLimitUntil ?: return
        val state = _composer.value
        if (state.text.isBlank()) return
        _composer.update { it.copy(text = "") }
        viewModelScope.launch {
            val wait = until - System.currentTimeMillis() + 1000
            if (wait > 0) delay(wait)
            _composer.update { it.copy(rateLimitUntil = null) }
            send(
                text = state.text,
                images = state.images,
                documents = state.documents,
                mode = _ui.value.mode,
                searchMode = _ui.value.searchMode,
                imageMode = state.imageMode,
            )
        }
    }

    fun clearRateLimit() = _composer.update { it.copy(rateLimitUntil = null) }

    // ---------------------------------------------------------------- modes & settings

    /** Spec §6.3 — a separate-session mode aborts the stream and starts a fresh session. */
    fun setMode(mode: ModeId) {
        viewModelScope.launch {
            val wasDifferent = mode != _ui.value.mode
            prefs.setSelectedMode(mode)
            if (Models.info(mode).separateSession && wasDifferent) {
                stopStream()
                _composer.update { it.copy(imageMode = false) }
                _ui.update { it.copy(drawerOpen = false, modelNotice = false) }
                if (_ui.value.temporary) {
                    _ui.update { it.copy(temporaryMessages = emptyList(), temporary = false) }
                }
                val chat = repo.createChat(System.currentTimeMillis())
                _ui.update { it.copy(activeChatId = chat.id, mode = mode) }
            } else {
                _ui.update { it.copy(mode = mode) }
            }
        }
    }

    fun setSearchMode(mode: SearchMode) = viewModelScope.launch { prefs.setSearchMode(mode) }
    fun setResponseLength(value: ResponseLength) = viewModelScope.launch { prefs.setResponseLength(value) }
    fun setReasoningEffort(value: ReasoningEffort) = viewModelScope.launch { prefs.setReasoningEffort(value) }
    fun setMemorySuggest(enabled: Boolean) = viewModelScope.launch { prefs.setMemorySuggest(enabled) }

    fun setApiBase(value: String) {
        viewModelScope.launch {
            prefs.setApiBase(value)
            refreshServerState()
        }
    }

    // ---------------------------------------------------------------- memory

    fun remember(text: String): MemoryGuard.Result {
        val result = MemoryGuard.normalize(text, _ui.value.memories)
        if (result is MemoryGuard.Result.Ok) {
            val now = System.currentTimeMillis()
            viewModelScope.launch { repo.putMemory(Memory(Rules.uid(), result.text, now, now)) }
        }
        return result
    }

    fun forgetMemory(id: String) = viewModelScope.launch { repo.deleteMemory(id) }

    fun forgetEverything() = viewModelScope.launch { repo.clearMemories() }

    fun acceptMemorySuggestion(text: String) {
        val result = MemoryGuard.normalize(text, _ui.value.memories)
        if (result is MemoryGuard.Result.Ok) {
            val now = System.currentTimeMillis()
            viewModelScope.launch { repo.putMemory(Memory(Rules.uid(), result.text, now, now)) }
        }
        _ui.update { it.copy(memorySuggestion = it.memorySuggestion.filterNot { s -> s == text }) }
    }

    fun dontSuggestAgain() {
        viewModelScope.launch {
            prefs.setMemorySuggestOff(true)
            prefs.setMemorySuggest(false)
            _ui.update { it.copy(memorySuggestEnabled = false, memorySuggestion = emptyList()) }
        }
    }

    // ---------------------------------------------------------------- data controls

    suspend fun exportBackup(): String = repo.exportBackup(System.currentTimeMillis())

    fun importBackup(text: String) {
        viewModelScope.launch {
            when (val result = repo.restoreBackup(text)) {
                is ChatRepository.RestoreOutcome.Ok ->
                    notice("Imported ${result.chats} chats · ${result.messages} messages", Notice.Tone.SUCCESS)
                is ChatRepository.RestoreOutcome.Error ->
                    notice("Import failed: ${result.reason}", Notice.Tone.RED)
            }
        }
    }

    fun wipeEverything() {
        viewModelScope.launch {
            repo.wipeAll()
            _ui.update { it.copy(activeChatId = null, temporaryMessages = emptyList(), temporary = false) }
            notice("All data cleared", Notice.Tone.SUCCESS)
            startNewChat()
        }
    }

    // ---------------------------------------------------------------- search & gallery

    fun searchAll(query: String): List<LocalSearch.Hit> {
        val state = _ui.value
        return LocalSearch.search(query, state.chats, state.messages)
    }

    data class GalleryItem(
        val messageId: String,
        val chatId: String,
        val url: String,
        val prompt: String?,
        val createdAt: Long,
        val generated: Boolean,
    )

    /** Spec §10.5 — a view over history: every image created or sent on this device. */
    suspend fun galleryItems(): List<GalleryItem> {
        val messages = repo.allMessages()
        val items = ArrayList<GalleryItem>()
        for (message in messages) {
            message.images.orEmpty().forEach { image ->
                items.add(GalleryItem(message.id, message.chatId, image.url, null, message.createdAt, false))
            }
            message.generatedImages.orEmpty().forEach { image ->
                items.add(GalleryItem(message.id, message.chatId, image.url, image.prompt, image.createdAt, true))
            }
        }
        return items.sortedByDescending { it.createdAt }
    }

    /** Origin used for share-as-link fragments (spec §10.10). */
    suspend fun serverOrigin(): String = container.apiClient.baseUrl()
}
