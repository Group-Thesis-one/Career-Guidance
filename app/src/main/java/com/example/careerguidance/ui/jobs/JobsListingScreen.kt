package com.example.careerguidance.ui.jobs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onBack: () -> Unit,
    onViewApplicants: (jobId: String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val context = LocalContext.current

    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var jobs by remember { mutableStateOf<List<JobPost>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // applicant profile data
    var applicantCvUrl by remember { mutableStateOf<String?>(null) }
    var applicantName by remember { mutableStateOf("") }
    var applicantEmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }

    // track jobs already applied to
    val appliedJobs = remember { mutableStateMapOf<String, Boolean>() }

    // load user role and applicant info
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
                applicantCvUrl = doc.getString("cvUrl")
                applicantName = doc.getString("name") ?: ""
                applicantEmail = doc.getString("email") ?: (auth.currentUser?.email ?: "")
            }
            .addOnFailureListener { e ->
                role = "applicant"
                error = e.message
            }
    }

    // load jobs
    LaunchedEffect(role) {
        if (role == null) return@LaunchedEffect

        loading = true
        error = null

        val query = if (role == "company") {
            firestore.collection("jobs")
                .whereEqualTo("companyId", uid)
        } else {
            firestore.collection("jobs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snap, e ->
            if (e != null) {
                loading = false
                error = e.message
                jobs = emptyList()
                return@addSnapshotListener
            }

            val docs = snap?.documents ?: emptyList()

            val list = docs.mapNotNull { d ->
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

            jobs = list
            loading = false

            // check which jobs applicant already applied to
            if (role == "applicant" && uid != null) {
                appliedJobs.clear()
                list.forEach { job ->
                    firestore.collection("jobs")
                        .document(job.id)
                        .collection("applications")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            appliedJobs[job.id] = doc.exists()
                        }
                }
            }
        }
    }

    fun applyToJob(job: JobPost) {
        val currentUid = auth.currentUser?.uid ?: return

        if (applicantCvUrl.isNullOrBlank()) {
            Toast.makeText(
                context,
                "Upload your CV in Profile before applying.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val applicationData = hashMapOf(
            "applicantId" to currentUid,
            "applicantName" to applicantName,
            "applicantEmail" to applicantEmail,
            "cvUrl" to applicantCvUrl,
            "appliedAt" to Timestamp.now(),
            "jobId" to job.id,
            "companyId" to job.companyId
        )

        firestore.collection("jobs")
            .document(job.id)
            .collection("applications")
            .document(currentUid)
            .set(applicationData)
            .addOnSuccessListener {
                appliedJobs[job.id] = true
                Toast.makeText(context, "Applied successfully", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Apply failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (role == "company") "My Job Openings" else "Job Openings") },
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
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load jobs.")
                }
            }

            jobs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
                        val alreadyApplied = appliedJobs[job.id] == true

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        job.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (alreadyApplied) {
                                        Text(
                                            text = "Applied",
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = MaterialTheme.shapes.small
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

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

                                Spacer(Modifier.height(14.dp))

                                if (role == "applicant") {
                                    Button(
                                        onClick = { applyToJob(job) },
                                        enabled = !alreadyApplied,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (alreadyApplied)
                                                "Already applied"
                                            else
                                                "Apply"
                                        )
                                    }
                                }

                                if (role == "company") {
                                    Button(
                                        onClick = { onViewApplicants(job.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("View applicants")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
