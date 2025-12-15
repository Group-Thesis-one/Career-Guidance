package com.example.careerguidance.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    var role by remember { mutableStateOf("applicant") }
    var loading by remember { mutableStateOf(true) }

    // Common fields
    var email by remember { mutableStateOf(auth.currentUser?.email ?: "") }
    var name by remember { mutableStateOf("") }

    // Applicant fields (minimal set)
    var location by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("") } // stored as number; edited as text
    var goalRole by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var cvUrl by remember { mutableStateOf<String?>(null) }

    // Per-field editing state
    var editKey by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

    fun startEdit(key: String, current: String) {
        editKey = key
        editValue = current
    }

    fun cancelEdit() {
        editKey = null
        editValue = ""
    }

    fun saveEdit() {
        val key = editKey ?: return
        if (uid == null) return

        val trimmed = editValue.trim()

        if (key == "experienceYears") {
            if (trimmed.isNotBlank() && trimmed.toIntOrNull() == null) {
                Toast.makeText(context, "Experience years must be a number", Toast.LENGTH_LONG).show()
                return
            }
        }

        val valueForFirestore: Any =
            if (key == "experienceYears") (trimmed.toIntOrNull() ?: 0) else trimmed

        firestore.collection("users")
            .document(uid)
            .update(mapOf(key to valueForFirestore))
            .addOnSuccessListener {
                when (key) {
                    "name" -> name = trimmed
                    "location" -> location = trimmed
                    "educationLevel" -> educationLevel = trimmed
                    "major" -> major = trimmed
                    "experienceYears" -> experienceYears = trimmed
                    "goalRole" -> goalRole = trimmed
                    "githubUrl" -> githubUrl = trimmed
                    "linkedinUrl" -> linkedinUrl = trimmed
                }
                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                cancelEdit()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun openLink(url: String) {
        val u = url.trim()
        if (u.isBlank()) return
        val normalized = if (u.startsWith("http://") || u.startsWith("https://")) u else "https://$u"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link", Toast.LENGTH_LONG).show()
        }
    }

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
                location = doc.getString("location") ?: ""
                educationLevel = doc.getString("educationLevel") ?: ""
                major = doc.getString("major") ?: ""
                experienceYears = (doc.getLong("experienceYears") ?: 0L).toString()
                goalRole = doc.getString("goalRole") ?: ""
                githubUrl = doc.getString("githubUrl") ?: ""
                linkedinUrl = doc.getString("linkedinUrl") ?: ""
                cvUrl = doc.getString("cvUrl")

                loading = false
            }
            .addOnFailureListener {
                loading = false
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

            // EMAIL (read-only)
            SectionTitle("Email")
            Text(email, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(20.dp))

            // NAME
            ProfileFieldReadOnlyOrEdit(
                label = if (role == "company") "Company Name" else "Name",
                value = name,
                isEditing = editKey == "name",
                editValue = editValue,
                onEditClick = { startEdit("name", name) },
                onValueChange = { editValue = it },
                onSave = { saveEdit() },
                onCancel = { cancelEdit() }
            )

            Spacer(Modifier.height(16.dp))

            if (role == "applicant") {

                ProfileFieldReadOnlyOrEdit(
                    label = "Location",
                    value = location,
                    isEditing = editKey == "location",
                    editValue = editValue,
                    onEditClick = { startEdit("location", location) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() }
                )

                Spacer(Modifier.height(16.dp))

                ProfileFieldReadOnlyOrEdit(
                    label = "Education Level",
                    value = educationLevel,
                    isEditing = editKey == "educationLevel",
                    editValue = editValue,
                    onEditClick = { startEdit("educationLevel", educationLevel) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() }
                )

                Spacer(Modifier.height(16.dp))

                ProfileFieldReadOnlyOrEdit(
                    label = "Major / Field of Study",
                    value = major,
                    isEditing = editKey == "major",
                    editValue = editValue,
                    onEditClick = { startEdit("major", major) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() }
                )

                Spacer(Modifier.height(16.dp))

                ProfileFieldReadOnlyOrEdit(
                    label = "Experience Years",
                    value = experienceYears,
                    isEditing = editKey == "experienceYears",
                    editValue = editValue,
                    onEditClick = { startEdit("experienceYears", experienceYears) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() }
                )

                Spacer(Modifier.height(16.dp))

                ProfileFieldReadOnlyOrEdit(
                    label = "Goal Role",
                    value = goalRole,
                    isEditing = editKey == "goalRole",
                    editValue = editValue,
                    onEditClick = { startEdit("goalRole", goalRole) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() }
                )

                Spacer(Modifier.height(16.dp))

                ProfileLinkField(
                    label = "GitHub",
                    value = githubUrl,
                    isEditing = editKey == "githubUrl",
                    editValue = editValue,
                    onEditClick = { startEdit("githubUrl", githubUrl) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() },
                    onOpen = { openLink(githubUrl) }
                )

                Spacer(Modifier.height(16.dp))

                ProfileLinkField(
                    label = "LinkedIn",
                    value = linkedinUrl,
                    isEditing = editKey == "linkedinUrl",
                    editValue = editValue,
                    onEditClick = { startEdit("linkedinUrl", linkedinUrl) },
                    onValueChange = { editValue = it },
                    onSave = { saveEdit() },
                    onCancel = { cancelEdit() },
                    onOpen = { openLink(linkedinUrl) }
                )

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
            }

            Spacer(Modifier.height(36.dp))

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

@Composable
private fun ProfileFieldReadOnlyOrEdit(
    label: String,
    value: String,
    isEditing: Boolean,
    editValue: String,
    onEditClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))

    if (!isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isBlank()) "(not set)" else value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit $label")
            }
        }
    } else {
        OutlinedTextField(
            value = editValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        }
    }
}

@Composable
private fun ProfileLinkField(
    label: String,
    value: String,
    isEditing: Boolean,
    editValue: String,
    onEditClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit
) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))

    if (!isEditing) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isBlank()) "(not set)" else value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit $label")
                }
            }
            if (value.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open $label")
                }
            }
        }
    } else {
        OutlinedTextField(
            value = editValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("example.com or https://example.com") }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        }
    }
}
