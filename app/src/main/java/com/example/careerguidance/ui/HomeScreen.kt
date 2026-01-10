package com.example.careerguidance.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onUploadCvClick: () -> Unit,
    onJobsClick: () -> Unit,
    onCreateJobClick: () -> Unit,
    onRecommendationsClick: () -> Unit,
    onSkillImpactClick: () -> Unit,
    onActionPlanClick: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var role by remember { mutableStateOf<String?>(null) }
    var cvUploaded by remember { mutableStateOf(false) }
    var goalRole by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    var goalDialogOpen by remember { mutableStateOf(false) }

    val goalRoleOptions = remember {
        listOf(
            "Android Developer",
            "Android Developer Intern",
            "Backend Developer (Node.js)",
            "Backend Developer (Java/Spring)",
            "Full Stack Web Developer",
            "Frontend Developer (React)",
            "QA Engineer (Manual Testing)",
            "QA Automation Engineer",
            "Junior DevOps Engineer",
            "Cloud Engineer",
            "Data Analyst",
            "Junior Data Scientist",
            "Junior Machine Learning Engineer",
            "Cybersecurity Analyst",
            "UI/UX Designer"
        )
    }

    fun loadHomeData() {
        if (user == null) {
            loading = false
            return
        }

        loading = true
        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                role = doc.getString("role") ?: "applicant"
                cvUploaded = !doc.getString("cvUrl").isNullOrBlank()
                goalRole = doc.getString("goalRole") ?: ""
                loading = false
            }
            .addOnFailureListener {
                role = "applicant"
                cvUploaded = false
                goalRole = ""
                loading = false
            }
    }

    LaunchedEffect(user) {
        loadHomeData()
    }

    fun saveGoal(selected: String) {
        val uid = user?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .update("goalRole", selected)
            .addOnSuccessListener {
                goalRole = selected
                Toast.makeText(context, "Career goal saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
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

            // 1) view job openings
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

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onProfileClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Profile")
                }

            } else {
                // 2) upload / update cv
                Button(
                    onClick = { onUploadCvClick() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (cvUploaded) "Update CV" else "Upload CV")
                }

                Spacer(Modifier.height(12.dp))

                // 3) set career goal
                Button(
                    onClick = { goalDialogOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set a career goal")
                }

                if (goalRole.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Current goal: $goalRole",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 4) career recommendations
                Button(
                    onClick = onRecommendationsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Career Recommendations")
                }

                Spacer(Modifier.height(12.dp))

                // 5) my action plan
                Button(
                    onClick = onActionPlanClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("My action plan")
                }

                Spacer(Modifier.height(12.dp))

                // 6) how we helped improve your skills
                Button(
                    onClick = onSkillImpactClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("How we helped improve your skills")
                }

                Spacer(Modifier.height(12.dp))

                // 7) go to profile
                Button(
                    onClick = onProfileClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Profile")
                }
            }
        }

        if (goalDialogOpen) {
            var temp by remember {
                mutableStateOf(
                    if (goalRole.isBlank()) goalRoleOptions.first() else goalRole
                )
            }

            AlertDialog(
                onDismissRequest = { goalDialogOpen = false },
                title = { Text("Select one career goal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        goalRoleOptions.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = temp == option,
                                    onClick = { temp = option }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(option)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            saveGoal(temp)
                            goalDialogOpen = false
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { goalDialogOpen = false }) { Text("Cancel") }
                }
            )
        }
    }
}
