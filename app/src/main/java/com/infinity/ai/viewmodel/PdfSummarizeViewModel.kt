package com.infinity.ai.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infinity.ai.ai.repository.AIRepository
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.data.library.EntryType
import com.infinity.ai.data.library.LibraryRepository
import com.infinity.ai.model.ChatMessage
import com.infinity.ai.pdf.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * PdfSummarizeViewModel
 *
 * Completely independent from ChatViewModel. Owns its own AIRepository instance.
 * Calls repository.generate() with an empty history and a summarization prompt —
 * the exact same API surface ChatViewModel uses. Zero changes to the AI pipeline.
 *
 * State machine:
 *   Idle → Extracting → Summarizing → Done
 *                                   → Error
 */
class PdfSummarizeViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "PdfSummarizeVM"
        // If no token arrives within this window the prefill is stuck (prompt too large
        // or model error). Surface an error instead of stalling the UI indefinitely.
        private const val FIRST_TOKEN_TIMEOUT_MS = 60_000L
    }

    // ── Own repository instance — isolated from ChatViewModel ─────────────────
    private val repository  = AIRepository(app)
    private val extractor    = PdfTextExtractor(app)
    private val libraryRepo  = LibraryRepository.getInstance(app)

    private val _showSavedBanner = MutableStateFlow(false)
    val showSavedBanner: StateFlow<Boolean> = _showSavedBanner.asStateFlow()

    // ── Public state ──────────────────────────────────────────────────────────
    val aiState: StateFlow<AIInferenceState> = repository.aiState

    private val _uiState = MutableStateFlow<PdfSummarizeUiState>(PdfSummarizeUiState.Idle)
    val uiState: StateFlow<PdfSummarizeUiState> = _uiState.asStateFlow()

    private val _summaryText = MutableStateFlow("")
    val summaryText: StateFlow<String> = _summaryText.asStateFlow()

    private val _extractionProgress = MutableStateFlow(0f)
    val extractionProgress: StateFlow<Float> = _extractionProgress.asStateFlow()

    private var generationJob: Job? = null
    private var userStopped = false

    // ── AI initialisation (same pattern as ChatViewModel.initializeAI) ────────
    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initialize()
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    fun summarize(uri: Uri) {
        if (_uiState.value is PdfSummarizeUiState.Extracting ||
            _uiState.value is PdfSummarizeUiState.Summarizing) return

        generationJob?.cancel()
        _summaryText.value = ""
        userStopped = false

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            // ── 1. Extract text from PDF ──────────────────────────────────────
            _uiState.value = PdfSummarizeUiState.Extracting
            _extractionProgress.value = 0f

            val extractResult = extractor.extract(uri) { done, total ->
                _extractionProgress.value = if (total > 0) done.toFloat() / total else 0f
            }

            val pdfText = extractResult.getOrElse { e ->
                Log.e(TAG, "Extraction failed", e)
                _uiState.value = PdfSummarizeUiState.Error(e.message ?: "Failed to read PDF")
                return@launch
            }

            // ── 2. Check model readiness ──────────────────────────────────────
            val engineState = repository.aiState.value
            if (engineState is AIInferenceState.Error) {
                _uiState.value = PdfSummarizeUiState.Error("AI engine error. Please restart the app.")
                return@launch
            }
            if (engineState is AIInferenceState.Loading) {
                _uiState.value = PdfSummarizeUiState.Error("Model is still loading. Please wait and try again.")
                return@launch
            }

            // ── 3. Stream summary from AI ─────────────────────────────────────
            _uiState.value = PdfSummarizeUiState.Summarizing

            val prompt = buildSummaryPrompt(pdfText)
            Log.i(TAG, "Prompt chars: ${prompt.length}, est. tokens: ~${prompt.length / 4}")

            var firstTokenReceived = false
            var generationError: String? = null

            // Pass empty history — this is not a chat session.
            // The prompt itself contains all the context the model needs.
            try {
                val timedOut = withTimeoutOrNull(FIRST_TOKEN_TIMEOUT_MS) {
                    repository.generate(emptyList<ChatMessage>(), prompt)
                        .catch { e ->
                            Log.e(TAG, "Generation error: ${e.message}")
                            generationError = e.message ?: "Unknown generation error"
                        }
                        .onCompletion { cause ->
                            if (userStopped) {
                                _uiState.value = PdfSummarizeUiState.Done
                                return@onCompletion
                            }
                            val err = generationError
                            when {
                                err != null ->
                                    _uiState.value = PdfSummarizeUiState.Error("Generation failed: $err")
                                cause != null ->
                                    _uiState.value = PdfSummarizeUiState.Error("Generation cancelled unexpectedly.")
                                _summaryText.value.isBlank() ->
                                    _uiState.value = PdfSummarizeUiState.Error("No summary was generated. Please try again.")
                                else -> {
                                    _uiState.value = PdfSummarizeUiState.Done
                                    autoSave()
                                }
                            }
                        }
                        .collect { token ->
                            if (!firstTokenReceived) {
                                firstTokenReceived = true
                                Log.i(TAG, "First token received")
                            }
                            _summaryText.value += token
                        }
                }

                if (timedOut == null && !userStopped) {
                    Log.e(TAG, "No first token within ${FIRST_TOKEN_TIMEOUT_MS}ms — prompt likely exceeded context window")
                    repository.stop()
                    _uiState.value = PdfSummarizeUiState.Error(
                        "Model did not respond. The document may be too large. Try a shorter PDF."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _uiState.value = PdfSummarizeUiState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    fun stop() {
        userStopped = true
        repository.stop()
        generationJob?.cancel()
        if (_uiState.value is PdfSummarizeUiState.Summarizing) {
            _uiState.value = PdfSummarizeUiState.Done
        }
    }

    fun reset() {
        stop()
        _summaryText.value = ""
        _extractionProgress.value = 0f
        _uiState.value = PdfSummarizeUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        repository.stop()
        repository.unload()
    }

    // ── Prompt construction ───────────────────────────────────────────────────

    private suspend fun autoSave() {
        val text = _summaryText.value
        if (text.isBlank()) return
        libraryRepo.save(EntryType.PDF_SUMMARY, text)
        _showSavedBanner.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(2_500)
            _showSavedBanner.value = false
        }
    }

    private fun buildSummaryPrompt(pdfText: String): String =
        "Summarize the following document in 5 concise bullet points.\n\n$pdfText"
}

// ── UI state ──────────────────────────────────────────────────────────────────

sealed class PdfSummarizeUiState {
    object Idle        : PdfSummarizeUiState()
    object Extracting  : PdfSummarizeUiState()
    object Summarizing : PdfSummarizeUiState()
    object Done        : PdfSummarizeUiState()
    data class Error(val message: String) : PdfSummarizeUiState()
}
