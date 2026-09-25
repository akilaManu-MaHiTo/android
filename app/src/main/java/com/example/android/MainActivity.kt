package com.example.android

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(this)

        // If a valid saved JWT token exists, check role and navigate directly
        if (checkExistingSession()) {
            return
        }

        // Connect activity_main.xml to this Activity
        setContentView(R.layout.active_welcome_page)

        // Get views from XML
        val getStartedButton = findViewById<Button>(R.id.btnGetStarted)
        val signInText = findViewById<TextView>(R.id.tvSignIn)

        // Get Started button
        getStartedButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        // Sign In
        signInText.setOnClickListener {
            val intent = Intent(this, Signin::class.java)
            startActivity(intent)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkExistingSession(): Boolean {
        val token = ApiClient.token
        if (!token.isNullOrEmpty()) {
            if (ApiClient.isTokenValid) {
                val userJson = JwtUtils.getUserFromToken(token)
                val role = userJson?.optString("role", "")?.ifEmpty { ApiClient.decodedRole.orEmpty() }.orEmpty()

                // If offline, check JWT key and redirect immediately to the roles page
                if (!isNetworkAvailable()) {
                    if (userJson != null && role.isNotEmpty()) {
                        navigateToRoleScreen(role, userJson)
                        return true
                    }
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    var finalUserJson = userJson
                    try {
                        val serverUser = ApiClient.getUser()
                        if (serverUser != null) {
                            finalUserJson = serverUser
                        }
                    } catch (e: ApiFailure) {
                        if (e.status == 401) {
                            // Token revoked or invalid server-side -> clear stored token
                            ApiClient.clearToken()
                            return@launch
                        }
                    } catch (_: Exception) {
                        // Offline or network error -> use JWT userJson
                    }

                    val finalRole = finalUserJson?.optString("role", "")?.ifEmpty { ApiClient.decodedRole.orEmpty() }.orEmpty()
                    if (finalUserJson != null && finalRole.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            navigateToRoleScreen(finalRole, finalUserJson)
                        }
                    }
                }
                return true
            } else {
                // Token timestamp expired -> delete token from storage
                ApiClient.clearToken()
            }
        }
        return false
    }

    private fun navigateToRoleScreen(role: String, userJson: JSONObject) {
        val upperRole = role.uppercase()
        when {
            upperRole == "POSUMER" || upperRole == "PROSUMER" || upperRole.contains("POSUMER") || upperRole.contains("PROSUMER") -> {
                val intent = Intent(this, Posumer::class.java).apply {
                    putExtra("USER_JSON", userJson.toString())
                }
                startActivity(intent)
                finish()
            }
            upperRole == "GRID" || upperRole == "GRID_OPERATOR" || upperRole.contains("GRID") -> {
                val intent = Intent(this, GridOperator::class.java).apply {
                    putExtra("USER_JSON", userJson.toString())
                }
                startActivity(intent)
                finish()
            }
        }
    }
}