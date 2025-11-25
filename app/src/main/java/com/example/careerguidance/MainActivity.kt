package com.example.careerguidance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.careerguidance.ui.auth.LoginScreen
import com.example.careerguidance.ui.auth.SignupScreen
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val credential = Identity.getSignInClient(this)
                    .getSignInCredentialFromIntent(result.data)
                val token = credential.googleIdToken

                val firebaseCred = GoogleAuthProvider.getCredential(token, null)
                auth.signInWithCredential(firebaseCred)
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            NavHost(navController = nav, startDestination = "login") {

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            nav.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onGoogleLogin = { googleSignIn() },
                        onSignupClick = { nav.navigate("signup") }
                    )
                }

                composable("signup") {
                    SignupScreen(
                        onSignupSuccess = {
                            nav.navigate("home") {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        onBackToLogin = { nav.navigate("login") }
                    )
                }

                composable("home") {
                    Text("Home Screen")
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
