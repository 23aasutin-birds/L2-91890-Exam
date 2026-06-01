package com.example.herptofauna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.herptofauna.ui.theme.HerptofaunaTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HerptofaunaTheme {
                HomePage()
                }
            }
        }
}

@Preview(
    showBackground = true
)
@Composable
fun HomePage(modifier: Modifier = Modifier) {
    Column {
        Button(onClick = { ChecklistPage() }) {
            Text("Start Survey")
        }
    }
}

@Composable
fun ChecklistPage(modifier: Modifier = Modifier) {
    // Continue here...
}