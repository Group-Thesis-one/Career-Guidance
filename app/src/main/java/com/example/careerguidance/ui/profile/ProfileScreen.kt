package com.example.careerguidance.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.careerguidance.data.recommendation.SkillNormalizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val normalizer = remember { SkillNormalizer() }

    var role by remember { mutableStateOf("applicant") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf(auth.currentUser?.email ?: "") }

    // name lock logic
    var name by remember { mutableStateOf("") }
    var nameLocked by remember { mutableStateOf(false) }

    // applicant fields
    var location by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("") } // digits only
    var goalRole by remember { mutableStateOf("") }
    var cvUrl by remember { mutableStateOf<String?>(null) }

    // skills: multi-select
    val skillOptions = remember {
        listOf(
            "kotlin",
            "jetpack compose",
            "mvvm",
            "git",
            "rest api",
            "firebase auth",
            "firebase firestore",
            "room",
            "coroutines",
            "sql",
            "postgresql",
            "java",
            "javascript",
            "react",
            "nodejs",
            "docker",
            "linux",
            "testing",
            "api testing",
            "postman",
            "python",
            "statistics",
            "machine learning",
            "ui design",
            "ux research",
            "figma"
        )
    }
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }

    // dropdown/radio options
    val educationOptions = remember {
        listOf(
            "High School",
            "Diploma",
            "Bachelor",
            "Master",
            "PhD",
            "Other"
        )
    }

    val majorOptions = remember {
        listOf(
            "Information Technology",
            "Computer Science",
            "Software Engineering",
            "Data Science",
            "Cybersecurity",
            "Business",
            "Other"
        )
    }

    val goalRoleOptions = remember {
        listOf(
            "Android Developer",
            "Backend Developer",
            "Frontend Developer",
            "Full Stack Developer",
            "QA Engineer",
            "Data Analyst",
            "DevOps Engineer",
            "Cybersecurity Analyst",
            "UI/UX Designer",
            "Other"
        )
    }

    // UI state for menus/dialogs
    var educationExpanded by remember { mutableStateOf(false) }
    var majorExpanded by remember { mutableStateOf(false) }
    var skillsExpanded by remember { mutableStateOf(false) }
    var goalDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) {
            loading = false
            return@LaunchedEffect
        }

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                role = doc.getString("role") ?: "applicant"
                email = doc.getString("email") ?: (auth.currentUser?.email ?: "")

                name = doc.getString("name") ?: ""
                nameLocked = doc.getBoolean("nameLocked") ?: name.isNotBlank()

                // keep your existing field names (location, educationLevel, major, experienceYears, goalRole)
                location = doc.getString("location") ?: ""
                educationLevel = doc.getString("educationLevel") ?: ""
                major = doc.getString("major") ?: ""
                experienceYears = (doc.getLong("experienceYears") ?: 0L).toString()
                goalRole = doc.getString("goalRole") ?: ""
                cvUrl = doc.getString("cvUrl")

                // skills saved as array: skillsRaw
                val skillsRaw = (doc.get("skillsRaw") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                selectedSkills = skillsRaw.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

                loading = false
            }
            .addOnFailureListener { e ->
                loading = false
                Toast.makeText(context, e.message ?: "Failed to load profile", Toast.LENGTH_LONG).show()
            }
    }

    fun saveProfile() {
        if (uid == null) return

        if (!nameLocked && name.trim().isBlank()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_LONG).show()
            return
        }

        val expInt = experienceYears.trim().toIntOrNull() ?: 0

        val skillsRaw = selectedSkills.toList().sorted()
        val skillsNormalized = normalizer.normalizeAll(skillsRaw).toList().sorted()

        val data = mutableMapOf<String, Any>(
            "email" to email,
            "location" to location.trim(),
            "educationLevel" to educationLevel.trim(),
            "major" to major.trim(),
            "experienceYears" to expInt,
            "goalRole" to goalRole.trim(),
            "skillsRaw" to skillsRaw,
            "skillsNormalized" to skillsNormalized
        )

        // name can be set only once
        if (!nameLocked) {
            data["name"] = name.trim()
            data["nameLocked"] = true
        }

        saving = true

        firestore.collection("users")
            .document(uid)
            .update(data)
            .addOnSuccessListener {
                saving = false
                nameLocked = true
                Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                saving = false
                Toast.makeText(context, e.message ?: "Failed to save profile", Toast.LENGTH_LONG).show()
            }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (role == "company") "Company Profile" else "Applicant Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp)
                .padding(bottom = 24.dp)
        ) {

            SectionTitle("Email")
            Text(email, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(16.dp))

            SectionTitle(if (role == "company") "Company Name" else "Name")

            if (nameLocked) {
                Text(
                    text = if (name.isBlank()) "(not set)" else name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Name cannot be edited after it is set.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your name") }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "You can set the name only once.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))

            if (role == "applicant") {

                SectionTitle("Location")
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("City / Country") }
                )

                Spacer(Modifier.height(16.dp))

                SectionTitle("Education Level")
                ExposedDropdownMenuBox(
                    expanded = educationExpanded,
                    onExpandedChange = { educationExpanded = !educationExpanded }
                ) {
                    OutlinedTextField(
                        value = educationLevel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        placeholder = { Text("Select education level") }
                    )
                    ExposedDropdownMenu(
                        expanded = educationExpanded,
                        onDismissRequest = { educationExpanded = false }
                    ) {
                        educationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    educationLevel = option
                                    educationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionTitle("Major / Field of Study")
                ExposedDropdownMenuBox(
                    expanded = majorExpanded,
                    onExpandedChange = { majorExpanded = !majorExpanded }
                ) {
                    OutlinedTextField(
                        value = major,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        placeholder = { Text("Select major / field") }
                    )
                    ExposedDropdownMenu(
                        expanded = majorExpanded,
                        onDismissRequest = { majorExpanded = false }
                    ) {
                        majorOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    major = option
                                    majorExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionTitle("Experience Years")
                OutlinedTextField(
                    value = experienceYears,
                    onValueChange = { input ->
                        // digits only
                        experienceYears = input.filter { it.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") }
                )

                Spacer(Modifier.height(16.dp))

                SectionTitle("Goal Career")
                OutlinedTextField(
                    value = goalRole,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Select goal career") },
                    trailingIcon = {
                        TextButton(onClick = { goalDialogOpen = true }) {
                            Text("Choose")
                        }
                    }
                )

                if (goalDialogOpen) {
                    var tempSelection by remember { mutableStateOf(goalRole) }

                    AlertDialog(
                        onDismissRequest = { goalDialogOpen = false },
                        title = { Text("Select goal career") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                goalRoleOptions.forEach { option ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RadioButton(
                                            selected = tempSelection == option,
                                            onClick = { tempSelection = option }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(option)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                goalRole = tempSelection
                                goalDialogOpen = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { goalDialogOpen = false }) { Text("Cancel") }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                SectionTitle("Skills")
                ExposedDropdownMenuBox(
                    expanded = skillsExpanded,
                    onExpandedChange = { skillsExpanded = !skillsExpanded }
                ) {
                    val display = if (selectedSkills.isEmpty()) {
                        ""
                    } else {
                        "${selectedSkills.size} selected"
                    }

                    OutlinedTextField(
                        value = display,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        placeholder = { Text("Select skills") }
                    )

                    ExposedDropdownMenu(
                        expanded = skillsExpanded,
                        onDismissRequest = { skillsExpanded = false }
                    ) {
                        skillOptions.forEach { skill ->
                            val checked = selectedSkills.contains(skill)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = null
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(skill)
                                    }
                                },
                                onClick = {
                                    selectedSkills = if (checked) {
                                        selectedSkills - skill
                                    } else {
                                        selectedSkills + skill
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                SectionTitle("CV")
                if (!cvUrl.isNullOrBlank()) {
                    Text("CV uploaded.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cvUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open CV")
                    }
                } else {
                    Text("No CV uploaded yet.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Upload CV from Home screen before applying to jobs.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { saveProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                ) {
                    Text(if (saving) "Saving..." else "Save Profile")
                }
            } else {
                // company users: keep it simple for now (name + email + logout)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { saveProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                ) {
                    Text(if (saving) "Saving..." else "Save Profile")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
}
