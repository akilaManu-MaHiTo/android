package com.example.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.active_login)

        ApiClient.init(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email/Username is required"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }

        tvError.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Call POST /login API (Token response is stored automatically by ApiClient)
                ApiClient.login(email, password)

                // 2. Call GET /user API using Bearer Token header in ApiClient
                val userJson = ApiClient.getUser()
                val role = userJson.optString("role", "").uppercase()
                val userName = userJson.optString("userName", "User")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this@Login, "Welcome $userName ($role)", Toast.LENGTH_SHORT).show()

                    // Redirect based on role
                    when {
                        role == "POSUMER" || role == "PROSUMER" || role.contains("POSUMER") || role.contains("PROSUMER") -> {
                            val intent = Intent(this@Login, Posumer::class.java).apply {
                                putExtra("USER_JSON", userJson.toString())
                            }
                            startActivity(intent)
                            finish()
                        }
                        role == "GRID" || role == "GRID_OPERATOR" || role.contains("GRID") -> {
                            val intent = Intent(this@Login, GridOperator::class.java).apply {
                                putExtra("USER_JSON", userJson.toString())
                            }
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                            tvError.text = "Logged in as $userName ($role)."
                            tvError.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    tvError.text = e.message ?: "Login failed. Please try again."
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }
}
