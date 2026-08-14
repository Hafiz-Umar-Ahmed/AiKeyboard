package com.example.aikeyboard.ui.theme.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aikeyboard.data.AiProvider
import com.example.aikeyboard.data.ApiKeyStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKeyStore: ApiKeyStore,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Provider Keys", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Add at least one API key to enable AI features (grammar, tone, chat). " +
                        "Keys are encrypted on this device and are only ever sent directly " +
                        "to the provider they belong to. If you add more than one, the " +
                        "keyboard automatically falls back to the next provider when one " +
                        "hits its free-tier limit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AiProvider.entries.forEach { provider ->
                ApiKeyField(provider = provider, apiKeyStore = apiKeyStore)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ApiKeyField(provider: AiProvider, apiKeyStore: ApiKeyStore) {
    var text by remember { mutableStateOf(apiKeyStore.getKey(provider) ?: "") }
    var visible by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(apiKeyStore.hasKey(provider)) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                provider.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (saved) {
                AssistChip(onClick = {}, label = { Text("Active") })
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("${provider.displayName} API key") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Hide key" else "Show key"
                    )
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    apiKeyStore.saveKey(provider, text)
                    saved = true
                },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = {
                    apiKeyStore.clearKey(provider)
                    text = ""
                    saved = false
                },
                enabled = text.isNotBlank() || saved
            ) {
                Text("Clear")
            }
        }
    }
}