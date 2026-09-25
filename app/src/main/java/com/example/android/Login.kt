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
import com.example.android.LocalDB.UserDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
        val tvGoToSignin = findViewById<TextView>(R.id.tvGoToSignin)

        btnLogin.setOnClickListener {
            performLogin()
        }

        tvGoToSignin.setOnClickListener {
            val intent = Intent(this, Signin::class.java)
            startActivity(intent)
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
            val dbHelper = UserDatabaseHelper(this@Login)
            try {
                // 1. Call POST /login API (Token response is stored automatically by ApiClient)
                ApiClient.login(email, password)

                // 2. Call GET /user API using Bearer Token header in ApiClient
                val userJson = ApiClient.getUser()

                // Save user to local SQLite DB with password hash computed from password textbox input
                dbHelper.saveUser(userJson, plainPassword = password)

                val role = userJson.optString("role", "")
                val userName = userJson.optString("userName", "User")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this@Login, "Welcome $userName ($role)", Toast.LENGTH_SHORT).show()
                    navigateToRoleScreen(role, userJson)
                }
            } catch (e: Exception) {
                // Online login failed: Fall back to offline login via local SQLite credentials
                val offlineUser = dbHelper.authenticateOffline(email, password)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    if (offlineUser != null) {
                        val role = offlineUser.optString("role", "")
                        val userName = offlineUser.optString("userName", "User")
                        Toast.makeText(this@Login, "Welcome $userName ($role) [Offline Mode]", Toast.LENGTH_SHORT).show()
                        navigateToRoleScreen(role, offlineUser)
                    } else {
                        tvError.text = e.message ?: "Login failed. Please check your credentials or network connection."
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun navigateToRoleScreen(role: String, userJson: JSONObject) {
        val upperRole = role.uppercase()
        when {
            upperRole == "POSUMER" || upperRole == "PROSUMER" || upperRole.contains("POSUMER") || upperRole.contains("PROSUMER") -> {
                val intent = Intent(this@Login, Posumer::class.java).apply {
                    putExtra("USER_JSON", userJson.toString())
                }
                startActivity(intent)
                finish()
            }
            upperRole == "GRID" || upperRole == "GRID_OPERATOR" || upperRole.contains("GRID") -> {
                val intent = Intent(this@Login, GridOperator::class.java).apply {
                    putExtra("USER_JSON", userJson.toString())
                }
                startActivity(intent)
                finish()
            }
            else -> {
                val userName = userJson.optString("userName", "User")
                tvError.text = "Logged in as $userName ($role)."
                tvError.visibility = View.VISIBLE
            }
        }
    }
}
