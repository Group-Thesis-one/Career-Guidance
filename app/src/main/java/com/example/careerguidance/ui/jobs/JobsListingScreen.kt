package com.example.careerguidance.ui.jobs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class JobPost(
    val id: String,
    val companyId: String,
    val companyName: String,
    val title: String,
    val location: String,
    val salary: String,
    val description: String,
    val createdAt: Timestamp?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsListScreen(
    onBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var jobs by remember { mutableStateOf<List<JobPost>>(emptyList()) }

    LaunchedEffect(Unit) {
        firestore.collection("jobs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    loading = false
                    error = e.message
                    return@addSnapshotListener
                }

                val docs = snap?.documents ?: emptyList()
                jobs = docs.mapNotNull { d ->
                    val companyId = d.getString("companyId") ?: return@mapNotNull null
                    JobPost(
                        id = d.id,
                        companyId = companyId,
                        companyName = d.getString("companyName") ?: "",
                        title = d.getString("title") ?: "",
                        location = d.getString("location") ?: "",
                        salary = d.getString("salary") ?: "",
                        description = d.getString("description") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                }

                loading = false
                error = null
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Job Openings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp)
                ) {
                    Text("Failed to load jobs:")
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobs) { job ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(job.title, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(4.dp))
                                Text(job.companyName, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("Location: ${job.location}")
                                if (job.salary.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Salary: ${job.salary}")
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(job.description)
                            }
                        }
                    }
                }
            }
        }
    }
}
