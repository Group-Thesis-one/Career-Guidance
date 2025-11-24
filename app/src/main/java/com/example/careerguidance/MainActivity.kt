package com.example.careerguidance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        analytics = Firebase.analytics

        setContent {
            App()
        }
    }

    @Composable
    fun App() {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                HomeScreen(onTestAnalytics = { sendTestEvent() })
            }
        }
    }

    private fun sendTestEvent() {
        val bundle = Bundle().apply {
            putString("screen", "HomeScreen")
            putString("action", "TestButtonClicked")
        }
        analytics.logEvent("test_event", bundle)
    }
}

@Composable
fun HomeScreen(onTestAnalytics: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Welcome to Career Guidance!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onTestAnalytics
        ) {
            Text("Send Firebase Test Event")
        }
    }
}
