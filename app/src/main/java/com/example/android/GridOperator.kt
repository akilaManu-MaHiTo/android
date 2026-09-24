package com.example.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class GridOperator : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.active_grid_operator)

        val tvWelcomeUser = findViewById<TextView>(R.id.tvWelcomeUser)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvUserNic = findViewById<TextView>(R.id.tvUserNic)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val tvActivation = findViewById<TextView>(R.id.tvActivation)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val userJsonString = intent.getStringExtra("USER_JSON")
        if (!userJsonString.isNullOrEmpty()) {
            runCatching {
                val userJson = JSONObject(userJsonString)
                val userName = userJson.optString("userName", "User")
                val email = userJson.optString("email", "")
                val nic = userJson.optString("nic", "")
                val role = userJson.optString("role", "GRID")
                val activation = userJson.optBoolean("activation", true)

                tvWelcomeUser.text = "Welcome, $userName!"
                tvUserEmail.text = "Email: $email"
                tvUserNic.text = "NIC: $nic"
                tvUserRole.text = "Role: $role"
                tvActivation.text = "Activation: ${if (activation) "Active" else "Inactive"}"
            }
        }

        btnLogout.setOnClickListener {
            ApiClient.clearToken()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }
}
