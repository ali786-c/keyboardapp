package com.example.keyboad.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.keyboad.data.SettingsPreferencesManager
import com.example.keyboad.data.UserDictionaryManager

enum class SettingsScreenType {
    DASHBOARD, AI_SETTINGS, LANGUAGE_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsApp(
    dictionaryManager: UserDictionaryManager,
    settingsPreferencesManager: SettingsPreferencesManager
) {
    var currentScreen by remember { mutableStateOf(SettingsScreenType.DASHBOARD) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1117), Color(0xFF161B22))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            SettingsScreenType.DASHBOARD -> "Home"
                            SettingsScreenType.AI_SETTINGS -> "AI & Voice"
                            SettingsScreenType.LANGUAGE_SETTINGS -> "Languages"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D1117),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == SettingsScreenType.DASHBOARD,
                    onClick = { currentScreen = SettingsScreenType.DASHBOARD },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8AB4F8),
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color(0xFF8AB4F8),
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF21262D)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                    label = { Text("AI & Voice") },
                    selected = currentScreen == SettingsScreenType.AI_SETTINGS,
                    onClick = { currentScreen = SettingsScreenType.AI_SETTINGS },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8AB4F8),
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color(0xFF8AB4F8),
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF21262D)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Language, contentDescription = "Languages") },
                    label = { Text("Languages") },
                    selected = currentScreen == SettingsScreenType.LANGUAGE_SETTINGS,
                    onClick = { currentScreen = SettingsScreenType.LANGUAGE_SETTINGS },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8AB4F8),
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color(0xFF8AB4F8),
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF21262D)
                    )
                )
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(gradient)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    SettingsScreenType.DASHBOARD -> DashboardScreen()
                    SettingsScreenType.AI_SETTINGS -> AiSettingsScreen(
                        settingsPreferencesManager = settingsPreferencesManager
                    )
                    SettingsScreenType.LANGUAGE_SETTINGS -> LanguageSettingsScreen(
                        dictionaryManager = dictionaryManager
                    )
                }
            }
        }
    }
}
