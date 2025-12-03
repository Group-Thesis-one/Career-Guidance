package com.example.careerguidance.ui.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(
        ProfileState(loading = true)
    )
    val uiState: StateFlow<ProfileState> = _uiState

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = ProfileState(
                loading = false,
                error = "No logged-in user"
            )
            return
        }

        val uid = user.uid
        val email = user.email ?: ""

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name")
                    ?: user.displayName
                    ?: email

                val city = doc.getString("city") ?: ""
                val phone = doc.getString("phone") ?: ""
                val bio = doc.getString("bio") ?: ""

                // if nameLocked is missing, treat non-empty name as already locked
                val nameLocked = doc.getBoolean("nameLocked") ?: name.isNotBlank()

                _uiState.value = ProfileState(
                    name = name.orEmpty(),
                    email = email,
                    city = city,
                    phone = phone,
                    bio = bio,
                    nameEditable = !nameLocked,
                    loading = false,
                    error = null
                )
            }
            .addOnFailureListener { e ->
                val fallbackName = user.displayName ?: email
                _uiState.value = ProfileState(
                    name = fallbackName.orEmpty(),
                    email = email,
                    loading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
    }

    fun onNameChange(newName: String) {
        val current = _uiState.value
        if (!current.nameEditable) return   // ignore changes if locked

        _uiState.value = current.copy(
            name = newName,
            error = null,
            message = null
        )
    }

    fun onCityChange(newCity: String) {
        _uiState.value = _uiState.value.copy(
            city = newCity,
            error = null,
            message = null
        )
    }

    fun onPhoneChange(newPhone: String) {
        _uiState.value = _uiState.value.copy(
            phone = newPhone,
            error = null,
            message = null
        )
    }

    fun onBioChange(newBio: String) {
        _uiState.value = _uiState.value.copy(
            bio = newBio,
            error = null,
            message = null
        )
    }

    fun saveProfile() {
        val user = auth.currentUser
        val current = _uiState.value

        if (user == null) {
            _uiState.value = current.copy(error = "No logged-in user")
            return
        }

        // name can be set only once
        if (!current.nameEditable) {
            _uiState.value = current.copy(error = "Name can only be set once")
            return
        }

        if (current.name.isBlank()) {
            _uiState.value = current.copy(error = "Name cannot be empty")
            return
        }

        _uiState.value = current.copy(saving = true, error = null, message = null)

        val uid = user.uid

        val data = mapOf(
            "name" to current.name,
            "email" to (current.email.ifBlank { user.email ?: "" }),
            "city" to current.city,
            "phone" to current.phone,
            "bio" to current.bio,
            "createdAt" to FieldValue.serverTimestamp(),
            "nameLocked" to true               // lock name after first save
        )

        firestore.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    message = "Profile saved",
                    nameEditable = false          // lock in UI as well
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    error = e.message ?: "Failed to save profile"
                )
            }
    }
}
