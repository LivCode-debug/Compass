package com.example.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.Send
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToChat: (DomainMode) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDocumentReader: () -> Unit,
    onSendMessage: (String, android.net.Uri?) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Home") }

    Scaffold(
        bottomBar = { 
            BottomNavBar(selectedTab) { selectedTab = it } 
        },
        containerColor = Color(0xFFF7F9F2)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "Home" -> HomeContent(onNavigateToChat, onNavigateToSettings, onNavigateToDocumentReader, onSendMessage)
                "History" -> HistoryContent(homeViewModel, onNavigateToChat)
                "Circle" -> CircleContent()
                "Resources" -> ResourcesContent()
            }
        }
    }
}

@Composable
fun HistoryContent(homeViewModel: HomeViewModel, onNavigateToChat: (DomainMode) -> Unit) {
    val recentConversations by homeViewModel.recentConversations.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Recent Conversations", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1A))
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recentConversations.isEmpty()) {
            Text("No recent conversations.", fontSize = 14.sp, color = Color.Gray)
        } else {
            recentConversations.forEach { message ->
                val mode = try { DomainMode.valueOf(message.domainMode) } catch(e: Exception) { DomainMode.COMMUNITY }
                val title = message.text
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onNavigateToChat(mode) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E8CD))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when(mode) {
                            DomainMode.HEALTH -> Icons.Default.MedicalServices
                            DomainMode.EDUCATION -> Icons.Default.School
                            else -> Icons.Default.Chat
                        }
                        val iconTint = when(mode) {
                            DomainMode.HEALTH -> Color(0xFFC0392B)
                            DomainMode.EDUCATION -> Color(0xFF2980B9)
                            else -> Color(0xFF386B40)
                        }
                        Icon(icon, contentDescription = null, tint = iconTint)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424940),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircleContent() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Community Circle (Offline Sync)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1A))
        Text("Messages sync via Bluetooth mesh when near others.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { /* Sync manually */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B40))) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync via Bluetooth")
            }
        }
        
        val posts = listOf(
            "Community Health Workshop tomorrow at the village center!" to "Dr. Amina", 
            "Anyone have the grade 4 reading list for the new term?" to "Teacher John", 
            "Water sanitation tips during the rainy season" to "Local Council"
        )
        posts.forEach { (title, author) ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E8CD))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF191C1A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Posted by $author • 2 hours ago (via mesh)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ResourcesContent() {
    val context = LocalContext.current
    var userResources by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                val fileName = it.lastPathSegment ?: "Uploaded Document"
                userResources = userResources + (fileName to "PDF • Uploaded")
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Saved Resources (Available Offline)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1A))
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B40)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload New Resource")
        }

        val defaultResources = listOf("First Aid Guide" to "PDF • 2 MB", "English-Local Dictionary" to "App Module • 12 MB", "Crop Rotation Manual" to "PDF • 5 MB")
        val allResources = defaultResources + userResources

        allResources.forEach { (title, meta) ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E8CD))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF386B40))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF191C1A),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(meta, fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun HomeContent(onNavigateToChat: (DomainMode) -> Unit, onNavigateToSettings: () -> Unit, onNavigateToDocumentReader: () -> Unit, onSendMessage: (String, android.net.Uri?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HeaderSection(onNavigateToSettings)
        Spacer(modifier = Modifier.height(24.dp))
        PromptArea(
            onNavigateToChat = { onNavigateToChat(DomainMode.COMMUNITY) },
            onSendMessage = onSendMessage
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "CORE SERVICES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424940),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HealthCard(
                    onClick = { onNavigateToChat(DomainMode.HEALTH) }
                )
                CommunityCard(
                    onClick = { onNavigateToChat(DomainMode.COMMUNITY) }
                )
                AgricultureCard(
                    onClick = { onNavigateToChat(DomainMode.AGRICULTURE) }
                )
                EnvironmentCard(
                    onClick = { onNavigateToChat(DomainMode.ENVIRONMENT) }
                )
                EmergencyCard(
                    onClick = { onNavigateToChat(DomainMode.EMERGENCY) }
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EducationCard(
                    onClick = { onNavigateToChat(DomainMode.EDUCATION) }
                )
                InnovationCard(
                    onClick = { onNavigateToChat(DomainMode.INNOVATION) }
                )
                BusinessCard(
                    onClick = { onNavigateToChat(DomainMode.BUSINESS) }
                )
                CivicCard(
                    onClick = { onNavigateToChat(DomainMode.CIVIC) }
                )
                ToolCard(
                    onClick = { onNavigateToChat(DomainMode.TOOL) }
                )
                DocumentReaderCard(
                    onClick = { onNavigateToDocumentReader() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        QuickStats()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeaderSection(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF386B40)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Compass",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C1A)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF386B40))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "OFFLINE MODE ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF386B40),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Memory, contentDescription = "Manage Models", tint = Color(0xFF424940))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD7E8CD))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF141F0E)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptArea(onNavigateToChat: () -> Unit, onSendMessage: (String, Uri?) -> Unit) {
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                onSendMessage(spokenText, null)
            }
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val tempFile = File(context.cacheDir, "temp_camera_home_${System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                val imageUri = Uri.fromFile(tempFile)
                // If they took a photo, just send it with whatever text they have or empty text
                onSendMessage(textInput, imageUri)
                textInput = ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD7E8CD), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { 
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                }
                try {
                    speechRecognizerLauncher.launch(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFB7F397))
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF141F0E), modifier = Modifier.size(24.dp))
        }
        
        TextField(
            value = textInput,
            onValueChange = { textInput = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            placeholder = { Text("How can I help you today?", fontSize = 14.sp, color = Color(0xFF72796F)) },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            maxLines = 3
        )
        
        if (textInput.isNotBlank()) {
            IconButton(
                onClick = { 
                    onSendMessage(textInput, null)
                    textInput = ""
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF386B40))
            }
        } else {
            IconButton(onClick = { cameraLauncher.launch(null) }) {
                Icon(Icons.Default.CameraEnhance, contentDescription = "Camera", tint = Color(0xFF72796F))
            }
        }
    }
}

@Composable
private fun HealthCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE8F3D6))
            .border(1.dp, Color(0xFFD1E1B5), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF1D3720))
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF386B40))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LATEST",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Health",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1D3720),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create solutions that support healthcare and well-being.",
                fontSize = 14.sp,
                color = Color(0xFF3E4A3B).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1D3720))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Health", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EducationCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE0E2F9))
            .border(1.dp, Color(0xFFCCD0F2), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF171B2C))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Education",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF171B2C),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Help students, teachers, or schools.",
                fontSize = 14.sp,
                color = Color(0xFF444659).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF171B2C))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Education", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CommunityCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE5F1E2))
            .border(1.dp, Color(0xFFC7E2C2), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF1D3720))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Community\nImpact",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1D3720),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Solve problems that affect people in your community.",
                fontSize = 14.sp,
                color = Color(0xFF3E4A3B).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1D3720))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Community", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InnovationCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF3E5F5))
            .border(1.dp, Color(0xFFE1BEE7), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF4B2354))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Open\nInnovation",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B2354),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Any other creative idea that uses Gemma.",
                fontSize = 14.sp,
                color = Color(0xFF4B2354).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF4B2354))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Innovation", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AgricultureCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF9FBE7))
            .border(1.dp, Color(0xFFE6EE9C), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF33691E))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Agriculture\n& Farming",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF33691E),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crop management, soil health, and pest control.",
                fontSize = 14.sp,
                color = Color(0xFF33691E).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF33691E))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Agriculture", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BusinessCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE3F2FD))
            .border(1.dp, Color(0xFF90CAF9), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF0D47A1))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Local\nEnterprise",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0D47A1),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Financial literacy and micro-business growth.",
                fontSize = 14.sp,
                color = Color(0xFF0D47A1).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0D47A1))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Business", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EnvironmentCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE0F2F1))
            .border(1.dp, Color(0xFF80CBC4), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF004D40))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Environment",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF004D40),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sustainability, clean water, and local eco-actions.",
                fontSize = 14.sp,
                color = Color(0xFF004D40).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF004D40))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Environment", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CivicCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFE8EAF6))
            .border(1.dp, Color(0xFF9FA8DA), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF1A237E))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Civic\nEngagement",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A237E),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Human rights, local governance, and community organizing.",
                fontSize = 14.sp,
                color = Color(0xFF1A237E).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1A237E))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Civic", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EmergencyCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFFFEBEE))
            .border(1.dp, Color(0xFFEF9A9A), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB71C1C))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Disaster\nPreparedness",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB71C1C),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Offline emergency protocols, first aid, and survival.",
                fontSize = 14.sp,
                color = Color(0xFFB71C1C).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFB71C1C))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Emergency", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ToolCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF424242))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Generator\nTool",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF424242),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lesson Plans, Symptom Trackers & more.",
                fontSize = 14.sp,
                color = Color(0xFF424242).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF424242))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Tool", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun QuickStats() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
            title = "PROGRESS",
            value = "82% Today"
        )
        StatBox(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "COMMUNITY",
            value = "12 Discussions"
        )
    }
}

@Composable
private fun StatBox(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD7E8CD), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF386B40), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF72796F)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun BottomNavBar(selectedTab: String, onTabSelected: (String) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavBarItem(icon = Icons.Default.Home, label = "Home", isSelected = selectedTab == "Home", onClick = { onTabSelected("Home") })
            NavBarItem(icon = Icons.Default.History, label = "History", isSelected = selectedTab == "History", onClick = { onTabSelected("History") })
            NavBarItem(icon = Icons.Default.Groups, label = "Circle", isSelected = selectedTab == "Circle", onClick = { onTabSelected("Circle") })
            NavBarItem(icon = Icons.Default.Inventory2, label = "Resources", isSelected = selectedTab == "Resources", onClick = { onTabSelected("Resources") })
        }
    }
}

@Composable
private fun NavBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF386B40) else Color(0xFF424940)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFFD7E8CD) else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun DocumentReaderCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFFFF3E0))
            .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFE65100))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Offline Doc\nReader",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE65100),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Read local PDF resources and ask questions.",
                fontSize = 14.sp,
                color = Color(0xFFE65100).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE65100))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Reader", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
