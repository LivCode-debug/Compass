package com.example.ui

import android.widget.Toast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.LocalGemmaClient
import com.example.api.ModelDownloader
import com.example.api.ModelVariant
import kotlinx.coroutines.launch

@Composable
fun useModelStatus(): State<Boolean> {
    val isReady = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            isReady.value = LocalGemmaClient.isInitialized.value
            kotlinx.coroutines.delay(500)
        }
    }
    return isReady
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInitialized by LocalGemmaClient.isInitialized.collectAsStateWithLifecycle()
    val currentModel by LocalGemmaClient.currentModelName.collectAsStateWithLifecycle()
    val availableModels by LocalGemmaClient.availableModels.collectAsStateWithLifecycle()
    val errorMessage by LocalGemmaClient.errorMessage.collectAsStateWithLifecycle()

    val isReadyState = useModelStatus()
    val isReady = isReadyState.value
    val statusText = if (isReady) "Ready" else "Disconnected"

    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Downloader states
    val isDownloading by ModelDownloader.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by ModelDownloader.downloadProgress.collectAsStateWithLifecycle()
    val downloadStatus by ModelDownloader.downloadStatus.collectAsStateWithLifecycle()

    var presetExpanded by remember { mutableStateOf(false) }
    var selectedPresetIndex by remember { mutableStateOf(0) } // 0 = Gemma 4 E2B IT, 1 = Gemma 4 E4B IT QAT

    DisposableEffect(Unit) {
        LocalGemmaClient.startBackgroundScanning(context)
        onDispose {
            LocalGemmaClient.stopBackgroundScanning()
            ModelDownloader.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Local Models", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { LocalGemmaClient.scanModels(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Status Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isReady) MaterialTheme.colorScheme.primary else Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Active: $currentModel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // SECTION 1: Local Model Selection
            Text(
                text = "ACTIVE MODEL CONFIGURATION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = currentModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Model on Device") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (availableModels.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No local models found on device") },
                                onClick = { expanded = false }
                            )
                        } else {
                            availableModels.forEach { modelPath ->
                                val displayName = modelPath.substringAfterLast("/")
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        expanded = false
                                        LocalGemmaClient.initialize(context, modelPath)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (availableModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val pathToDelete = availableModels.find { it.endsWith(currentModel) }
                        if (pathToDelete != null) {
                            LocalGemmaClient.deleteModel(pathToDelete)
                            LocalGemmaClient.scanModels(context)
                            Toast.makeText(context, "Model deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Selected Model")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 2: Online Model Downloader
            Text(
                text = "DOWNLOAD MODELS ONLINE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LiteRT Model Downloader",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Download open models directly into the app storage. It is recommended to download over Wi-Fi as these models are large.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Selection Dropdown
                    ExposedDropdownMenuBox(
                        expanded = presetExpanded,
                        onExpandedChange = { if (!isDownloading) presetExpanded = !presetExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectionText = "${ModelVariant.PRESETS[selectedPresetIndex].name} (${ModelVariant.PRESETS[selectedPresetIndex].expectedSize})"
                        OutlinedTextField(
                            value = selectionText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model Variant Source") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = !isDownloading
                        )
                        ExposedDropdownMenu(
                            expanded = presetExpanded,
                            onDismissRequest = { presetExpanded = false }
                        ) {
                            ModelVariant.PRESETS.forEachIndexed { index, preset ->
                                DropdownMenuItem(
                                    text = { Text("${preset.name} (${preset.expectedSize})") },
                                    onClick = {
                                        selectedPresetIndex = index
                                        presetExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDownloading) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val progress = downloadProgress
                            if (progress != null) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = downloadStatus ?: "Downloading...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { ModelDownloader.cancelDownload() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Download")
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Display downloadStatus if present to show the last result (Success / Error / Cancelled)
                            downloadStatus?.let { status ->
                                val isError = status.contains("Error", ignoreCase = true)
                                val isSuccess = status.contains("Success", ignoreCase = true)
                                val color = when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isSuccess -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isError -> MaterialTheme.colorScheme.errorContainer
                                        isSuccess -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isError) Icons.Default.Error else if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = color,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val urlToDownload = ModelVariant.PRESETS[selectedPresetIndex].downloadUrl
                                    val filename = urlToDownload.substringAfterLast("/")

                                    if (urlToDownload.isNotBlank()) {
                                        coroutineScope.launch {
                                            ModelDownloader.downloadModel(
                                                context = context,
                                                modelUrl = urlToDownload,
                                                outputFileName = filename,
                                                authToken = null
                                            )
                                        }
                                    }
                                },
                                enabled = true,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Download")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

