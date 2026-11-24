package com.pawrescue.utils

import android.util.Base64
import org.json.JSONObject

data class JwtPayload(
    val userId: String?,
    val role: String,
    val expEpochSeconds: Long?
)

/**
 * Decodifica el payload Base64URL de un JWT (sin verificar firma, ya que la verificación
 * corresponde al backend) para leer `sub` (UUID real), `role` y `exp` en el cliente.
 */
object JwtUtils {
    fun decodePayload(token: String?): JwtPayload? {
        if (token.isNullOrBlank()) return null
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
            JwtPayload(
                userId = json.optString("sub").takeIf { it.isNotBlank() },
                role = json.optString("role").takeIf { it.isNotBlank() } ?: "USER",
                expEpochSeconds = if (json.has("exp")) json.optLong("exp") else null
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isExpired(token: String?, skewSeconds: Long = 30L): Boolean {
        val exp = decodePayload(token)?.expEpochSeconds ?: return false
        val nowSeconds = System.currentTimeMillis() / 1000L
        return nowSeconds + skewSeconds >= exp
    }
}
