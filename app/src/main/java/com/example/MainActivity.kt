package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel
import com.example.ui.BrowserApp

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val initialUrl = intent?.data?.toString()
    
    enableEdgeToEdge()
    setContent {
      val viewModel: BrowserViewModel = viewModel()
      viewModel.initialize(applicationContext)
      val isDarkMode by viewModel.isDarkMode.collectAsState()
      
      MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
           androidx.compose.runtime.LaunchedEffect(initialUrl) {
               if (initialUrl != null) {
                   // if URL was sent, either open it in current active tab or create new
                   viewModel.loadUrl(initialUrl) 
               }
           }
           
           BrowserApp(viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      // Here usually we wouldn't be able to access viewModel directly.
      // But it's alright for now just handling initial start.
  }
}
