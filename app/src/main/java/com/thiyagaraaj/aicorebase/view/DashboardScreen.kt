package com.thiyagaraaj.aicorebase.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiyagaraaj.aicorebase.model.GeminiNanoViewModel
import com.thiyagaraaj.aicorebase.model.VoiceAiViewModel

@Composable
fun DashboardScreen() {
    var showScreen by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if ( showScreen == 0) {
            Button(
                onClick = {
                    showScreen = 1
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Text Based")
            }

            // Generate Button
            Button(
                onClick = {
                    showScreen = 2
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voice Based")
            }
        }
        else if ( showScreen == 1) {
            TextAIView(
                onBack = { value ->
                    showScreen = value
                }
            )
        }
        else if ( showScreen ==  2) {
            VoiceAIView(
                onBack = { value ->
                    showScreen = value
                }
            )
        }
    }
}

@Composable
fun TextAIView(
    onBack: (Int) -> Unit,
    viewModel: GeminiNanoViewModel = viewModel()
){
    var inputText by remember { mutableStateOf("") }

    val outputText by viewModel.output.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Text - Gemini Nano AI",
            style = MaterialTheme.typography.titleLarge
        )
        Button(
            onClick = {
                onBack(0)
            },
            modifier = Modifier.padding(0.dp),
        ) {
            Text("Back")
        }

        // Input Box
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter prompt") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // Generate Button
        Button(
            onClick = { viewModel.generateText(inputText) },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Run On-Device")
            }
        }

        // Output Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = outputText,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
                    .fillMaxSize(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun VoiceAIView(
    onBack: (Int) -> Unit,
    viewModel: VoiceAiViewModel = viewModel()
) {
    val context = LocalContext.current
    val statusText by viewModel.statusText.collectAsState()
    val userSpeech by viewModel.userSpeech.collectAsState()
    val geminiReply by viewModel.geminiReply.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Voice + Gemini Nano",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Speak, then Gemini Nano replies on-screen and aloud.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = {
                onBack(0)
            },
            modifier = Modifier.padding(0.dp),
        ) {
            Text("Back")
        }

        if (!hasMicPermission) {
            Text(
                text = "Microphone permission is required for speech recognition.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Microphone Permission")
            }
        }

        // Single control button
        Button(
            onClick = {
                if (!hasMicPermission) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@Button
                }
                if (isListening) {
                    viewModel.stopListening()
                } else {
                    viewModel.startListening()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasMicPermission && !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isListening) "Stop Listening" else "Start Listening")
        }

        if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Status: $statusText",
                    style = MaterialTheme.typography.bodySmall
                )
                if (userSpeech.isNotBlank()) {
                    Text(
                        text = "You said:\n$userSpeech",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (geminiReply.isNotBlank()) {
                    Text(
                        text = "Gemini Nano:\n$geminiReply",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}