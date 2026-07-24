package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.api.LocalGemmaClient
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.DomainMode
import com.example.ui.HomeScreen
import com.example.ui.ModelManagerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Request permissions for local models
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    } else {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    
    // Initialize LocalGemmaClient
    LocalGemmaClient.scanModels(this)
    LocalGemmaClient.initialize(this)

    // Initialize Database and Repository
    val database = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "compass_chat_db"
    )
    .fallbackToDestructiveMigration()
    .build()
    val repository = ChatRepository(database.chatMessageDao())
    
    val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(repository) as T
            }
            if (modelClass.isAssignableFrom(com.example.ui.HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return com.example.ui.HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val chatViewModelCommunity: ChatViewModel = viewModel(key = "community", factory = viewModelFactory)
            val chatViewModelHealth: ChatViewModel = viewModel(key = "health", factory = viewModelFactory)
            val chatViewModelEducation: ChatViewModel = viewModel(key = "education", factory = viewModelFactory)
            val chatViewModelInnovation: ChatViewModel = viewModel(key = "innovation", factory = viewModelFactory)
            val chatViewModelAgriculture: ChatViewModel = viewModel(key = "agriculture", factory = viewModelFactory)
            val chatViewModelBusiness: ChatViewModel = viewModel(key = "business", factory = viewModelFactory)
            val chatViewModelEnvironment: ChatViewModel = viewModel(key = "environment", factory = viewModelFactory)
            val chatViewModelCivic: ChatViewModel = viewModel(key = "civic", factory = viewModelFactory)
            val chatViewModelEmergency: ChatViewModel = viewModel(key = "emergency", factory = viewModelFactory)
            val chatViewModelTool: ChatViewModel = viewModel(key = "tool", factory = viewModelFactory)
            val homeViewModel: com.example.ui.HomeViewModel = viewModel(factory = viewModelFactory)

            androidx.compose.runtime.LaunchedEffect(Unit) {
                chatViewModelCommunity.setMode(DomainMode.COMMUNITY)
                chatViewModelHealth.setMode(DomainMode.HEALTH)
                chatViewModelEducation.setMode(DomainMode.EDUCATION)
                chatViewModelInnovation.setMode(DomainMode.INNOVATION)
                chatViewModelAgriculture.setMode(DomainMode.AGRICULTURE)
                chatViewModelBusiness.setMode(DomainMode.BUSINESS)
                chatViewModelEnvironment.setMode(DomainMode.ENVIRONMENT)
                chatViewModelCivic.setMode(DomainMode.CIVIC)
                chatViewModelEmergency.setMode(DomainMode.EMERGENCY)
                chatViewModelTool.setMode(DomainMode.TOOL)
            }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        onNavigateToChat = { mode ->
                            when (mode) {
                                DomainMode.HEALTH -> navController.navigate("chat_health")
                                DomainMode.EDUCATION -> navController.navigate("chat_education")
                                DomainMode.COMMUNITY -> navController.navigate("chat_community")
                                DomainMode.INNOVATION -> navController.navigate("chat_innovation")
                                DomainMode.AGRICULTURE -> navController.navigate("chat_agriculture")
                                DomainMode.BUSINESS -> navController.navigate("chat_business")
                                DomainMode.ENVIRONMENT -> navController.navigate("chat_environment")
                                DomainMode.CIVIC -> navController.navigate("chat_civic")
                                DomainMode.EMERGENCY -> navController.navigate("chat_emergency")
                                DomainMode.TOOL -> navController.navigate("chat_tool")
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate("models")
                        },
                        onNavigateToDocumentReader = {
                            navController.navigate("document_reader")
                        },
                        onSendMessage = { text, uri ->
                            chatViewModelCommunity.sendMessage(text, uri?.toString(), this@MainActivity)
                            navController.navigate("chat_community")
                        }
                    )
                }
                composable("chat_community") {
                    ChatScreen(
                        viewModel = chatViewModelCommunity,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_health") {
                    ChatScreen(
                        viewModel = chatViewModelHealth,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_education") {
                    ChatScreen(
                        viewModel = chatViewModelEducation,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_innovation") {
                    ChatScreen(
                        viewModel = chatViewModelInnovation,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_agriculture") {
                    ChatScreen(
                        viewModel = chatViewModelAgriculture,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_business") {
                    ChatScreen(
                        viewModel = chatViewModelBusiness,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_environment") {
                    ChatScreen(
                        viewModel = chatViewModelEnvironment,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_civic") {
                    ChatScreen(
                        viewModel = chatViewModelCivic,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_emergency") {
                    ChatScreen(
                        viewModel = chatViewModelEmergency,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat_tool") {
                    ChatScreen(
                        viewModel = chatViewModelTool,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("models") {
                    ModelManagerScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("document_reader") {
                    com.example.ui.DocumentReaderScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
      }
    }
  }
}
