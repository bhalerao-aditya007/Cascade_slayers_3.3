package com.cascade.attentiontwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Placeholder - will be replaced with full UI
            androidx.compose.material3.Text("Attention Twin")
        }
    }
}
