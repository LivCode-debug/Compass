package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Candidate
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.ThinkingConfig
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

enum class DomainMode {
    HEALTH,
    EDUCATION,
    COMMUNITY,
    INNOVATION,
    AGRICULTURE,
    BUSINESS,
    ENVIRONMENT,
    CIVIC,
    EMERGENCY,
    TOOL
}

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _currentMode = MutableStateFlow(DomainMode.COMMUNITY)
    val currentMode = _currentMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatHistory: StateFlow<List<ChatMessage>> = _currentMode
        .flatMapLatest { mode ->
            repository.getMessagesForDomain(mode.name)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun setMode(mode: DomainMode) {
        _currentMode.value = mode
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear(_currentMode.value.name)
        }
    }

    fun sendMessage(text: String, imageUri: String? = null, context: android.content.Context? = null) {
        if (text.isBlank() && imageUri == null) return

        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Save user message to Room
            repository.insert(ChatMessage(text = text, isUser = true, domainMode = _currentMode.value.name, imageUri = imageUri))

            // 2. Build history for API request
            // Map Room messages to Gemini API format
            val history = chatHistory.value.takeLast(10).mapNotNull { msg ->
                val partsList = mutableListOf<Part>()
                if (msg.text.isNotBlank()) {
                    partsList.add(Part(text = msg.text))
                }
                if (msg.imageUri != null && context != null) {
                    val base64 = getBase64Image(context, msg.imageUri)
                    if (base64 != null) {
                        partsList.add(Part(inlineData = com.example.api.InlineData(mimeType = "image/jpeg", data = base64)))
                    }
                }
                if (partsList.isNotEmpty()) {
                    Content(
                        role = if (msg.isUser) "user" else "model",
                        parts = partsList
                    )
                } else null
            }
            
            // 3. Append the new message
            val currentParts = mutableListOf<Part>()
            if (text.isNotBlank()) {
                currentParts.add(Part(text = text))
            }
            if (imageUri != null && context != null) {
                val base64 = getBase64Image(context, imageUri)
                if (base64 != null) {
                    currentParts.add(Part(inlineData = com.example.api.InlineData(mimeType = "image/jpeg", data = base64)))
                }
            }
            val currentContents = history + Content(role = "user", parts = currentParts)

            // 4. Construct System Instruction based on mode
            val systemInstructionText = when (_currentMode.value) {
                DomainMode.HEALTH -> "You are Compass, an offline-first community health navigator. Provide locally relevant, accurate medical guidelines and health protocols in simple language. Act as a partner in decision-making and explain your reasoning. Emphasize that you are an AI assistant and users should consult local health workers for severe emergencies. Use simple text formatting. You may use **bold** or *italic*, but do not use complex markdown like headers or lists."
                DomainMode.EDUCATION -> "You are Compass, an offline-first community education navigator. Provide educational support across multiple subjects. Act as a Socratic tutor, explaining concepts clearly, asking guiding questions, and adapting to the learner's level. Use simple text formatting. You may use **bold** or *italic*, but do not use complex markdown like headers or lists."
                DomainMode.COMMUNITY -> "You are Compass, a community impact navigator. Help users solve problems that affect people in their community. Use simple text formatting. You may use **bold** or *italic*, but do not use complex markdown like headers or lists."
                DomainMode.INNOVATION -> "You are Compass, an open innovation assistant. Help users with any other creative idea. Use simple text formatting. You may use **bold** or *italic*, but do not use complex markdown like headers or lists."
                DomainMode.AGRICULTURE -> "You are Compass, an agriculture advisor. Help local farmers with crop management, soil health, pest control, and sustainable farming using offline knowledge. Use simple text formatting."
                DomainMode.BUSINESS -> "You are Compass, a micro-business mentor. Help local entrepreneurs with financial literacy, bookkeeping, pricing, and small business growth. Use simple text formatting."
                DomainMode.ENVIRONMENT -> "You are Compass, an environmental guide. Provide advice on sustainability, clean water, waste management, and local eco-actions. Use simple text formatting."
                DomainMode.CIVIC -> "You are Compass, a civic empowerment guide. Educate users on human rights, local governance, community organizing, and legal basics. Use simple text formatting."
                DomainMode.EMERGENCY -> "You are Compass, a disaster preparedness assistant. Provide offline emergency protocols, first aid, flood/fire survival strategies, and crisis management. Use simple text formatting."
                DomainMode.TOOL -> "You are Compass, a tool generator. Generate structured outputs like Lesson Plans for teachers or Symptom Trackers for health users, formatted for offline sharing. Use clear, organized text formatting."
            } + " IMPORTANT: You MUST restrict your answers strictly to this domain. If the user asks a question outside of this domain, politely refuse to answer and remind them of your specific role."

            val systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))

            // 5. Select Model and Configuration based on complexity
            val request: GenerateContentRequest
            val apiKey = BuildConfig.GEMINI_API_KEY
            val responseText: String

            try {
                if (com.example.api.LocalGemmaClient.isInitialized.value && imageUri == null) {
                    // Local model only supports text
                    val prompt = systemInstructionText + "\n\n" + currentContents.joinToString("\n") { "${it.role}: ${it.parts.firstOrNull()?.text}" }
                    val gemmaResponse = com.example.api.LocalGemmaClient.generateResponse(prompt)
                    if (gemmaResponse != null) {
                        responseText = gemmaResponse
                    } else {
                        throw Exception("Local Gemma model failed to respond.")
                    }
                } else {
                    if (_currentMode.value == DomainMode.HEALTH || _currentMode.value == DomainMode.EDUCATION) {
                        // Complex tasks require Pro with HIGH thinking level
                        request = GenerateContentRequest(
                            contents = currentContents,
                            systemInstruction = systemInstruction,
                            generationConfig = GenerationConfig(
                                thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
                            )
                        )
                        val response = RetrofitClient.service.generateContentPro(apiKey, request)
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I'm sorry, I couldn't process that request."
                    } else {
                        // General/Fast tasks
                        request = GenerateContentRequest(
                            contents = currentContents,
                            systemInstruction = systemInstruction
                        )
                        val response = RetrofitClient.service.generateContentFlash(apiKey, request)
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I'm sorry, I couldn't process that request."
                    }
                }

                // 6. Save model response to Room
                repository.insert(ChatMessage(text = responseText, isUser = false, domainMode = _currentMode.value.name))

            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("Unable to resolve host") == true || e is java.net.UnknownHostException) {
                    "Offline Mode Active: The online Gemini API is unreachable. I can only process text using the local on-device model while disconnected. Please connect to the internet to analyze images."
                } else {
                    "Error: ${e.message}"
                }
                repository.insert(ChatMessage(text = errorMsg, isUser = false, domainMode = _currentMode.value.name))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
