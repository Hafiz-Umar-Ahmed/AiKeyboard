package com.example.aikeyboard.keyboard

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aikeyboard.R
import com.example.aikeyboard.service.KeyboardService
import com.example.aikeyboard.service.TextFieldAccessor
import com.example.aikeyboard.service.performKeyAction

// Shared key styling — rounded-square ("squircle") corners and a bit of
// breathing room between keys, matching the design reference.
private const val KEY_RADIUS = 14f
private const val KEY_PADDING = 4

@Composable
fun AIKeyBoard() {
    val context = LocalContext.current
    val ime = context as KeyboardService
    val viewModel: KeyboardViewModel = viewModel(factory = remember { KeyboardViewModelFactory(context) })
    val uiState by viewModel.uiState.collectAsState()

    // Emoji picker is plain UI state, not AI-related, so it lives here rather
    // than in KeyboardViewModel. Only relevant while the letter keyboard (no
    // AI panel) is showing.
    var showEmojiPanel by remember { mutableStateOf(false) }

    // QWERTY Layout Definitions
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Set the overall keyboard background to a subtle tone
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        // --- AI TOOLBAR ---
        AIToolBar(
            activePanel = uiState.panel,
            canUndo = uiState.lastInsertSnapshot != null,
            onAssistantClick = { viewModel.toggleChat() },
            onGrammarClick = { viewModel.toggleGrammarCheck() },
            onToneClick = { viewModel.toggleToneMenu() },
            onContinueClick = { viewModel.toggleContinue() },
            onUndoClick = {
                viewModel.undoLastInsert { snapshot ->
                    TextFieldAccessor.replaceEntireField(ime.currentInputConnection, snapshot)
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- KEY AREA: either the QWERTY rows, an AI panel, or the emoji picker ---
        when (uiState.panel) {
            AiPanelState.ToneMenu -> {
                ToneMenuPanel(
                    isProcessing = uiState.isProcessing,
                    statusMessage = uiState.statusMessage,
                    selectedTone = uiState.selectedTone,
                    previewText = uiState.tonePreviewText,
                    provider = uiState.lastUsedProvider,
                    onToneSelected = { tone ->
                        viewModel.requestTonePreview(tone) {
                            TextFieldAccessor.getWorkingText(ime.currentInputConnection)
                        }
                    },
                    onInsert = {
                        val snapshot = TextFieldAccessor.getFullText(ime.currentInputConnection)
                        viewModel.insertTonePreview(snapshot) { newText ->
                            TextFieldAccessor.replaceWorkingText(ime.currentInputConnection, newText)
                        }
                    },
                    onClose = { viewModel.closePanel() }
                )
            }

            AiPanelState.GrammarCheck -> {
                // Nothing to choose here (unlike Tone) — run automatically the
                // first time this panel is opened in a session.
                LaunchedEffect(Unit) {
                    if (uiState.grammarPreviewText == null && !uiState.isProcessing) {
                        viewModel.requestGrammarFix {
                            TextFieldAccessor.getWorkingText(ime.currentInputConnection)
                        }
                    }
                }
                GrammarCheckPanel(
                    isProcessing = uiState.isProcessing,
                    statusMessage = uiState.statusMessage,
                    sourceText = uiState.grammarSourceText,
                    previewText = uiState.grammarPreviewText,
                    provider = uiState.lastUsedProvider,
                    onRetry = {
                        viewModel.requestGrammarFix {
                            TextFieldAccessor.getWorkingText(ime.currentInputConnection)
                        }
                    },
                    onInsert = {
                        val snapshot = TextFieldAccessor.getFullText(ime.currentInputConnection)
                        viewModel.insertGrammarFix(snapshot) { newText ->
                            TextFieldAccessor.replaceWorkingText(ime.currentInputConnection, newText)
                        }
                    },
                    onClose = { viewModel.closePanel() }
                )
            }

            AiPanelState.Continue -> {
                LaunchedEffect(Unit) {
                    if (uiState.continuePreviewText == null && !uiState.isProcessing) {
                        viewModel.requestContinuation {
                            TextFieldAccessor.getWorkingText(ime.currentInputConnection)
                        }
                    }
                }
                ContinuePanel(
                    isProcessing = uiState.isProcessing,
                    statusMessage = uiState.statusMessage,
                    previewText = uiState.continuePreviewText,
                    provider = uiState.lastUsedProvider,
                    onRetry = {
                        viewModel.requestContinuation {
                            TextFieldAccessor.getWorkingText(ime.currentInputConnection)
                        }
                    },
                    onInsert = {
                        val snapshot = TextFieldAccessor.getFullText(ime.currentInputConnection)
                        viewModel.insertContinuation(snapshot) { newText ->
                            TextFieldAccessor.insertAtCursor(ime.currentInputConnection, newText)
                        }
                    },
                    onClose = { viewModel.closePanel() }
                )
            }

            AiPanelState.Chat -> {
                Column {
                    ChatPanel(
                        messages = uiState.chatMessages,
                        draft = uiState.chatDraft,
                        isSending = uiState.isChatSending,
                        statusMessage = uiState.statusMessage,
                        onSend = { viewModel.sendChatMessage() },
                        onInsert = { message ->
                            val snapshot = TextFieldAccessor.getFullText(ime.currentInputConnection)
                            viewModel.insertChatMessage(message, snapshot) { newText ->
                                TextFieldAccessor.replaceWorkingText(ime.currentInputConnection, newText)
                            }
                        },
                        onClose = { viewModel.closePanel() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Reuse the same QWERTY rows to type the chat prompt — key
                    // presses are redirected into the chat draft instead of the
                    // real input field while this panel is open. Emoji picker
                    // isn't available in this context, so its slot is disabled.
                    StandardKeyRows(
                        row1 = row1,
                        row2 = row2,
                        row3 = row3,
                        onKeyAction = { action -> handleChatKeyAction(action, viewModel) },
                        onEmojiClick = null
                    )
                }
            }

            AiPanelState.Hidden -> {
                if (showEmojiPanel) {
                    EmojiPanel(
                        onEmojiSelected = { emoji ->
                            TextFieldAccessor.insertAtCursor(ime.currentInputConnection, emoji)
                        },
                        onBackspace = { performKeyAction(KeyAction.Delete, ime) },
                        onBackToLetters = { showEmojiPanel = false }
                    )
                } else {
                    StandardKeyRows(
                        row1 = row1,
                        row2 = row2,
                        row3 = row3,
                        onEmojiClick = { showEmojiPanel = true }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) 40.dp else 4.dp
            )
        )
    }
}

/** Routes a key press typed on the reused QWERTY rows into the chat draft instead of the real field. */
private fun handleChatKeyAction(action: KeyAction, viewModel: KeyboardViewModel) {
    when (action) {
        is KeyAction.CommitText -> viewModel.appendChatDraft(action.text)
        KeyAction.Delete -> viewModel.backspaceChatDraft()
        KeyAction.Enter -> viewModel.sendChatMessage()
        KeyAction.Done -> Unit
    }
}

/**
 * The standard QWERTY rows, extracted so the AI panels can swap in for this
 * block. [onKeyAction], if provided, redirects every key press away from the
 * real input field (used by the Chat panel to type into its draft instead).
 * [onEmojiClick] opens the emoji picker; pass null to show it disabled (used
 * in contexts, like Chat, where the emoji picker isn't wired up).
 */
@Composable
private fun StandardKeyRows(
    row1: List<String>,
    row2: List<String>,
    row3: List<String>,
    onKeyAction: ((KeyAction) -> Unit)? = null,
    onEmojiClick: (() -> Unit)? = null
) {
    // --- ROW 1 ---
    Row(modifier = Modifier.fillMaxWidth()) {
        row1.forEach { item ->
            KeyboardTextKey(text = item, weight = 1f, onKeyAction = onKeyAction)
        }
    }

    // --- ROW 2 (Slightly indented like a standard keyboard) ---
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        row2.forEach { item ->
            KeyboardTextKey(text = item, weight = 1f, onKeyAction = onKeyAction)
        }
    }

    // --- ROW 3 (Shift + Z...M + Backspace) ---
    Row(modifier = Modifier.fillMaxWidth()) {
        KeyboardTextKey(text = "⇧", weight = 1.3f, isSpecial = true, accentColor = true, onKeyAction = onKeyAction)

        row3.forEach { item ->
            KeyboardTextKey(text = item, weight = 1f, onKeyAction = onKeyAction)
        }

        AIKeyboardKey(
            key = KeyItem(
                keyAction = KeyAction.Delete,
                keyType = KeyType.KeyIcon(
                    icon = ImageVector.vectorResource(R.drawable.ic_delete_text),
                    description = R.string.clear
                )
            ),
            vibrateOnClick = true, soundOnClick = true, keyPadding = KEY_PADDING,
            keyHeight = 50f, keyWidth = 40f, keyBorderWidth = 0f, keyRadius = KEY_RADIUS,
            modifier = Modifier.weight(1.3f),
            isSpecial = true,
            accentColor = true,
            onKeyAction = onKeyAction
        )
    }

    // --- ROW 4 (123, emoji, comma, space, dot, mic, Return) ---
    Row(modifier = Modifier.fillMaxWidth()) {
        KeyboardTextKey(text = "?123", weight = 1.2f, isSpecial = true, onKeyAction = onKeyAction)

        AccessoryIconKey(
            icon = Icons.Default.EmojiEmotions,
            contentDescription = "Emoji",
            weight = 0.9f,
            onClick = onEmojiClick
        )

        KeyboardTextKey(text = ",", weight = 1f, onKeyAction = onKeyAction)

        // Space bar — commits an actual space character but is *labeled*
        // "space" (matching the reference design) rather than showing nothing.
        AIKeyboardKey(
            key = KeyItem(
                keyAction = KeyAction.CommitText(text = " "),
                keyType = KeyType.KeyText(value = "space")
            ),
            vibrateOnClick = true, soundOnClick = true, keyPadding = KEY_PADDING,
            keyHeight = 50f, keyWidth = 60f, keyBorderWidth = 0f, keyRadius = KEY_RADIUS,
            modifier = Modifier.weight(4f),
            isSpecial = true,
            onKeyAction = onKeyAction
        )

        KeyboardTextKey(text = ".", weight = 1f, onKeyAction = onKeyAction)

        AccessoryIconKey(
            icon = Icons.Default.Mic,
            contentDescription = "Voice input (coming soon)",
            weight = 0.9f,
            onClick = null // TODO: wire up SpeechRecognizer in a future pass
        )

        // Return/Enter — labeled with text (matching the reference design)
        // instead of an icon; the action itself (KeyAction.Enter) is unchanged.
        AIKeyboardKey(
            key = KeyItem(
                keyAction = KeyAction.Enter,
                keyType = KeyType.KeyText(value = "Return")
            ),
            vibrateOnClick = true, soundOnClick = true, keyPadding = KEY_PADDING,
            keyHeight = 50f, keyWidth = 56f, keyBorderWidth = 0f, keyRadius = KEY_RADIUS,
            modifier = Modifier.weight(1.6f),
            isSpecial = true,
            onKeyAction = onKeyAction
        )
    }
}

// Helper composable to render standard text keys cleanly
@Composable
fun RowScope.KeyboardTextKey(
    text: String,
    weight: Float,
    isSpecial: Boolean = false,
    accentColor: Boolean = false,
    onKeyAction: ((KeyAction) -> Unit)? = null
) {
    AIKeyboardKey(
        key = KeyItem(
            keyAction = KeyAction.CommitText(text = text),
            keyType = KeyType.KeyText(value = text)
        ),
        vibrateOnClick = true,
        soundOnClick = true,
        keyPadding = KEY_PADDING,
        keyHeight = 50f,
        keyWidth = 32f,
        keyBorderWidth = 0f,
        keyRadius = KEY_RADIUS,
        modifier = Modifier.weight(weight),
        isSpecial = isSpecial,
        accentColor = accentColor,
        onKeyAction = onKeyAction
    )
}

/**
 * A small icon-only accessory key (emoji toggle, mic) styled like the other
 * keys. When [onClick] is null, renders a dimmed, non-interactive version so
 * the row layout stays stable across contexts where the action isn't wired
 * up yet (e.g. emoji inside the Chat panel's reused rows).
 */
@Composable
private fun RowScope.AccessoryIconKey(
    icon: ImageVector,
    contentDescription: String,
    weight: Float,
    onClick: (() -> Unit)?
) {
    val view = LocalView.current
    val enabled = onClick != null

    Box(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .padding(KEY_PADDING.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(KEY_RADIUS.dp))
            .clip(RoundedCornerShape(KEY_RADIUS.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (enabled) {
                    Modifier.clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick?.invoke()
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

// UI for the AI feature row above the keyboard. Whichever icon's panel is
// currently open is tinted primary so there's a clear sense of active state.
// The 5th icon (undo) restores the field to how it was before the most
// recent AI insert, dimmed/disabled when there's nothing to undo.
@Composable
fun AIToolBar(
    activePanel: AiPanelState,
    canUndo: Boolean,
    onAssistantClick: () -> Unit,
    onGrammarClick: () -> Unit,
    onToneClick: () -> Unit,
    onContinueClick: () -> Unit,
    onUndoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            // Toolbar gets a slightly different background shade
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onAssistantClick) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "AI Assistant",
                tint = if (activePanel == AiPanelState.Chat) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IconButton(onClick = onGrammarClick) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Fix Grammar",
                tint = if (activePanel == AiPanelState.GrammarCheck) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        IconButton(onClick = onToneClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change Tone",
                tint = if (activePanel == AiPanelState.ToneMenu) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        IconButton(onClick = onContinueClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Continue Writing",
                tint = if (activePanel == AiPanelState.Continue) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        IconButton(onClick = onUndoClick, enabled = canUndo) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo last AI insert",
                tint = if (canUndo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
    }
}