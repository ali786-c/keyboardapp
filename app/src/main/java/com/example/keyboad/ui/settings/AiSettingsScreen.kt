package com.example.keyboad.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.keyboad.data.SettingsPreferencesManager
import com.example.keyboad.ui.settings.components.SectionHeader
import com.example.keyboad.ui.settings.components.SettingsCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AiSettingsScreen(
    settingsPreferencesManager: SettingsPreferencesManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Permission State
    var hasMicPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) 
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }
    
    var apiKeyInput by remember { mutableStateOf(settingsPreferencesManager.getApiKey() ?: "") }
    var apiKeySavedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        SectionHeader("Voice Typing")
        
        SettingsCard(
            title = "Microphone Permission",
            description = if (hasMicPermission) "Access granted" else "Tap to allow microphone access",
            icon = if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
            iconColor = if (hasMicPermission) Color(0xFF3FB950) else Color(0xFFEA4335),
            onClick = { 
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )
        
        SectionHeader("Gemini AI")
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2128).copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Text("API Key", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Required for AI translation features. Get yours from Google AI Studio.", fontSize = 14.sp, color = Color.Gray, lineHeight = 18.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Enter API Key", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8AB4F8),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF8AB4F8)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        settingsPreferencesManager.saveApiKey(apiKeyInput)
                        apiKeySavedMessage = "API Key Saved Successfully!"
                        scope.launch {
                            delay(3000)
                            apiKeySavedMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Key", fontWeight = FontWeight.Bold)
                }
                
                if (apiKeySavedMessage != null) {
                    Text(
                        text = apiKeySavedMessage!!,
                        color = Color(0xFF3FB950),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
                    )
                }
            }
        }
    }
}
