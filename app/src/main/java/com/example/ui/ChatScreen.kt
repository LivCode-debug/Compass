package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ChatMessage
import com.example.api.LocalGemmaClient
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val messages by viewModel.chatHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = false)
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle(initialValue = DomainMode.COMMUNITY)

    var textInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val isReady by LocalGemmaClient.isInitialized.collectAsStateWithLifecycle()
    var wasReady by remember { mutableStateOf(isReady) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val tempFile = File(context.cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                selectedImageUri = Uri.fromFile(tempFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isReady) {
        if (wasReady && !isReady) {
            Toast.makeText(context, "Lost connection to local model runtime", Toast.LENGTH_SHORT).show()
        }
        wasReady = isReady
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val titleText = when(currentMode) {
        DomainMode.HEALTH -> "Community Health"
        DomainMode.EDUCATION -> "Socratic Tutoring"
        DomainMode.COMMUNITY -> "Community Impact"
        DomainMode.INNOVATION -> "Open Innovation"
        DomainMode.AGRICULTURE -> "Agriculture & Farming"
        DomainMode.BUSINESS -> "Local Enterprise"
        DomainMode.ENVIRONMENT -> "Sustainability"
        DomainMode.CIVIC -> "Civic Engagement"
        DomainMode.EMERGENCY -> "Disaster Prep"
        DomainMode.TOOL -> "Generator Tool"
    }
    val titleColor = when(currentMode) {
        DomainMode.HEALTH -> Color(0xFF1D3720)
        DomainMode.EDUCATION -> Color(0xFF171B2C)
        DomainMode.COMMUNITY -> Color(0xFF424940)
        DomainMode.INNOVATION -> Color(0xFF4B2354)
        DomainMode.AGRICULTURE -> Color(0xFF33691E)
        DomainMode.BUSINESS -> Color(0xFF0D47A1)
        DomainMode.ENVIRONMENT -> Color(0xFF004D40)
        DomainMode.CIVIC -> Color(0xFF1A237E)
        DomainMode.EMERGENCY -> Color(0xFFB71C1C)
        DomainMode.TOOL -> Color(0xFF424242)
    }
    val containerColor = when(currentMode) {
        DomainMode.HEALTH -> Color(0xFFE8F3D6)
        DomainMode.EDUCATION -> Color(0xFFE0E2F9)
        DomainMode.COMMUNITY -> Color(0xFFE5F1E2)
        DomainMode.INNOVATION -> Color(0xFFF3E5F5)
        DomainMode.AGRICULTURE -> Color(0xFFF9FBE7)
        DomainMode.BUSINESS -> Color(0xFFE3F2FD)
        DomainMode.ENVIRONMENT -> Color(0xFFE0F2F1)
        DomainMode.CIVIC -> Color(0xFFE8EAF6)
        DomainMode.EMERGENCY -> Color(0xFFFFEBEE)
        DomainMode.TOOL -> Color(0xFFF5F5F5)
    }

    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(titleText, fontWeight = FontWeight.Bold, color = titleColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            // Persistent Offline-First Status Indicator
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.height(20.dp).padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(if (isReady) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isReady) "Offline-First Ready" else "Model Disconnected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mode Selection")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Health Navigator") },
                                onClick = { 
                                    viewModel.setMode(DomainMode.HEALTH)
                                    expanded = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Education Navigator") },
                                onClick = { 
                                    viewModel.setMode(DomainMode.EDUCATION)
                                    expanded = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("General Navigator") },
                                onClick = { 
                                    viewModel.setMode(DomainMode.COMMUNITY)
                                    expanded = false 
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear History") },
                                onClick = {
                                    viewModel.clearHistory()
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                )
            )
        },
        bottomBar = {
            // Input Area
            Surface(
                color = containerColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach Image", tint = titleColor)
                        }
                        IconButton(onClick = { cameraLauncher.launch(null) }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo", tint = titleColor)
                        }
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            placeholder = { Text("Ask Compass...") },
                            shape = RoundedCornerShape(28.dp),
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        val fabColor = when(currentMode) {
                            DomainMode.HEALTH -> Color(0xFF386B40)
                            DomainMode.EDUCATION -> Color(0xFF171B2C)
                            DomainMode.COMMUNITY -> Color(0xFF386B40)
                            DomainMode.INNOVATION -> Color(0xFF7E57C2)
                            DomainMode.AGRICULTURE -> Color(0xFF689F38)
                            DomainMode.BUSINESS -> Color(0xFF1976D2)
                            DomainMode.ENVIRONMENT -> Color(0xFF00796B)
                            DomainMode.CIVIC -> Color(0xFF3949AB)
                            DomainMode.EMERGENCY -> Color(0xFFD32F2F)
                            DomainMode.TOOL -> Color(0xFF616161)
                        }
                        FloatingActionButton(
                            onClick = {
                                if ((textInput.isNotBlank() || selectedImageUri != null) && !isLoading) {
                                    viewModel.sendMessage(textInput, selectedImageUri?.toString(), context)
                                    textInput = ""
                                    selectedImageUri = null
                                }
                            },
                            containerColor = fabColor,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "How can Compass help you today?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(messages) { message ->
                        MessageBubble(message)
                    }
                }
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    val bubbleColor = if (isUser) {
        when (message.domainMode) {
            DomainMode.HEALTH.name -> Color(0xFF386B40)
            DomainMode.EDUCATION.name -> Color(0xFF171B2C)
            DomainMode.COMMUNITY.name -> Color(0xFF386B40)
            DomainMode.INNOVATION.name -> Color(0xFF7E57C2)
            DomainMode.AGRICULTURE.name -> Color(0xFF689F38)
            DomainMode.BUSINESS.name -> Color(0xFF1976D2)
            DomainMode.ENVIRONMENT.name -> Color(0xFF00796B)
            DomainMode.CIVIC.name -> Color(0xFF3949AB)
            DomainMode.EMERGENCY.name -> Color(0xFFD32F2F)
            DomainMode.TOOL.name -> Color(0xFF616161)
            else -> MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        when (message.domainMode) {
            DomainMode.HEALTH.name -> Color.White
            DomainMode.EDUCATION.name -> Color.White
            DomainMode.COMMUNITY.name -> Color.White
            DomainMode.INNOVATION.name -> Color.White
            DomainMode.AGRICULTURE.name -> Color.White
            DomainMode.BUSINESS.name -> Color.White
            DomainMode.ENVIRONMENT.name -> Color.White
            DomainMode.CIVIC.name -> Color.White
            DomainMode.EMERGENCY.name -> Color.White
            DomainMode.TOOL.name -> Color.White
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    }
    
    val textColor = if (isUser) {
        when (message.domainMode) {
            DomainMode.HEALTH.name -> Color.White
            DomainMode.EDUCATION.name -> Color.White
            DomainMode.COMMUNITY.name -> Color.White
            DomainMode.INNOVATION.name -> Color.White
            DomainMode.AGRICULTURE.name -> Color.White
            DomainMode.BUSINESS.name -> Color.White
            DomainMode.ENVIRONMENT.name -> Color.White
            DomainMode.CIVIC.name -> Color.White
            DomainMode.EMERGENCY.name -> Color.White
            DomainMode.TOOL.name -> Color.White
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }
    } else {
        when (message.domainMode) {
            DomainMode.HEALTH.name -> Color(0xFF1D3720)
            DomainMode.EDUCATION.name -> Color(0xFF171B2C)
            DomainMode.COMMUNITY.name -> Color(0xFF1D3720)
            DomainMode.INNOVATION.name -> Color(0xFF4B2354)
            DomainMode.AGRICULTURE.name -> Color(0xFF33691E)
            DomainMode.BUSINESS.name -> Color(0xFF0D47A1)
            DomainMode.ENVIRONMENT.name -> Color(0xFF004D40)
            DomainMode.CIVIC.name -> Color(0xFF1A237E)
            DomainMode.EMERGENCY.name -> Color(0xFFB71C1C)
            DomainMode.TOOL.name -> Color(0xFF424242)
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = if (isUser) 24.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 24.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.imageUri != null) {
                    AsyncImage(
                        model = Uri.parse(message.imageUri),
                        contentDescription = "Message Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = if (message.text.isNotBlank()) 8.dp else 0.dp)
                    )
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = parseMarkdown(message.text),
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
