package com.example.aikeyboard.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GenerateRowWithKeyTexts(
    itemsText: List<String>,
    rightKey: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (item in itemsText) {
            AIKeyboardKey(
                key = KeyItem(
                    keyAction = KeyAction.CommitText(text = item),
                    keyType = KeyType.KeyText(value = item)
                ),
                vibrateOnClick = true,
                soundOnClick = true,
                keyPadding = 2,
                keyHeight = 54f,
                keyWidth = 60f,
                keyBorderWidth = 1f,
                keyRadius = 5f,
                modifier = Modifier.weight(1f)
            )
        }
        rightKey()
    }
}