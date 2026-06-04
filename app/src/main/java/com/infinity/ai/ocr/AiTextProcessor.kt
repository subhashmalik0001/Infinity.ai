package com.infinity.ai.ocr

import android.util.Log
import com.infinity.ai.ai.repository.AIRepository
import com.infinity.ai.ai.state.AIInferenceState
import com.infinity.ai.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AiTextProcessor
 *
 * Shared helper that runs a single prompt through AIRepository.generate()
 * with token-budget enforcement and first-token timeout.
 *
 * Used by OcrViewModel, ScreenshotExplainerViewModel, and QuizViewModel
 * so the safety logic is not duplicated.
 *
 * Caller supplies:
 *   - repository    : their own AIRepository instance
 *   - prompt        : already-built prompt string (caller enforces MAX_CHARS on input text)
 *   - outputFlow    : MutableStateFlow<String> to stream tokens into
 *   - onDone        : called on natural completion or user stop
 *   - onError       : called with message on any failure
 *   - userStopped   : lambda returning current userStopped flag
 */
object AiTextProcessor {

    private const val TAG              = "AiTextProcessor"
    private const val TIMEOUT_MS       = 60_000L
    // N_CTX=2048, MAX_TOKENS=512, overhead≈134 → 1402 tokens for content
    // At 4 chars/token for clean text → 5608 chars absolute max.
    // We cap at 800 to stay safe with noisy OCR output (same as PDF extractor).
    const val MAX_INPUT_CHARS          = 800

    /**
     * Build a token-safe prompt:
     *   prefix (the instruction) + "\n\n" + text (capped at MAX_INPUT_CHARS).
     * Estimated total prompt tokens ≈ (prefix.length + text.length) / 4 + 134 overhead.
     */
    fun buildPrompt(prefix: String, text: String): String {
        val safeText = if (text.length > MAX_INPUT_CHARS) text.take(MAX_INPUT_CHARS) else text
        val prompt   = "$prefix\n\n$safeText"
        Log.i(TAG, "Prompt: ${prompt.length} chars, est ~${prompt.length / 4 + 134} tokens")
        return prompt
    }

    /**
     * Stream [prompt] through [repository] into [outputFlow].
     * Calls [onDone] on completion (natural or user-stopped).
     * Calls [onError] with a user-readable message on any failure.
     */
    suspend fun stream(
        repository  : AIRepository,
        prompt      : String,
        outputFlow  : MutableStateFlow<String>,
        userStopped : () -> Boolean,
        onDone      : () -> Unit,
        onError     : (String) -> Unit
    ) {
        // Guard: don't start if engine is in a bad state
        when (val s = repository.aiState.value) {
            is AIInferenceState.Error   -> { onError("AI engine error. Please restart the app."); return }
            is AIInferenceState.Loading -> { onError("Model is still loading. Please wait and try again."); return }
            else                        -> Unit
        }

        var generationError: String? = null

        val timedOut = withTimeoutOrNull(TIMEOUT_MS) {
            repository.generate(emptyList<ChatMessage>(), prompt)
                .catch { e ->
                    Log.e(TAG, "Generation error: ${e.message}")
                    generationError = e.message ?: "Unknown error"
                }
                .onCompletion { cause ->
                    if (userStopped()) { onDone(); return@onCompletion }
                    val err = generationError
                    when {
                        err != null   -> onError("Generation failed: $err")
                        cause != null -> onError("Generation cancelled unexpectedly.")
                        outputFlow.value.isBlank() -> onError("No response generated. Please try again.")
                        else          -> onDone()
                    }
                }
                .collect { token -> outputFlow.value += token }
        }

        if (timedOut == null && !userStopped()) {
            Log.e(TAG, "No first token within ${TIMEOUT_MS}ms")
            repository.stop()
            onError("Model did not respond. Try with a smaller image or less text.")
        }
    }
}
