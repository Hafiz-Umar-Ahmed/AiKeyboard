package com.example.aikeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aikeyboard.data.ApiKeyStore
import com.example.aikeyboard.ui.theme.AiKeyboardTheme
import com.example.aikeyboard.ui.theme.screens.SettingsScreen
import com.example.aikeyboard.ui.theme.screens.SetupScreen

private enum class AppScreen { SETUP, SETTINGS }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // One ApiKeyStore instance for the whole activity — EncryptedSharedPreferences
        // construction isn't free, so we don't want to recreate it on every recompose.
        val apiKeyStore = ApiKeyStore(applicationContext)

        setContent {
            AiKeyboardTheme {
                var screen by remember { mutableStateOf(AppScreen.SETUP) }

                when (screen) {
                    AppScreen.SETUP -> SetupScreen(
                        onEnableKeyboard = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        onSelectKeyboard = {
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        },
                        onOpenSettings = { screen = AppScreen.SETTINGS }
                    )

                    AppScreen.SETTINGS -> SettingsScreen(
                        apiKeyStore = apiKeyStore,
                        onBack = { screen = AppScreen.SETUP }
                    )
                }
            }
        }
    }
}