package com.example.careerguidance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.careerguidance.ui.auth.LoginScreen
import com.example.careerguidance.ui.auth.SignupScreen
import com.example.careerguidance.ui.home.HomeScreen
import com.example.careerguidance.ui.profile.ProfileScreen
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val credential = Identity.getSignInClient(this)
                    .getSignInCredentialFromIntent(result.data)

                val token = credential.googleIdToken
                val firebaseCred = GoogleAuthProvider.getCredential(token, null)

                auth.signInWithCredential(firebaseCred)
                    .addOnSuccessListener {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid

                            // Check role in Firestore – block company accounts
                            firestore.collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val role = doc.getString("role")

                                    if (role == "company") {
                                        // Company is not allowed to use Google login
                                        auth.signOut()
                                        Toast.makeText(
                                            this,
                                            "Company accounts cannot use Google sign-in. Please log in with email and password.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        // currentUser becomes null → AuthStateListener keeps you on login
                                    } else {
                                        // applicant or no role → allowed
                                        // AuthStateListener will navigate to home
                                    }
                                }
                                .addOnFailureListener {
                                    // If role doc can't be read, treat as applicant by default.
                                    // AuthStateListener will still navigate.
                                }
                        }
                    }
                    .addOnFailureListener { it.printStackTrace() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            App()
        }
    }

    @Composable
    fun App() {
        val nav = rememberNavController()
        val auth = FirebaseAuth.getInstance()

        var currentUser by remember { mutableStateOf(auth.currentUser) }

        // Listen to login / logout changes
        DisposableEffect(Unit) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
            }
            auth.addAuthStateListener(listener)

            onDispose {
                auth.removeAuthStateListener(listener)
            }
        }

        // Auto navigate when user is logged in
        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                nav.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = nav,
                startDestination = if (currentUser != null) "home" else "login"
            ) {
                composable("login") {
                    LoginScreen(
                        onGoogleLogin = { googleSignIn() },
                        onSignupClick = { nav.navigate("signup") }
                    )
                }

                composable("signup") {
                    SignupScreen(
                        onSignupSuccess = {
                            // AuthStateListener will handle navigation to home
                        },
                        onBackToLogin = { nav.navigate("login") }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onProfileClick = { nav.navigate("profile") },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            nav.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        onBack = { nav.popBackStack() },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            nav.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun googleSignIn() {
        val request = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        Identity.getSignInClient(this)
            .beginSignIn(request)
            .addOnSuccessListener {
                googleLauncher.launch(
                    IntentSenderRequest.Builder(it.pendingIntent).build()
                )
            }
            .addOnFailureListener { it.printStackTrace() }
    }
}
