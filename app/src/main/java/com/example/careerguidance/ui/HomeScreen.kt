package com.example.careerguidance.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onUploadCvClick: () -> Unit,
    onJobsClick: () -> Unit,
    onCreateJobClick: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()

    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        if (user != null) {
            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    role = doc.getString("role") ?: "applicant"
                    loading = false
                }
                .addOnFailureListener {
                    role = "applicant"
                    loading = false
                }
        } else {
            loading = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onLogout,
                    icon = { Icon(Icons.Filled.Logout, contentDescription = "Logout") },
                    label = { Text("Logout") }
                )
            }
        }
    ) { padding ->

        if (loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (role == "company") "Company Dashboard" else "Applicant Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onJobsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View job openings")
            }

            Spacer(Modifier.height(12.dp))

            if (role == "company") {
                Button(
                    onClick = onCreateJobClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create job opening")
                }
            } else {
                Button(
                    onClick = onUploadCvClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload CV")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onProfileClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Profile")
            }
        }
    }
}
