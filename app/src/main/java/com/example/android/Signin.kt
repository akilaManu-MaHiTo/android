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

class Signin : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNic: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.active_signin)

        ApiClient.init(this)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etNic = findViewById(R.id.etNic)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        btnSignIn.setOnClickListener {
            performOnlineRegistration()
        }

        tvGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun performOnlineRegistration() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val nic = etNic.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Username is required"
            return
        }
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }
        if (nic.isEmpty()) {
            etNic.error = "NIC number is required"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }
        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Please confirm your password"
            return
        }
        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        tvError.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        btnSignIn.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Online registration via /register API (Role fixed to PROSUMER / POSUMER)
                ApiClient.register(
                    userName = username,
                    email = email,
                    plainPassword = password,
                    role = "POSUMER",
                    nic = nic
                )

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    Toast.makeText(
                        this@Signin,
                        "Registration successful! Please wait for admin activation before logging in.",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToLogin()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    tvError.text = e.message ?: "Registration failed. Please check your connection and try again."
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}
