package com.example.careerguidance.ui.jobs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCreateScreen(
    onBack: () -> Unit,
    onJobPosted: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var loading by remember { mutableStateOf(false) }

    var companyName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Job") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Job title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = salary,
                onValueChange = { salary = it },
                label = { Text("Salary (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (uid == null) {
                        Toast.makeText(context, "Not logged in", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (companyName.isBlank() || title.isBlank() || location.isBlank() || description.isBlank()) {
                        Toast.makeText(context, "Please fill required fields", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    loading = true

                    val jobData = hashMapOf(
                        "companyId" to uid,
                        "companyName" to companyName.trim(),
                        "title" to title.trim(),
                        "location" to location.trim(),
                        "salary" to salary.trim(),
                        "description" to description.trim(),
                        "createdAt" to Timestamp.now()
                    )

                    firestore.collection("jobs")
                        .add(jobData)
                        .addOnSuccessListener {
                            loading = false
                            Toast.makeText(context, "Job posted", Toast.LENGTH_SHORT).show()
                            onJobPosted()
                        }
                        .addOnFailureListener { e ->
                            loading = false
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text("Post job")
            }
        }
    }
}
