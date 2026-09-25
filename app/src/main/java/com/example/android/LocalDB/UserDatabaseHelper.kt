package com.example.android.LocalDB

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.security.MessageDigest

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID TEXT PRIMARY KEY NOT NULL,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL,
                $COLUMN_ACTIVATION INTEGER NOT NULL DEFAULT 0,
                $COLUMN_ACTIVATION_PENDING INTEGER NOT NULL DEFAULT 0,
                $COLUMN_DEACTIVATION_REQUESTED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_SESSION_VERSION TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(database)
    }

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun saveUser(user: JSONObject, plainPassword: String? = null) {
        val hashedPassword = if (!plainPassword.isNullOrEmpty()) {
            hashPassword(plainPassword)
        } else {
            user.optString("passwordHash")
        }

        val values = ContentValues().apply {
            put(COLUMN_ID, user.optString("_id").ifEmpty { user.optString("id") })
            put(COLUMN_USERNAME, user.optString("userName").ifEmpty { user.optString("username") })
            put(COLUMN_EMAIL, user.optString("email"))
            put(COLUMN_PASSWORD_HASH, hashedPassword)
            put(COLUMN_ROLE, user.optString("role"))
            put(COLUMN_ACTIVATION, if (user.optBoolean("activation")) 1 else 0)
            put(COLUMN_ACTIVATION_PENDING, if (user.optBoolean("activationPending")) 1 else 0)
            put(COLUMN_DEACTIVATION_REQUESTED, if (user.optBoolean("deactivationRequested")) 1 else 0)
            put(COLUMN_SESSION_VERSION, user.optString("sessionVersion"))
        }

        writableDatabase.use { database ->
            database.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun authenticateOffline(emailOrUsername: String, plainPassword: String): JSONObject? {
        val hashedPassword = hashPassword(plainPassword)
        val selection = "(LOWER($COLUMN_EMAIL) = LOWER(?) OR LOWER($COLUMN_USERNAME) = LOWER(?)) AND $COLUMN_PASSWORD_HASH = ?"
        val selectionArgs = arrayOf(emailOrUsername, emailOrUsername, hashedPassword)

        readableDatabase.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val user = JSONObject()
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val usernameIndex = cursor.getColumnIndex(COLUMN_USERNAME)
                val emailIndex = cursor.getColumnIndex(COLUMN_EMAIL)
                val passwordHashIndex = cursor.getColumnIndex(COLUMN_PASSWORD_HASH)
                val roleIndex = cursor.getColumnIndex(COLUMN_ROLE)
                val activationIndex = cursor.getColumnIndex(COLUMN_ACTIVATION)
                val activationPendingIndex = cursor.getColumnIndex(COLUMN_ACTIVATION_PENDING)
                val deactivationRequestedIndex = cursor.getColumnIndex(COLUMN_DEACTIVATION_REQUESTED)
                val sessionVersionIndex = cursor.getColumnIndex(COLUMN_SESSION_VERSION)

                if (idIndex >= 0) user.put("_id", cursor.getString(idIndex))
                if (usernameIndex >= 0) user.put("userName", cursor.getString(usernameIndex))
                if (emailIndex >= 0) user.put("email", cursor.getString(emailIndex))
                if (passwordHashIndex >= 0) user.put("passwordHash", cursor.getString(passwordHashIndex))
                if (roleIndex >= 0) user.put("role", cursor.getString(roleIndex))
                if (activationIndex >= 0) user.put("activation", cursor.getInt(activationIndex) == 1)
                if (activationPendingIndex >= 0) user.put("activationPending", cursor.getInt(activationPendingIndex) == 1)
                if (deactivationRequestedIndex >= 0) user.put("deactivationRequested", cursor.getInt(deactivationRequestedIndex) == 1)
                if (sessionVersionIndex >= 0) user.put("sessionVersion", cursor.getString(sessionVersionIndex))

                return user
            }
        }
        return null
    }

    fun isConnectionOk(): Boolean {
        return try {
            val db = readableDatabase
            db.isOpen
        } catch (e: Exception) {
            false
        }
    }

    fun getUserByEmailOrUsername(emailOrUsername: String): JSONObject? {
        val selection = "LOWER($COLUMN_EMAIL) = LOWER(?) OR LOWER($COLUMN_USERNAME) = LOWER(?)"
        val selectionArgs = arrayOf(emailOrUsername, emailOrUsername)

        readableDatabase.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val user = JSONObject()
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val usernameIndex = cursor.getColumnIndex(COLUMN_USERNAME)
                val emailIndex = cursor.getColumnIndex(COLUMN_EMAIL)
                val passwordHashIndex = cursor.getColumnIndex(COLUMN_PASSWORD_HASH)
                val roleIndex = cursor.getColumnIndex(COLUMN_ROLE)
                val activationIndex = cursor.getColumnIndex(COLUMN_ACTIVATION)
                val activationPendingIndex = cursor.getColumnIndex(COLUMN_ACTIVATION_PENDING)
                val deactivationRequestedIndex = cursor.getColumnIndex(COLUMN_DEACTIVATION_REQUESTED)
                val sessionVersionIndex = cursor.getColumnIndex(COLUMN_SESSION_VERSION)

                if (idIndex >= 0) user.put("_id", cursor.getString(idIndex))
                if (usernameIndex >= 0) user.put("userName", cursor.getString(usernameIndex))
                if (emailIndex >= 0) user.put("email", cursor.getString(emailIndex))
                if (passwordHashIndex >= 0) user.put("passwordHash", cursor.getString(passwordHashIndex))
                if (roleIndex >= 0) user.put("role", cursor.getString(roleIndex))
                if (activationIndex >= 0) user.put("activation", cursor.getInt(activationIndex) == 1)
                if (activationPendingIndex >= 0) user.put("activationPending", cursor.getInt(activationPendingIndex) == 1)
                if (deactivationRequestedIndex >= 0) user.put("deactivationRequested", cursor.getInt(deactivationRequestedIndex) == 1)
                if (sessionVersionIndex >= 0) user.put("sessionVersion", cursor.getString(sessionVersionIndex))

                return user
            }
        }
        return null
    }

    fun saveLocalUser(
        id: String,
        username: String,
        email: String,
        encryptedPassword: String,
        role: String
    ): JSONObject {
        val values = ContentValues().apply {
            put(COLUMN_ID, id.ifEmpty { "local_${System.currentTimeMillis()}" })
            put(COLUMN_USERNAME, username)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD_HASH, encryptedPassword)
            put(COLUMN_ROLE, role)
            put(COLUMN_ACTIVATION, 1)
            put(COLUMN_ACTIVATION_PENDING, 0)
            put(COLUMN_DEACTIVATION_REQUESTED, 0)
            put(COLUMN_SESSION_VERSION, "1")
        }

        writableDatabase.use { db ->
            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        return JSONObject().apply {
            put("_id", id.ifEmpty { "local_${System.currentTimeMillis()}" })
            put("userName", username)
            put("email", email)
            put("passwordHash", encryptedPassword)
            put("role", role)
            put("activation", true)
        }
    }

    companion object {
        private const val DATABASE_NAME = "solar_grid.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD_HASH = "password_hash"
        private const val COLUMN_ROLE = "role"
        private const val COLUMN_ACTIVATION = "activation"
        private const val COLUMN_ACTIVATION_PENDING = "activation_pending"
        private const val COLUMN_DEACTIVATION_REQUESTED = "deactivation_requested"
        private const val COLUMN_SESSION_VERSION = "session_version"
    }
}
