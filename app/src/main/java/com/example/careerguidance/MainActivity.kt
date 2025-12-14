// File: app/src/main/java/com/example/careerguidance/MainActivity.kt
package com.example.careerguidance

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.careerguidance.data.SupabaseConfig
import com.example.careerguidance.ui.auth.LoginScreen
import com.example.careerguidance.ui.auth.SignupScreen
import com.example.careerguidance.ui.home.HomeScreen
import com.example.careerguidance.ui.jobs.JobCreateScreen
import com.example.careerguidance.ui.jobs.JobsListScreen
import com.example.careerguidance.ui.profile.ProfileScreen
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val httpClient = OkHttpClient()

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val credential = Identity.getSignInClient(this)
                    .getSignInCredentialFromIntent(result.data)

                val token = credential.googleIdToken
                val firebaseCred = GoogleAuthProvider.getCredential(token, null)

                auth.signInWithCredential(firebaseCred)
                    .addOnSuccessListener {
                        val user = auth.currentUser ?: return@addOnSuccessListener
                        val uid = user.uid

                        // Block company accounts from Google sign-in
                        firestore.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role")
                                if (role == "company") {
                                    auth.signOut()
                                    Toast.makeText(
                                        this,
                                        "Company accounts cannot use Google sign-in. Please log in with email and password.",
                                        Toast.LENGTH_LONG
                                    ).show()
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
        setContent { App() }
    }

    @androidx.compose.runtime.Composable
    fun App() {
        val nav = rememberNavController()
        val context = LocalContext.current

        var currentUser by remember { mutableStateOf(auth.currentUser) }

        // Pick a PDF and upload to Supabase, then save cvUrl in Firestore
        val uploadCvLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(context, "Not logged in", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(context, "Cannot read selected file", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            val buffer = ByteArrayOutputStream()
            inputStream.use { stream ->
                val data = ByteArray(4096)
                while (true) {
                    val n = stream.read(data)
                    if (n == -1) break
                    buffer.write(data, 0, n)
                }
            }
            val fileBytes = buffer.toByteArray()

            val uid = user.uid
            val objectPath = "cvs/$uid/${System.currentTimeMillis()}.pdf"

            val uploadUrl =
                "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/${SupabaseConfig.SUPABASE_BUCKET}/$objectPath"

            val requestBody =
                fileBytes.toRequestBody("application/pdf".toMediaType(), 0, fileBytes.size)

            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_SERVICE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_SERVICE_KEY}")
                .addHeader("Content-Type", "application/pdf")
                .addHeader("x-upsert", "true")
                .post(requestBody)
                .build()

            Thread {
                try {
                    val response: Response = httpClient.newCall(request).execute()
                    val bodyText = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                context,
                                "Upload failed: ${response.code} $bodyText",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@Thread
                    }

                    // Bucket must be public for this URL to work
                    val publicUrl =
                        "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/${SupabaseConfig.SUPABASE_BUCKET}/$objectPath"

                    val userData = mapOf(
                        "cvUrl" to publicUrl,
                        "email" to (user.email ?: "")
                    )

                    firestore.collection("users")
                        .document(uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "CV uploaded successfully",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "Upload OK, failed to save URL: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "Upload error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        }

        // Auth state listener
        DisposableEffect(Unit) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
            }
            auth.addAuthStateListener(listener)
            onDispose { auth.removeAuthStateListener(listener) }
        }

        // Navigate to home when logged in
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
                        onSignupSuccess = { /* auth listener will navigate */ },
                        onBackToLogin = { nav.navigate("login") }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onProfileClick = { nav.navigate("profile") },
                        onUploadCvClick = { uploadCvLauncher.launch(arrayOf("application/pdf")) },
                        onJobsClick = { nav.navigate("jobs") },
                        onCreateJobClick = { nav.navigate("job_create") },
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

                composable("jobs") {
                    JobsListScreen(
                        onBack = { nav.popBackStack() }
                    )
                }

                composable("job_create") {
                    JobCreateScreen(
                        onBack = { nav.popBackStack() },
                        onJobPosted = { nav.navigate("jobs") }
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
