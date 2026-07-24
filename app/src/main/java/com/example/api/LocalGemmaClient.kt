package com.example.api

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocalGemmaClient {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    
    // Caching mechanism for fast-switching
    private var cachedEngine: Engine? = null
    private var cachedConversation: Conversation? = null
    private var currentModelPathStr: String? = null
    private var cachedModelPathStr: String? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    private val _currentModelName = MutableStateFlow("None")
    val currentModelName = _currentModelName.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private var backgroundScanningJob: kotlinx.coroutines.Job? = null

    fun startBackgroundScanning(context: Context) {
        if (backgroundScanningJob?.isActive == true) return
        
        backgroundScanningJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                scanModels(context)
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun stopBackgroundScanning() {
        backgroundScanningJob?.cancel()
        backgroundScanningJob = null
    }

    private fun searchFilesRecursively(dir: File, maxDepth: Int, currentDepth: Int = 0, foundModels: MutableList<String>) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory) return
        
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    searchFilesRecursively(file, maxDepth, currentDepth + 1, foundModels)
                } else if (file.isFile) {
                    val name = file.name
                    if (name.endsWith(".tflite") || name.endsWith(".bin") || name.endsWith(".task") || name.endsWith(".litertlm")) {
                        foundModels.add(file.absolutePath)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scanModels(context: Context) {
        val searchPaths = listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null),
            File("/data/local/tmp/llm/"),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
            File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "EdgeGallery"),
            File("/sdcard/Download/"),
            File("/sdcard/Download/EdgeGallery/")
        )
        
        val foundModels = mutableListOf<String>()
        
        for (dir in searchPaths) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                searchFilesRecursively(dir, maxDepth = 3, foundModels = foundModels)
            }
        }
        
        if (_availableModels.value != foundModels) {
            _availableModels.value = foundModels
        }
    }

    fun deleteModel(modelPath: String) {
        try {
            val file = File(modelPath)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initialize(context: Context, modelPath: String? = null) {
        val targetModelPath = modelPath ?: _availableModels.value.firstOrNull() ?: return
        
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            if (targetModelPath == currentModelPathStr && _isInitialized.value) return@launch

            _isInitialized.value = false

            // Check if requested model is in high-speed RAM cache
            if (targetModelPath == cachedModelPathStr && cachedEngine != null) {
                // Swap current active model with cached model
                val tempEngine = engine
                val tempConversation = conversation
                val tempPath = currentModelPathStr
                
                engine = cachedEngine
                conversation = cachedConversation
                currentModelPathStr = cachedModelPathStr
                
                cachedEngine = tempEngine
                cachedConversation = tempConversation
                cachedModelPathStr = tempPath
                
                _isInitialized.value = true
                _currentModelName.value = File(targetModelPath).name
                return@launch
            }

            try {
                val modelFile = File(targetModelPath)
                if (modelFile.exists()) {
                    val engineConfig = EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = Backend.CPU()
                    )
                    val newEngine = Engine(engineConfig)
                    newEngine.initialize()
                    
                    val conversationConfig = ConversationConfig(
                        samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
                    )
                    val newConversation = newEngine.createConversation(conversationConfig)
                    
                    // Close the oldest cached model to prevent OOM
                    try { cachedConversation?.close() } catch (e: Exception) {}
                    try { cachedEngine?.close() } catch (e: Exception) {}
                    
                    // Move current model to cache
                    cachedEngine = engine
                    cachedConversation = conversation
                    cachedModelPathStr = currentModelPathStr
                    
                    // Set new model as active
                    engine = newEngine
                    conversation = newConversation
                    currentModelPathStr = targetModelPath
                    
                    _isInitialized.value = true
                    _currentModelName.value = modelFile.name
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "File does not exist: $targetModelPath"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isInitialized.value = false
                _currentModelName.value = "Error Loading"
                _errorMessage.value = e.message
            }
        }
    }

    suspend fun generateResponse(prompt: String): String? = withContext(Dispatchers.IO) {
        if (!_isInitialized.value || conversation == null) {
            return@withContext null
        }
        try {
            val responseMessage = conversation?.sendMessage(prompt)
            val contents = responseMessage?.contents?.contents
            val textContent = contents?.filterIsInstance<com.google.ai.edge.litertlm.Content.Text>()?.firstOrNull()
            textContent?.text ?: responseMessage.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}
