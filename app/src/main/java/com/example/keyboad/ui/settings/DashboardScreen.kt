package com.example.keyboad.ui.settings

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboad.ui.settings.components.SectionHeader
import com.example.keyboad.ui.settings.components.SettingsCard
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.keyboad.R
import androidx.compose.ui.layout.ContentScale

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Brand Header Logo
        Image(
            painter = painterResource(id = R.drawable.ic_logo_foreground),
            contentDescription = "SwiftMind Logo",
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SwiftMind",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Premium AI Keyboard",
            fontSize = 14.sp,
            color = Color(0xFF8AB4F8),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SETUP SECTION
        SectionHeader("System Setup")
        
        SettingsCard(
            title = "Enable Keyboard",
            description = "Grant permission in system settings",
            icon = Icons.Default.ToggleOn,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        )

        SettingsCard(
            title = "Switch Input Method",
            description = "Choose SwiftMind as your primary keyboard",
            icon = Icons.Default.KeyboardAlt,
            onClick = {
                val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        )

        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Version 2.0.1 - Powered by Gemini AI",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
