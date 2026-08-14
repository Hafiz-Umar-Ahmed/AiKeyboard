package com.example.aikeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aikeyboard.data.ApiKeyStore
import com.example.aikeyboard.ui.theme.AiKeyboardTheme
import com.example.aikeyboard.ui.theme.screens.SettingsScreen
import com.example.aikeyboard.ui.theme.screens.SetupScreen
import androidx.core.content.edit

private enum class AppScreen { SETUP, SETTINGS }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKeyStore = ApiKeyStore(applicationContext)
        val sharedPrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            // Read from SharedPreferences, default to system preference if not set
            var isDarkTheme by remember {
                mutableStateOf(sharedPrefs.getBoolean("is_dark", systemDark))
            }

            // Pass the forced darkTheme state to your theme
            AiKeyboardTheme(darkTheme = isDarkTheme) {androidx.compose.material3.Surface(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var screen by remember { mutableStateOf(AppScreen.SETUP) }

                when (screen) {
                    AppScreen.SETUP -> SetupScreen(
                        isDarkTheme = !isDarkTheme,
                        onToggleTheme = {
                            isDarkTheme = !isDarkTheme
                            sharedPrefs.edit { putBoolean("is_dark", isDarkTheme) }
                            Log.d("AI keyboard", "is dark theme:$isDarkTheme")
                        },
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
}