package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MediaViewModel
import com.example.ui.StreamVaultApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val viewModel = MediaViewModel()
    
    setContent {
      MyApplicationTheme {
        StreamVaultApp(viewModel = viewModel)
      }
    }
  }
}
