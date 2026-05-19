package com.example.keyboad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.keyboad.data.SettingsPreferencesManager
import com.example.keyboad.data.UserDictionaryManager
import com.example.keyboad.ui.settings.SettingsApp
import com.example.keyboad.ui.theme.KeyboadTheme

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val dictionaryManager = UserDictionaryManager(this)
        val settingsPreferencesManager = SettingsPreferencesManager(this)

        setContent {
            KeyboadTheme {
                SettingsApp(
                    dictionaryManager = dictionaryManager,
                    settingsPreferencesManager = settingsPreferencesManager
                )
            }
        }
    }
}