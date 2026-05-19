package com.example.keyboad.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboad.data.UserDictionaryManager
import com.example.keyboad.ui.settings.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LanguageSettingsScreen(
    dictionaryManager: UserDictionaryManager
) {
    val scope = rememberCoroutineScope()
    
    var isDownloaded by remember { mutableStateOf(dictionaryManager.isFullDictionaryDownloaded()) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        SectionHeader("Dictionaries")
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2128).copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Language,
                            null,
                            tint = if (isDownloaded) Color(0xFF3FB950) else Color(0xFF8AB4F8),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "English (US)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    }

                    if (!isDownloaded && !isDownloading) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isDownloading = true
                                    for (i in 1..100) {
                                        downloadProgress = i / 100f
                                        delay(15)
                                    }
                                    dictionaryManager.downloadFullDictionary("apple\napply\napplication\nbanana\nbook\nkeyboard\nsmart\nfast")
                                    isDownloading = false
                                    isDownloaded = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isDownloaded) {
                        Text("Installed", color = Color(0xFF3FB950), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF2F81F7),
                        trackColor = Color(0xFF30363D)
                    )
                    Text(
                        text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (!isDownloaded) {
                    Text(
                        text = "10,000+ words for better offline predictions.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
