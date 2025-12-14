package com.example.careerguidance.ui.jobs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
fun JobsListingScreen(
    onBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var jobs by remember { mutableStateOf<List<JobPost>>(emptyList()) }

    // Load user role
    LaunchedEffect(uid) {
        if (uid == null) {
            role = "applicant"
            loading = false
            return@LaunchedEffect
        }

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                role = doc.getString("role") ?: "applicant"
            }
            .addOnFailureListener {
                role = "applicant"
            }
    }

    // Load jobs based on role
    LaunchedEffect(role) {
        if (role == null) return@LaunchedEffect

        loading = true
        error = null

        val query = if (role == "company") {
            // No orderBy → avoids Firestore index requirement
            firestore.collection("jobs")
                .whereEqualTo("companyId", uid)
        } else {
            firestore.collection("jobs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snap, e ->
            if (e != null) {
                error = e.message
                jobs = emptyList()
                loading = false
                return@addSnapshotListener
            }

            val docs = snap?.documents ?: emptyList()

            var list = docs.mapNotNull { d ->
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

            // Local sort for company jobs
            if (role == "company") {
                list = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

            jobs = list
            loading = false
            error = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (role == "company")
                            "My Job Openings"
                        else
                            "Job Openings"
                    )
                },
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
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            jobs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (role == "company")
                            "You have no jobs created right now."
                        else
                            "No job openings available."
                    )
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
                        Card(modifier = Modifier.fillMaxWidth()) {
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
