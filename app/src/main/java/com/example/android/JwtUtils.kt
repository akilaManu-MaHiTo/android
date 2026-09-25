package com.example.android

import android.util.Base64
import org.json.JSONObject

/**
 * Utility object to parse and inspect JWT tokens.
 */
object JwtUtils {

    /**
     * Parses the payload section of a JWT token into a JSONObject.
     * Returns null if token is invalid or parsing fails.
     */
    fun parsePayload(token: String?): JSONObject? {
        if (token.isNullOrBlank()) return null
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val payloadBase64 = parts[1]
            val decodedBytes = Base64.decode(
                payloadBase64,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            JSONObject(String(decodedBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks whether the given JWT token is present and its expiration timestamp ("exp") is still valid.
     * Timestamp in JWT "exp" is in seconds since UNIX epoch.
     */
    fun isTokenValid(token: String?): Boolean {
        val payload = parsePayload(token) ?: return false
        val exp = payload.optLong("exp", -1L)
        if (exp == -1L) {
            // If token has no 'exp' claim, consider it valid if payload exists
            return true
        }
        val currentTimeInSeconds = System.currentTimeMillis() / 1000
        return currentTimeInSeconds < exp
    }

    /**
     * Extracts the user role from JWT token payload claims ("role", "roles", or nested "user.role").
     */
    fun getRole(token: String?): String? {
        val payload = parsePayload(token) ?: return null
        var role = payload.optString("role")
        if (role.isEmpty()) {
            role = payload.optString("roles")
        }
        if (role.isEmpty()) {
            role = payload.optJSONObject("user")?.optString("role").orEmpty()
        }
        return role.ifEmpty { null }
    }

    /**
     * Extracts basic user info from JWT token claims as a fallback JSONObject.
     */
    fun getUserFromToken(token: String?): JSONObject? {
        val payload = parsePayload(token) ?: return null
        if (payload.has("user")) {
            return payload.optJSONObject("user")
        }
        val user = JSONObject()
        val email = payload.optString("email").ifEmpty { payload.optString("sub") }
        val userName = payload.optString("userName").ifEmpty { payload.optString("username").ifEmpty { email } }
        val role = getRole(token).orEmpty()

        user.put("email", email)
        user.put("userName", userName)
        user.put("role", role)
        user.put("activation", payload.optBoolean("activation", true))
        return user
    }
}
