package com.example.careerguidance.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
        if (uid != null) {
            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    email = doc.getString("email") ?: ""
                    name = doc.getString("name") ?: ""
                    address = doc.getString("address") ?: ""
                    phone = doc.getString("phone") ?: ""
                    cvUrl = doc.getString("cvUrl")
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                }
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("User Profile") },
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
            Text(
                text = "Email",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // NAME
            ProfileFieldWithEdit(
                label = "Name",
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
                                Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to update name: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                onCancelClick = {
                    editedName = name
                    isEditingName = false
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ADDRESS
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
                                Toast.makeText(context, "Address updated", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to update address: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                onCancelClick = {
                    editedAddress = address
                    isEditingAddress = false
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PHONE
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
                                Toast.makeText(context, "Phone updated", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to update phone: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                onCancelClick = {
                    editedPhone = phone
                    isEditingPhone = false
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CV SECTION
            Text(
                text = "Uploaded CV",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (cvUrl != null) {
                Text(
                    text = "Your CV is uploaded.",
                    style = MaterialTheme.typography.bodyMedium
                )

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
                Text(
                    text = "No CV uploaded yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium
    )
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
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit $label"
                )
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
            Button(
                onClick = onSaveClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
    }
}
