package com.llamacon99

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.llamacon99.ui.navigation.LlamaCon99NavGraph
import com.llamacon99.ui.theme.LlamaCon99Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LlamaCon99Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LlamaCon99NavGraph()
                }
            }
        }
    }
}
