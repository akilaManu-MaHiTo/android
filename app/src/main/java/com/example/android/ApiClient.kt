package com.example.android

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Global API Client that wraps AccountApi, manages token persistence,
 * uses BuildConfig.API_BASE_URL for backend communication,
 * and attaches "Authorization: Bearer <token>" header to all requests.
 */
object ApiClient {
    private const val PREF_NAME = "solar_grid_prefs"
    private const val KEY_TOKEN = "auth_token"

    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    val serverUrl: String
        get() = BuildConfig.API_BASE_URL

    var token: String?
        get() = sharedPreferences?.getString(KEY_TOKEN, null)
        set(value) {
            sharedPreferences?.edit()?.putString(KEY_TOKEN, value)?.apply()
        }

    fun clearToken() {
        token = null
    }

    /**
     * Connects using AccountApi instance and passes the global Bearer token.
     */
    fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        requiresAuth: Boolean = true
    ): JSONObject {
        val activeToken = if (requiresAuth) token else null
        val accountApi = AccountApi(serverUrl)

        return try {
            accountApi.request(path, method, body, activeToken)
        } catch (e: ApiFailure) {
            if (e.status == 404 && path.startsWith("/api/")) {
                accountApi.request(path.removePrefix("/api"), method, body, activeToken)
            } else {
                throw e
            }
        }
    }

    /**
     * Calls POST /login via AccountApi and stores the returned Bearer token.
     */
    fun login(emailOrUser: String, pass: String): JSONObject {
        val body = JSONObject().apply {
            put("email", emailOrUser)
            put("username", emailOrUser)
            put("password", pass)
        }
        val response = request("/login", method = "POST", body = body, requiresAuth = false)

        // Store token from login response
        val extractedToken = response.optString("token").ifEmpty {
            response.optString("accessToken").ifEmpty {
                response.optString("jwt").ifEmpty {
                    response.optJSONObject("data")?.optString("token").orEmpty()
                }
            }
        }
        if (extractedToken.isNotEmpty()) {
            token = extractedToken
        }
        return response
    }

    /**
     * Calls GET /user via AccountApi with stored Bearer token in Authorization header.
     */
    fun getUser(): JSONObject {
        return request("/user", method = "GET", requiresAuth = true)
    }
}
