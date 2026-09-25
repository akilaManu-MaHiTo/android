package com.example.android

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    // 16-byte secret key for AES encryption
    private const val SECRET_KEY_16_BYTES = "SolarGridSecKey1" // 16 chars = 128 bits
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

    private fun getSecretKey(): SecretKeySpec {
        return SecretKeySpec(SECRET_KEY_16_BYTES.toByteArray(Charsets.UTF_8), ALGORITHM)
    }

    /**
     * Encrypts a plain text password using AES and returns Base64 string.
     */
    fun encryptPassword(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * Decrypts an encrypted Base64 password string back to plain text.
     * If decryption fails or text is not Base64 encrypted, returns original text.
     */
    fun decryptPassword(encryptedText: String): String {
        return try {
            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey())
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Fallback to original text if not encrypted or decryption fails
            encryptedText
        }
    }
}
