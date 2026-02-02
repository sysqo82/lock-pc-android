package com.lockpc.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        api = NetworkClient.create(ApiService::class.java)

        val prefs = SecurePrefs.get(this)

        val inputEmail: EditText = findViewById(R.id.inputEmail)
        val inputPassword: EditText = findViewById(R.id.inputPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val progress: ProgressBar = findViewById(R.id.progress)

        // If biometric login is enabled and we have stored credentials, prompt user
        val biometricEnabled = prefs.getBoolean(SecurePrefs.KEY_BIOMETRIC_ENABLED, false)
        val storedEmail = prefs.getString(SecurePrefs.KEY_BIOMETRIC_EMAIL, null)
        val storedPassword = prefs.getString(SecurePrefs.KEY_BIOMETRIC_PASSWORD, null)

        if (biometricEnabled && !storedEmail.isNullOrEmpty() && !storedPassword.isNullOrEmpty()) {
            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS
            ) {
                val executor = ContextCompat.getMainExecutor(this)
                val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        performLogin(storedEmail, storedPassword, btnLogin, progress, prefs, saveCredentials = false)
                    }
                })

                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Login with biometrics")
                    .setSubtitle("Use your fingerprint or face to log in")
                    .setNegativeButtonText("Cancel")
                    .build()

                prompt.authenticate(info)
            }
        }

        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password, btnLogin, progress, prefs, saveCredentials = true)
        }
    }
    private fun performLogin(
        email: String,
        password: String,
        btnLogin: Button,
        progress: ProgressBar,
        prefs: android.content.SharedPreferences,
        saveCredentials: Boolean
    ) {
        progress.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = api.login(email, password)
                val path = response.raw().request.url.encodedPath
                if (path.contains("/dashboard")) {
                    // Also request a JWT for native clients and store securely
                    try {
                        val tokenResp = api.loginToken(email, password)
                        if (tokenResp.isSuccessful) {
                            val body = tokenResp.body()
                            val token = body?.token
                            if (!token.isNullOrEmpty()) {
                                prefs.edit().putString(SecurePrefs.KEY_JWT, token).apply()
                            }
                        }
                    } catch (_: Exception) {
                    }
                    if (saveCredentials && prefs.getBoolean(SecurePrefs.KEY_BIOMETRIC_ENABLED, false)) {
                        prefs.edit()
                            .putString(SecurePrefs.KEY_BIOMETRIC_EMAIL, email)
                            .putString(SecurePrefs.KEY_BIOMETRIC_PASSWORD, password)
                            .apply()
                    }
                    Toast.makeText(this@LoginActivity, "Logged in", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
                btnLogin.isEnabled = true
            }
        }
    }
}
