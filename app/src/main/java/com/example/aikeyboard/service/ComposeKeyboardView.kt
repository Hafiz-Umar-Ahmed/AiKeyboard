package com.example.aikeyboard.service

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.example.aikeyboard.keyboard.AIKeyBoard
import com.example.aikeyboard.ui.theme.AiKeyboardTheme

class ComposeKeyboardView(
    context: Context
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {

        val systemDark = isSystemInDarkTheme()

        val sharedPrefs = remember {
            context.getSharedPreferences(
                "theme_prefs",
                Context.MODE_PRIVATE
            )
        }

        var isDarkTheme by remember {
            mutableStateOf(
                sharedPrefs.getBoolean(
                    "is_dark",
                    systemDark
                )
            )
        }

        DisposableEffect(sharedPrefs) {

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->

                    if (key == "is_dark") {

                        isDarkTheme = sharedPrefs.getBoolean(
                            "is_dark",
                            systemDark
                        )
                    }
                }

            sharedPrefs.registerOnSharedPreferenceChangeListener(listener)

            onDispose {
                sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        AiKeyboardTheme(
            darkTheme = isDarkTheme
        ) {
            AIKeyBoard()
        }
    }
}