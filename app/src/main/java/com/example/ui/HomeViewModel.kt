package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(private val repository: ChatRepository) : ViewModel() {
    val recentConversations: StateFlow<List<ChatMessage>> = repository.getRecentUserMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
