package com.example.aikeyboard.keyboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aikeyboard.ai.AiRequest
import com.example.aikeyboard.ai.AiResult
import com.example.aikeyboard.ai.AiRouter
import com.example.aikeyboard.ai.TokenEstimator
import com.example.aikeyboard.data.AiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tones the user can rewrite their text into. Add more here as needed. */
enum class Tone(val label: String, val instruction: String) {
    PROFESSIONAL("Professional", "Rewrite this in a professional, polished tone suitable for work correspondence."),
    FRIENDLY("Friendly", "Rewrite this in a warm, friendly, casual tone."),
    CONCISE("Concise", "Rewrite this to be as short and clear as possible without losing its meaning."),
    CLARIFYING("Clarifying", "Rewrite this to maximize clarity and readability. Untangle any confusing phrasing, fix grammar, and make the core message unmistakable."),
    CONFIDENT("Confident", "Rewrite this in a direct, confident tone."),
    EMPATHETIC("Empathetic", "Rewrite this in a gentle, empathetic, understanding tone."),
    FORMAL("Formal", "Rewrite this in a formal, respectful tone suitable for official communication."),
    WITTY("Witty", "Rewrite this with a light, witty sense of humor, while keeping the original meaning clear.")
}

/** Which overlay, if any, is currently shown in place of the QWERTY rows. */
sealed class AiPanelState {
    data object Hidden : AiPanelState()
    data object ToneMenu : AiPanelState()
    data object Chat : AiPanelState()
    data object GrammarCheck : AiPanelState()
    data object Continue : AiPanelState()
}

data class KeyboardUiState(
    val panel: AiPanelState = AiPanelState.Hidden,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val lastUsedProvider: AiProvider? = null,

    // --- Tone feature ---
    val toneSourceText: String? = null,
    val tonePreviewText: String? = null,
    val selectedTone: Tone? = null,

    // --- Grammar feature ---
    val grammarSourceText: String? = null,
    val grammarPreviewText: String? = null,

    // --- Continue-writing feature ---
    val continueSourceText: String? = null,
    val continuePreviewText: String? = null,

    // --- Chat feature ---
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatDraft: String = "",
    val isChatSending: Boolean = false
)

class KeyboardViewModel(context: Context) : ViewModel() {

    private val router = AiRouter(context.applicationContext)

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    // ---------- Panel visibility ----------
    fun toggleToneMenu() = togglePanel(AiPanelState.ToneMenu)
    fun toggleChat() = togglePanel(AiPanelState.Chat)
    fun toggleGrammarCheck() = togglePanel(AiPanelState.GrammarCheck)
    fun toggleContinue() = togglePanel(AiPanelState.Continue)

    private fun togglePanel(target: AiPanelState) {
        val opening = _uiState.value.panel != target
        _uiState.value = _uiState.value.copy(
            panel = if (opening) target else AiPanelState.Hidden,
            statusMessage = null
        )
    }

    fun closePanel() {
        _uiState.value = _uiState.value.copy(panel = AiPanelState.Hidden, statusMessage = null)
    }

    // ---------- Tone feature ----------
    fun requestTonePreview(tone: Tone, liveFieldText: () -> String?) {
        val source = _uiState.value.toneSourceText ?: liveFieldText()
        if (source.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Nothing to rewrite yet — type something first.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            statusMessage = null,
            selectedTone = tone,
            toneSourceText = source
        )

        viewModelScope.launch {
            val sysInstruction = """
                You are an elite copywriter and text editor operating directly inside a user's mobile keyboard.
                Your ONLY purpose is to rewrite the user's input text to match the requested tone.
                
                CRITICAL DIRECTIVES:
                1. Output ONLY the final rewritten text. 
                2. NO preamble, NO pleasantries, NO conversational filler (e.g., do not say "Here is the rewrite:").
                3. DO NOT wrap the output in quotation marks or markdown blocks.
                4. Preserve the original facts, intent, and core meaning entirely. Do not hallucinate new information.
            """.trimIndent()

            val prompt = buildString {
                append("Task: ")
                append(tone.instruction)
                append("\n\nOriginal Text:\n")
                append(source)
            }

            val request = AiRequest(
                prompt = prompt,
                systemInstruction = sysInstruction,
                temperature = 0.2, // Strict accuracy
                maxOutputTokens = TokenEstimator.getChatMaxTokens()
            )

            when (val result = router.generate(request)) {
                is AiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        tonePreviewText = result.text,
                        lastUsedProvider = result.provider
                    )
                }
                is AiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = result.error.message
                    )
                }
            }
        }
    }

    fun insertTonePreview(onInsert: (String) -> Unit) {
        val text = _uiState.value.tonePreviewText ?: return
        onInsert(text)
        _uiState.value = _uiState.value.copy(panel = AiPanelState.Hidden)
    }

    // ---------- Grammar feature ----------
    fun requestGrammarFix(liveFieldText: () -> String?) {
        val source = _uiState.value.grammarSourceText ?: liveFieldText()
        if (source.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Nothing to check yet — type something first.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            statusMessage = null,
            grammarSourceText = source,
            grammarPreviewText = null
        )

        viewModelScope.launch {
            val sysInstruction = """
                You are a professional proofreader built into a mobile keyboard.
                Your task is to fix any spelling, grammar, and punctuation mistakes.
                
                CRITICAL DIRECTIVES:
                1. Preserve the original meaning, tone, and formatting exactly.
                2. If there are no mistakes, return the text completely unchanged.
                3. DO NOT output preambles, explanations, quotes, or markdown. Output ONLY the corrected text.
            """.trimIndent()

            val prompt = "Text to check:\n$source"

            val request = AiRequest(
                prompt = prompt,
                systemInstruction = sysInstruction,
                temperature = 0.1, // Very low temperature to prevent unprompted creative changes
                maxOutputTokens = TokenEstimator.getChatMaxTokens()
            )

            when (val result = router.generate(request)) {
                is AiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        grammarPreviewText = result.text,
                        lastUsedProvider = result.provider
                    )
                }
                is AiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = result.error.message
                    )
                }
            }
        }
    }

    fun insertGrammarFix(onInsert: (String) -> Unit) {
        val text = _uiState.value.grammarPreviewText ?: return
        onInsert(text)
        _uiState.value = _uiState.value.copy(panel = AiPanelState.Hidden)
    }

    // ---------- Continue-writing feature ----------
    fun requestContinuation(liveFieldText: () -> String?) {
        val source = _uiState.value.continueSourceText ?: liveFieldText()
        if (source.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Type something first so I know how to continue.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            statusMessage = null,
            continueSourceText = source,
            continuePreviewText = null
        )

        viewModelScope.launch {
            val sysInstruction = """
                You are an AI co-writer embedded in a mobile keyboard.
                Your task is to continue the user's text naturally.
                
                CRITICAL DIRECTIVES:
                1. Write roughly one to three sentences that flow smoothly from where the text leaves off.
                2. Match the exact tone, style, and language of the original text.
                3. ONLY return the continuation itself. Do NOT repeat the original text.
                4. NO preamble, NO extra commentary, NO quotes, NO markdown formatting.
            """.trimIndent()

            val prompt = "Text so far:\n$source"

            val request = AiRequest(
                prompt = prompt,
                systemInstruction = sysInstruction,
                temperature = 0.8, // Slightly higher for creativity in continuing the thought
                maxOutputTokens = TokenEstimator.getChatMaxTokens()
            )

            when (val result = router.generate(request)) {
                is AiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        continuePreviewText = result.text,
                        lastUsedProvider = result.provider
                    )
                }
                is AiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        statusMessage = result.error.message
                    )
                }
            }
        }
    }

    fun insertContinuation(onInsert: (String) -> Unit) {
        val text = _uiState.value.continuePreviewText ?: return
        val source = _uiState.value.continueSourceText.orEmpty()
        val needsLeadingSpace = source.isNotEmpty() &&
                !source.last().isWhitespace() &&
                text.isNotEmpty() &&
                !text.first().isWhitespace()

        onInsert(if (needsLeadingSpace) " $text" else text)
        _uiState.value = _uiState.value.copy(panel = AiPanelState.Hidden)
    }

    // ---------- Chat feature ----------
    fun appendChatDraft(text: String) {
        _uiState.value = _uiState.value.copy(chatDraft = _uiState.value.chatDraft + text)
    }

    fun backspaceChatDraft() {
        val current = _uiState.value.chatDraft
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(chatDraft = current.dropLast(1))
        }
    }

    fun sendChatMessage() {
        val draft = _uiState.value.chatDraft.trim()
        if (draft.isBlank() || _uiState.value.isChatSending) return

        val userMessage = ChatMessage(role = ChatMessage.Role.USER, text = draft)
        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + userMessage,
            chatDraft = "",
            isChatSending = true,
            statusMessage = null
        )

        viewModelScope.launch {
            val transcript = buildString {
                _uiState.value.chatMessages.forEach { message ->
                    append(if (message.role == ChatMessage.Role.USER) "User: " else "Assistant: ")
                    append(message.text)
                    append("\n")
                }
            }

            val sysInstruction = """
                You are a highly efficient AI assistant integrated directly into a mobile keyboard. 
                
                CRITICAL DIRECTIVES:
                1. Provide immediate, concise answers optimized for small mobile screens.
                2. DO NOT use markdown formatting (like **bolding**, italics, or headers) unless specifically asked, because your output will be pasted as raw text into other apps.
                3. Eliminate all conversational filler (e.g., do not say "Here is your answer").
                4. If the user asks for code or data, provide just the raw data so they can insert it seamlessly.
            """.trimIndent()

            val request = AiRequest(
                prompt = transcript,
                systemInstruction = sysInstruction,
                temperature = 0.7,
                maxOutputTokens = TokenEstimator.getChatMaxTokens()
            )

            when (val result = router.generate(request)) {
                is AiResult.Success -> {
                    val assistantMessage = ChatMessage(role = ChatMessage.Role.ASSISTANT, text = result.text)
                    _uiState.value = _uiState.value.copy(
                        chatMessages = _uiState.value.chatMessages + assistantMessage,
                        isChatSending = false,
                        lastUsedProvider = result.provider
                    )
                }
                is AiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isChatSending = false,
                        statusMessage = result.error.message
                    )
                }
            }
        }
    }

    fun insertChatMessage(message: ChatMessage, onInsert: (String) -> Unit) {
        onInsert(message.text)
        _uiState.value = _uiState.value.copy(panel = AiPanelState.Hidden)
    }
}

class KeyboardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return KeyboardViewModel(context) as T
    }
}