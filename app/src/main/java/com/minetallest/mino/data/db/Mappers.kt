package com.minetallest.mino.data.db

import com.minetallest.mino.domain.Chat
import com.minetallest.mino.domain.ChatFolder
import com.minetallest.mino.domain.ChatMessage
import com.minetallest.mino.domain.DocumentAttachment
import com.minetallest.mino.domain.GeneratedImage
import com.minetallest.mino.domain.ImageAttachment
import com.minetallest.mino.domain.Memory
import com.minetallest.mino.domain.Role
import com.minetallest.mino.domain.SearchSource
import com.minetallest.mino.domain.TokenUsage
import com.minetallest.mino.domain.TrashEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object Mappers {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    private val imageList = ListSerializer(ImageAttachment.serializer())
    private val genList = ListSerializer(GeneratedImage.serializer())
    private val docList = ListSerializer(DocumentAttachment.serializer())
    private val sourceList = ListSerializer(SearchSource.serializer())
    private val stringList = ListSerializer(kotlinx.serialization.serializer<String>())
    private val messageList = ListSerializer(ChatMessage.serializer())

    fun encodeImages(v: List<ImageAttachment>?) = v?.let { json.encodeToString(imageList, it) }
    fun decodeImages(s: String?) = s?.let { json.decodeFromString(imageList, it) }
    fun encodeGenerated(v: List<GeneratedImage>?) = v?.let { json.encodeToString(genList, it) }
    fun decodeGenerated(s: String?) = s?.let { json.decodeFromString(genList, it) }
    fun encodeDocs(v: List<DocumentAttachment>?) = v?.let { json.encodeToString(docList, it) }
    fun decodeDocs(s: String?) = s?.let { json.decodeFromString(docList, it) }
    fun encodeSources(v: List<SearchSource>?) = v?.let { json.encodeToString(sourceList, it) }
    fun decodeSources(s: String?) = s?.let { json.decodeFromString(sourceList, it) }
    fun encodeStrings(v: List<String>?) = v?.let { json.encodeToString(stringList, it) }
    fun decodeStrings(s: String?) = s?.let { json.decodeFromString(stringList, it) }
    fun encodeUsage(v: TokenUsage?) = v?.let { json.encodeToString(TokenUsage.serializer(), it) }
    fun decodeUsage(s: String?) = s?.let { json.decodeFromString(TokenUsage.serializer(), it) }
    fun encodeMessages(v: List<ChatMessage>) = json.encodeToString(messageList, v)
    fun decodeMessages(s: String) = json.decodeFromString(messageList, s)

    fun ChatEntity.toDomain() = Chat(id, title, createdAt, updatedAt, pinned, folder)
    fun Chat.toEntity() = ChatEntity(id, title, createdAt, updatedAt, pinned, folder)

    fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        chatId = chatId,
        role = Role.fromWire(role),
        content = content,
        images = decodeImages(images),
        generatedImages = decodeGenerated(generatedImages),
        documents = decodeDocs(documents),
        model = model,
        searchQuery = searchQuery,
        sources = decodeSources(sources),
        usage = decodeUsage(usage),
        truncated = truncated,
        error = error,
        variants = decodeStrings(variants),
        variantIndex = variantIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun ChatMessage.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        role = role.wire,
        content = content,
        model = model,
        searchQuery = searchQuery,
        truncated = truncated,
        error = error,
        variantIndex = variantIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
        images = encodeImages(images),
        generatedImages = encodeGenerated(generatedImages),
        documents = encodeDocs(documents),
        sources = encodeSources(sources),
        usage = encodeUsage(usage),
        variants = encodeStrings(variants),
    )

    fun MemoryEntity.toDomain() = Memory(id, text, createdAt, updatedAt)
    fun Memory.toEntity() = MemoryEntity(id, text, createdAt, updatedAt)
    fun FolderEntity.toDomain() = ChatFolder(id, name, createdAt)
    fun ChatFolder.toEntity() = FolderEntity(id, name, createdAt)

    fun TrashEntity.toDomain() = TrashEntry(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pinned = pinned,
        folder = folder,
        messages = decodeMessages(messages),
        deletedAt = deletedAt,
    )

    fun TrashEntry.toEntity() = TrashEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pinned = pinned,
        folder = folder,
        messages = encodeMessages(messages),
        deletedAt = deletedAt,
    )
}
