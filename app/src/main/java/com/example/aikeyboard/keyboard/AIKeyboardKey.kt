package com.example.aikeyboard.keyboard

import android.annotation.SuppressLint
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.aikeyboard.R
import com.example.aikeyboard.service.KeyboardService
import com.example.aikeyboard.service.performKeyAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val MINUTE_IN_MILLISECONDS = 60000L
private const val REPEATABLE_ACTION_TIME_DELAY = 60L

@SuppressLint("ServiceCast")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AIKeyboardKey(
    key: KeyItem,
    keyPadding: Int,
    keyHeight: Float,
    keyWidth: Float,
    keyBorderWidth: Float, // Kept for compatibility, but we will pass 0f for a clean look
    keyRadius: Float,
    vibrateOnClick: Boolean,
    soundOnClick: Boolean,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false, // Styles special keys (Backspace, Enter, Toolbar) differently
    // When non-null, key presses go here instead of the real input field — used
    // by the Chat panel to type into its local draft while it's open.
    onKeyAction: ((KeyAction) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val context = LocalContext.current
    val ime = context as KeyboardService

    val coroutineScope = rememberCoroutineScope()
    val longClickPressed = remember { mutableStateOf(false) }

    val view = LocalView.current
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

    // CLEAN UI UPDATE: Use Material 3 surface colors for a minimal, native look
    val backgroundColor = if (!isPressed) {
        if (isSpecial) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val keyInfoColor = if (isSpecial) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    fun soundAndVibrate() {
        if (vibrateOnClick) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (soundOnClick) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, .1f)
        }
    }

    fun dispatchAction(action: KeyAction) {
        if (onKeyAction != null) {
            onKeyAction(action)
        } else {
            performKeyAction(action = action, ime = ime)
        }
    }

    fun onLongClick() {
        if (key.keyAction != KeyAction.Done) {
            longClickPressed.value = true
            coroutineScope.launch(Dispatchers.IO) {
                withTimeout(MINUTE_IN_MILLISECONDS) {
                    while (true) {
                        withContext(Dispatchers.Main) { dispatchAction(key.keyAction) }
                        delay(REPEATABLE_ACTION_TIME_DELAY)
                    }
                }
            }
        } else {
            dispatchAction(key.keyAction)
        }
        soundAndVibrate()
    }

    LaunchedEffect(key1 = isPressed, key2 = longClickPressed) {
        if (isPressed) {
            soundAndVibrate()
        } else {
            if (longClickPressed.value) {
                coroutineScope.coroutineContext.cancelChildren()
                longClickPressed.value = false
            }
        }
    }

    val keyboardKeyModifier = modifier
        .height(keyHeight.dp)
        .defaultMinSize(minWidth = keyWidth.dp)
        .padding(keyPadding.dp)
        // Add a tiny shadow for that premium native keyboard feel
        .shadow(elevation = 1.dp, shape = RoundedCornerShape(keyRadius.dp))
        .clip(RoundedCornerShape(keyRadius.dp))
        .background(color = backgroundColor)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { dispatchAction(key.keyAction) },
            onLongClick = { onLongClick() }
        )

    Box(modifier = keyboardKeyModifier) {
        when (val type = key.keyType) {
            is KeyType.KeyText -> {
                Text(
                    text = type.value,
                    style = MaterialTheme.typography.titleMedium,
                    color = keyInfoColor,
                    modifier = Modifier.align(Alignment.Center),
                )
                if (type.showDescription) {
                    Text(
                        text = "(${stringResource(type.description!!)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Serif,
                        color = keyInfoColor,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                    )
                }
            }
            is KeyType.KeyIcon -> {
                Icon(
                    imageVector = type.icon,
                    contentDescription = stringResource(type.description ?: R.string.description_not_available),
                    tint = keyInfoColor,
                    modifier = Modifier.align(Alignment.Center).padding(4.dp)
                )
            }
        }
    }
}