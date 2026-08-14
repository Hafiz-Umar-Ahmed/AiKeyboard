package com.example.aikeyboard.keyboard

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aikeyboard.R
import com.example.aikeyboard.service.KeyboardService
import com.example.aikeyboard.service.TextFieldAccessor

@Composable
fun AIKeyBoard() {
    val context = LocalContext.current
    val ime = context as KeyboardService
    val viewModel: KeyboardViewModel = viewModel(factory = remember { KeyboardViewModelFactory(context) })
    val uiState by viewModel.uiState.collectAsState()

    // QWERTY Layout Definitions
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Set the overall keyboard background to a subtle tone
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        // --- AI TOOLBAR ---
        AIToolBar(
            activePanel = uiState.panel,
            onAssistantClick = { viewModel.toggleChat() },
            onGrammarClick = { viewModel.toggleGrammarCheck() },
            onToneClick = { viewModel.toggleToneMenu() },
            onContinueClick = { viewModel.toggleContinue() }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- KEY AREA: either the QWERTY rows, or an AI panel in their place ---
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
                        viewModel.insertTonePreview { newText ->
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
                        viewModel.insertGrammarFix { newText ->
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
                        viewModel.insertContinuation { newText ->
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
                            viewModel.insertChatMessage(message) { newText ->
                                TextFieldAccessor.replaceWorkingText(ime.currentInputConnection, newText)
                            }
                        },
                        onClose = { viewModel.closePanel() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Reuse the same QWERTY rows to type the chat prompt — key
                    // presses are redirected into the chat draft instead of the
                    // real input field while this panel is open.
                    StandardKeyRows(
                        row1 = row1,
                        row2 = row2,
                        row3 = row3,
                        onKeyAction = { action -> handleChatKeyAction(action, viewModel) }
                    )
                }
            }

            AiPanelState.Hidden -> {
                StandardKeyRows(row1 = row1, row2 = row2, row3 = row3)
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
 */
@Composable
private fun StandardKeyRows(
    row1: List<String>,
    row2: List<String>,
    row3: List<String>,
    onKeyAction: ((KeyAction) -> Unit)? = null
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
        KeyboardTextKey(text = "⇧", weight = 1.3f, isSpecial = true, onKeyAction = onKeyAction)

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
            vibrateOnClick = true, soundOnClick = true, keyPadding = 3,
            keyHeight = 50f, keyWidth = 40f, keyBorderWidth = 0f, keyRadius = 8f,
            modifier = Modifier.weight(1.3f),
            isSpecial = true,
            onKeyAction = onKeyAction
        )
    }

    // --- ROW 4 (Symbols, Comma, Space, Dot, Enter) ---
    Row(modifier = Modifier.fillMaxWidth()) {
        KeyboardTextKey(text = "?123", weight = 1.2f, isSpecial = true, onKeyAction = onKeyAction)
        KeyboardTextKey(text = ",", weight = 1f, onKeyAction = onKeyAction)

        AIKeyboardKey(
            key = KeyItem(
                keyAction = KeyAction.CommitText(text = " "),
                keyType = KeyType.KeyText(value = " ", description = R.string.description_not_available)
            ),
            vibrateOnClick = true, soundOnClick = true, keyPadding = 3,
            keyHeight = 50f, keyWidth = 60f, keyBorderWidth = 0f, keyRadius = 8f,
            modifier = Modifier.weight(4f),
            onKeyAction = onKeyAction
        )

        KeyboardTextKey(text = ".", weight = 1f, onKeyAction = onKeyAction)

        AIKeyboardKey(
            key = KeyItem(
                keyAction = KeyAction.Enter,
                keyType = KeyType.KeyIcon(
                    icon = ImageVector.vectorResource(R.drawable.ic_enter),
                    description = R.string.enter
                )
            ),
            vibrateOnClick = true, soundOnClick = true, keyPadding = 3,
            keyHeight = 50f, keyWidth = 40f, keyBorderWidth = 0f, keyRadius = 8f,
            modifier = Modifier.weight(1.2f),
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
    onKeyAction: ((KeyAction) -> Unit)? = null
) {
    AIKeyboardKey(
        key = KeyItem(
            keyAction = KeyAction.CommitText(text = text),
            keyType = KeyType.KeyText(value = text)
        ),
        vibrateOnClick = true,
        soundOnClick = true,
        keyPadding = 3,
        keyHeight = 50f,
        keyWidth = 32f,
        keyBorderWidth = 0f, // Removed border for native look
        keyRadius = 8f,
        modifier = Modifier.weight(weight),
        isSpecial = isSpecial,
        onKeyAction = onKeyAction
    )
}

// UI for the AI feature row above the keyboard. Whichever icon's panel is
// currently open is tinted primary so there's a clear sense of active state.
@Composable
fun AIToolBar(
    activePanel: AiPanelState,
    onAssistantClick: () -> Unit,
    onGrammarClick: () -> Unit,
    onToneClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            // Toolbar gets a slightly different background shade
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
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
    }
}