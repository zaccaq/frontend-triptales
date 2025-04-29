package com.example.frontend_triptales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.frontend_triptales.ui.theme.navigation.TripTalesApp
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrontendtriptalesTheme {
                TripTalesApp()
            }
        }
    }
}
