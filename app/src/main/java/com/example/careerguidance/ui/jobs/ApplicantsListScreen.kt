package com.example.careerguidance.ui.jobs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class JobApplication(
    val applicantId: String,
    val applicantName: String,
    val applicantEmail: String,
    val cvUrl: String,
    val appliedAt: Timestamp?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicantsListScreen(
    jobId: String,
    onBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var applicants by remember { mutableStateOf<List<JobApplication>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jobId) {
        loading = true
        error = null

        firestore.collection("jobs")
            .document(jobId)
            .collection("applications")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    loading = false
                    error = e.message
                    applicants = emptyList()
                    return@addSnapshotListener
                }

                val docs = snap?.documents ?: emptyList()

                // Local sort by appliedAt to keep ordering without Firestore index
                val list = docs.mapNotNull { d ->
                    val applicantId = d.getString("applicantId") ?: return@mapNotNull null
                    JobApplication(
                        applicantId = applicantId,
                        applicantName = d.getString("applicantName") ?: "",
                        applicantEmail = d.getString("applicantEmail") ?: "",
                        cvUrl = d.getString("cvUrl") ?: "",
                        appliedAt = d.getTimestamp("appliedAt")
                    )
                }.sortedByDescending { it.appliedAt?.seconds ?: 0L }

                applicants = list
                loading = false
                error = null
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Applicants") },
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
                ) { CircularProgressIndicator() }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Failed to load applicants.")
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "")
                }
            }

            applicants.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("No applicants yet.") }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(applicants) { a ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (a.applicantName.isBlank()) "Applicant" else a.applicantName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(a.applicantEmail)

                                val dateText = a.appliedAt?.toDate()?.let {
                                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(it)
                                } ?: ""

                                if (dateText.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Applied: $dateText")
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (a.cvUrl.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(a.cvUrl))
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = a.cvUrl.isNotBlank()
                                ) {
                                    Text("Open CV")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
