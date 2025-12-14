package com.example.careerguidance.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
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

    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cvUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    // editing states
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    var isEditingAddress by remember { mutableStateOf(false) }
    var editedAddress by remember { mutableStateOf("") }

    var isEditingPhone by remember { mutableStateOf(false) }
    var editedPhone by remember { mutableStateOf("") }

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
                address = doc.getString("address") ?: ""
                phone = doc.getString("phone") ?: ""
                cvUrl = doc.getString("cvUrl")
                loading = false
            }
            .addOnFailureListener {
                email = auth.currentUser?.email ?: ""
                loading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (role == "company") "Company Profile" else "User Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .padding(padding)
                .padding(20.dp)
        ) {

            // EMAIL (read only)
            Text("Email", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(email, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))

            ProfileFieldWithEdit(
                label = if (role == "company") "Company Name" else "Name",
                value = name,
                isEditing = isEditingName,
                editedValue = editedName,
                onEditClick = {
                    editedName = name
                    isEditingName = true
                },
                onValueChange = { editedName = it },
                onSaveClick = {
                    if (uid != null) {
                        firestore.collection("users")
                            .document(uid)
                            .update("name", editedName)
                            .addOnSuccessListener {
                                name = editedName
                                isEditingName = false
                                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                },
                onCancelClick = {
                    editedName = name
                    isEditingName = false
                }
            )

            Spacer(Modifier.height(24.dp))

            ProfileFieldWithEdit(
                label = "Address",
                value = address,
                isEditing = isEditingAddress,
                editedValue = editedAddress,
                onEditClick = {
                    editedAddress = address
                    isEditingAddress = true
                },
                onValueChange = { editedAddress = it },
                onSaveClick = {
                    if (uid != null) {
                        firestore.collection("users")
                            .document(uid)
                            .update("address", editedAddress)
                            .addOnSuccessListener {
                                address = editedAddress
                                isEditingAddress = false
                                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                },
                onCancelClick = {
                    editedAddress = address
                    isEditingAddress = false
                }
            )

            Spacer(Modifier.height(24.dp))

            ProfileFieldWithEdit(
                label = "Phone",
                value = phone,
                isEditing = isEditingPhone,
                editedValue = editedPhone,
                onEditClick = {
                    editedPhone = phone
                    isEditingPhone = true
                },
                onValueChange = { editedPhone = it },
                onSaveClick = {
                    if (uid != null) {
                        firestore.collection("users")
                            .document(uid)
                            .update("phone", editedPhone)
                            .addOnSuccessListener {
                                phone = editedPhone
                                isEditingPhone = false
                                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                },
                onCancelClick = {
                    editedPhone = phone
                    isEditingPhone = false
                }
            )

            // CV section only for applicants
            if (role == "applicant") {
                Spacer(modifier = Modifier.height(32.dp))

                Text("Uploaded CV", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (cvUrl != null) {
                    Text("Your CV is uploaded.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

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
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

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
private fun ProfileFieldWithEdit(
    label: String,
    value: String,
    isEditing: Boolean,
    editedValue: String,
    onEditClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))

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
            value = editedValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSaveClick, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
            OutlinedButton(onClick = onCancelClick, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
    }
}
