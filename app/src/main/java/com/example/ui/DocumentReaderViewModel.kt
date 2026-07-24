package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.LocalGemmaClient
import com.example.data.ChatMessage
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentReaderViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _documentContent = MutableStateFlow<String?>(null)
    val documentContent: StateFlow<String?> = _documentContent

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isReading.value = true
            try {
                withContext(Dispatchers.IO) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val document = PDDocument.load(inputStream)
                        val stripper = PDFTextStripper()
                        val text = stripper.getText(document)
                        document.close()
                        _documentContent.value = text
                        
                        _messages.value = listOf(
                            ChatMessage(text = "I have read your document. What would you like to know about it?", isUser = false)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _documentContent.value = "Error reading document: ${e.message}"
            } finally {
                _isReading.value = false
            }
        }
    }

    fun sendMessage(text: String, context: Context) {
        if (text.isBlank()) return
        
        val docContent = _documentContent.value ?: ""

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val prompt = "Document Content:\n$docContent\n\nUser Question:\n$text\n\nAnswer the user's question based on the document content."
                val responseText = LocalGemmaClient.generateResponse(prompt)
                
                val assistantMessage = ChatMessage(text = responseText ?: "Sorry, I couldn't generate a response.", isUser = false)
                _messages.value = _messages.value + assistantMessage
            } catch (e: Exception) {
                e.printStackTrace()
                _messages.value = _messages.value + ChatMessage(text = "Error: ${e.message}", isUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
