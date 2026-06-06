package com.infinity.ai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infinity.ai.ai.repository.AIRepository
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.model.ChatMessage
import com.infinity.ai.data.library.LibraryRepository
import com.infinity.ai.data.library.EntryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val repository = AIRepository.getInstance(app)

    // ── Chat state ─────────────────────────────────────────────────────────────
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _showSuggestions = MutableStateFlow(true)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    // ── AI engine state (forwarded from repository) ────────────────────────────
    val aiState: StateFlow<AIInferenceState> = repository.aiState

    // ── Extraction progress (first launch only) ────────────────────────────────
    private val _extractionProgress = MutableStateFlow(0f)
    val extractionProgress: StateFlow<Float> = _extractionProgress.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    // ── Generation job ─────────────────────────────────────────────────────────
    private var generationJob: Job? = null
    private var messageIdCounter = 0L
    private fun nextId() = ++messageIdCounter
    private var userStoppedGeneration = false

    private val historyFile = java.io.File(app.filesDir, "chat_history.txt")

    private fun saveChatHistory(messages: List<ChatMessage>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyFile.bufferedWriter().use { writer ->
                    messages.forEach { msg ->
                        val escapedText = msg.text.replace("\n", "\\n").replace("|", "\\p")
                        writer.write("${msg.id}|${msg.isUser}|$escapedText\n")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save chat history", e)
            }
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!historyFile.exists()) return@launch
            try {
                val list = mutableListOf<ChatMessage>()
                historyFile.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split("|", limit = 3)
                        if (parts.size == 3) {
                            val id = parts[0].toLongOrNull() ?: 0L
                            val isUser = parts[1].toBooleanStrictOrNull() ?: false
                            val text = parts[2].replace("\\n", "\n").replace("\\p", "|")
                            list.add(ChatMessage(id, text, isUser))
                        }
                    }
                }
                _messages.value = list
                messageIdCounter = list.maxOfOrNull { it.id } ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load chat history", e)
            }
        }
    }

    init {
        initializeAI()
        loadChatHistory()
    }

    // ── AI Initialization ──────────────────────────────────────────────────────

    private fun initializeAI() {
        viewModelScope.launch(Dispatchers.IO) {
            _isExtracting.value = !repository.isModelOnDisk()
            repository.initialize { progress ->
                _extractionProgress.value = progress
            }
            _isExtracting.value = false
        }
    }

    // ── Input handling ─────────────────────────────────────────────────────────

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun sendMessage() {
        val text = _input.value.trim()
        if (text.isBlank()) return
        if (aiState.value is AIInferenceState.Thinking ||
            aiState.value is AIInferenceState.Responding) return

        if (aiState.value is AIInferenceState.Loading ||
            aiState.value is AIInferenceState.Error) {
            val userMsg = ChatMessage(nextId(), text, isUser = true)
            val errMsg  = ChatMessage(nextId(),
                if (aiState.value is AIInferenceState.Loading)
                    "Model is still loading, please wait a moment and try again."
                else
                    "AI engine error. Please restart the app.",
                isUser = false)
            _messages.value = _messages.value + userMsg + errMsg
            saveChatHistory(_messages.value)
            _input.value = ""
            _showSuggestions.value = false
            return
        }

        _showSuggestions.value = false
        _input.value = ""

        val userMsg = ChatMessage(nextId(), text, isUser = true)
        _messages.value = _messages.value + userMsg
        saveChatHistory(_messages.value)

        generateReply(text)
    }

    fun startFromSuggestion(prompt: String) {
        _showSuggestions.value = false
        val welcome = ChatMessage(nextId(), "Hello! I'm Infinity. How can I help you today?", isUser = false)
        val userMsg = ChatMessage(nextId(), prompt, isUser = true)
        _messages.value = listOf(welcome, userMsg)
        saveChatHistory(_messages.value)
        if (aiState.value is AIInferenceState.Loading ||
            aiState.value is AIInferenceState.Error) {
            val errMsg = ChatMessage(nextId(),
                if (aiState.value is AIInferenceState.Loading)
                    "Model is still loading, please wait a moment and try again."
                else
                    "AI engine error. Please restart the app.",
                isUser = false)
            _messages.value = _messages.value + errMsg
            saveChatHistory(_messages.value)
            return
        }
        generateReply(prompt)
    }

    fun stopGeneration() {
        userStoppedGeneration = true
        repository.stop()
        generationJob?.cancel()
    }

    fun clearChat() {
        stopGeneration()
        _messages.value = emptyList()
        saveChatHistory(emptyList())
        _input.value = ""
        _showSuggestions.value = true
    }

    // ── Streaming generation ───────────────────────────────────────────────────

    private fun generateReply(userInput: String) {
        val aiMsgId = nextId()
        val placeholder = ChatMessage(aiMsgId, "", isUser = false)
        _messages.value = _messages.value + placeholder

        // Stop previous generation before starting a new one
        userStoppedGeneration = false
        repository.stop()
        generationJob?.cancel()

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            val historyForPrompt = _messages.value
                .dropLast(1)
                .takeLast(20)

            try {
                repository.generate(historyForPrompt, userInput)
                    .catch { e ->
                        Log.e(TAG, "Generation failed: ${e.message}", e)
                        updateMessage(aiMsgId, "Sorry, I encountered an error: ${e.message}")
                    }
                    .onCompletion { cause ->
                        if (cause != null) {
                            Log.e(TAG, "Generation cancelled or failed", cause)
                        }
                        if (!userStoppedGeneration) {
                            val current = _messages.value.find { it.id == aiMsgId }
                            if (current?.text.isNullOrBlank()) {
                                updateMessage(aiMsgId, "I couldn't generate a response. Please try again.")
                            } else {
                                val responseText = current.text
                                viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        val repo = LibraryRepository.getInstance(getApplication())
                                        repo.save(
                                            type = EntryType.CHAT,
                                            content = "Q: $userInput\n\nA: $responseText",
                                            title = userInput.take(60).ifBlank { "Chat Response" },
                                            sourceInfo = "Infinity Chat"
                                        )
                                    } catch (ex: Exception) {
                                        Log.e(TAG, "Failed to save chat to library", ex)
                                    }
                                }
                            }
                        }
                        saveChatHistory(_messages.value)
                    }
                    .collect { token -> appendToken(aiMsgId, token) }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during generation", e)
                updateMessage(aiMsgId, "An unexpected error occurred. Please try again.")
            }
        }
    }

    private fun appendToken(msgId: Long, token: String) {
        // Bug fix: removed `if (updated !== current)` reference-equality guard.
        // List.map{} always returns a new object so the check was always true —
        // a misleading no-op. Just assign directly.
        _messages.value = _messages.value.map { msg ->
            if (msg.id == msgId) msg.copy(text = msg.text + token) else msg
        }
    }

    private fun updateMessage(msgId: Long, text: String) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == msgId) msg.copy(text = text) else msg
        }
    }

    // ── Lifecycle cleanup ──────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
        repository.unload()
    }
}
