package com.pawrescue.data.local

import android.content.Context
import android.content.SharedPreferences
import com.pawrescue.utils.JwtUtils

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "huellitas_safe_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_AVATAR = "user_avatar"
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String,
        name: String,
        userId: String,
        role: String? = null,
        avatarUrl: String? = null
    ) {
        val jwt = JwtUtils.decodePayload(accessToken)
        val resolvedUserId = jwt?.userId ?: userId
        val resolvedRole = role?.takeIf { it.isNotBlank() } ?: jwt?.role ?: "USER"

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_ID, resolvedUserId)
            putString(KEY_USER_ROLE, resolvedRole)
            if (!avatarUrl.isNullOrBlank()) {
                putString(KEY_USER_AVATAR, avatarUrl)
            }
            apply()
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        val jwt = JwtUtils.decodePayload(accessToken)
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            jwt?.userId?.let { putString(KEY_USER_ID, it) }
            jwt?.role?.let { putString(KEY_USER_ROLE, it) }
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "USER") ?: "USER"

    fun getUserAvatar(): String? = prefs.getString(KEY_USER_AVATAR, null)

    fun isAccessTokenValid(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrBlank() && !JwtUtils.isExpired(token)
    }

    fun hasRefreshToken(): Boolean = !getRefreshToken().isNullOrBlank()

    fun isLoggedIn(): Boolean = isAccessTokenValid() || hasRefreshToken()

    fun clearTokens() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
