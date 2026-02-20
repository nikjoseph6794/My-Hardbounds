package com.bookshlef.bookshelf

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bookshlef.bookshelf.util.MigrationManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var signInButton: SignInButton

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Handle splash screen transition
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        signInButton = findViewById(R.id.signInButton)

        // Configure Google Sign In
        val clientIdRes = resources.getIdentifier("default_web_client_id", "string", packageName)
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()

        if (clientIdRes != 0) {
            gsoBuilder.requestIdToken(getString(clientIdRes))
        } else {
            Toast.makeText(this, "Warning: web_client_id missing. Sign-in may fail.", Toast.LENGTH_LONG).show()
        }

        val gso = gsoBuilder.build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInButton.setSize(SignInButton.SIZE_WIDE)
        signInButton.setOnClickListener {
            signIn()
        }

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            handleMigrationAndProceed(currentUser.uid)
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account.idToken != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                Toast.makeText(this, "ID Token missing. Check Google Services config.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google sign in failed: ${e.message} (${e.statusCode})", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true, "Signing in...")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    handleMigrationAndProceed(user!!.uid)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleMigrationAndProceed(userId: String) {
        showLoading(true, "Migrating data to cloud...")
        lifecycleScope.launch {
            val success = MigrationManager.migrateDataToCloud(this@LoginActivity, userId)
            if (success) {
                // Determine which activity to launch next
                val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "Data migration failed. Proceeding anyway...", Toast.LENGTH_SHORT).show()
                // Proceed even if migration failed, maybe try later? For now, just let them in.
                val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showLoading(isLoading: Boolean, message: String = "") {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            statusText.text = message
            statusText.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            statusText.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
        }
    }
}
