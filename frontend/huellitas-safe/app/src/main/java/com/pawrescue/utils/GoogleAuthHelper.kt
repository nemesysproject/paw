package com.pawrescue.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.pawrescue.BuildConfig
import java.util.UUID

data class GoogleSignInPayload(
    val idToken: String,
    val nonce: String,
    val email: String?,
    val displayName: String?
)

/**
 * Helper moderno de autenticación con Google usando Android Credential Manager.
 * Requiere configurar GOOGLE_WEB_CLIENT_ID en `.env` (Web Client ID de Google Cloud Console).
 */
object GoogleAuthHelper {

    suspend fun signIn(context: Context): Result<GoogleSignInPayload> {
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (serverClientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Falta configurar GOOGLE_WEB_CLIENT_ID en el archivo .env del proyecto Android."
                )
            )
        }

        val nonce = UUID.randomUUID().toString()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(
                    GoogleSignInPayload(
                        idToken = googleCredential.idToken,
                        nonce = nonce,
                        email = googleCredential.id,
                        displayName = googleCredential.displayName
                    )
                )
            } else {
                Result.failure(IllegalStateException("Tipo de credencial no compatible con Google Sign-In."))
            }
        } catch (_: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Inicio de sesión con Google cancelado."))
        } catch (_: NoCredentialException) {
            Result.failure(
                IllegalStateException(
                    "No se encontró una cuenta de Google disponible en el dispositivo."
                )
            )
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(IllegalStateException("Error interpretando el token de Google: ${e.localizedMessage}"))
        } catch (e: GetCredentialException) {
            Result.failure(IllegalStateException("No se pudo autenticar con Google: ${e.errorMessage ?: e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
