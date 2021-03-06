package com.oneparchy.simplecontacts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "LoginActivity"
        private const val RC_GOOGLE_SIGN_IN = 3450
    }

    //declare view elements
    private lateinit var btnLogin: SignInButton
    private lateinit var sbView: CoordinatorLayout
    //declare Firebase Auth to use
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //init Firebase Auth
        auth = Firebase.auth

        //bind variables to view elements
        btnLogin = findViewById(R.id.btnLogin)
        sbView = findViewById(R.id.clLoginActivity)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))      //google services classpath higher than version 4.3.5 does not work for some reason - https://stackoverflow.com/questions/37810552/cannot-resolve-symbol-default-web-client-id-in-firebases-android-codelab
            .requestEmail()
            .build()

        val client: GoogleSignInClient = GoogleSignIn.getClient(this, gso)
        btnLogin.setOnClickListener {
            val signInIntent: Intent = client.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)         //Look into replacing startActivityForResult with Activity Results API
        }
    }

    // Check if user is signed in (non-null) and update UI accordingly
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    //Navigate to MainActivity if user login success
    private fun updateUI(user: FirebaseUser?) {
        if (user==null) {
            Log.w(TAG, "User is null, navigation cancelled")
            return
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    //This method runs when the Google signIn intent completes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, let user know
                Log.w(TAG, "Google sign in failed", e)
                Snackbar.make(sbView, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    //Authenticate to firebase with Google token
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Snackbar.make(sbView, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }
}