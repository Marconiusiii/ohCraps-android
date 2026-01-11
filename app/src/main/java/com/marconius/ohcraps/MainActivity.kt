package com.marconius.ohcraps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OhCrapsApp()
        }
    }
}

@Composable
fun OhCrapsApp() {
    MaterialTheme {
        Text(text = "Oh Craps Android App")
    }
}
