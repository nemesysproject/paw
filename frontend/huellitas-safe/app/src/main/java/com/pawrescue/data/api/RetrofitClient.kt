package com.pawrescue.data.api

import android.content.Context
import com.pawrescue.BuildConfig
import com.pawrescue.data.local.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(context.applicationContext).create(ApiService::class.java).also { apiService = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val sessionManager = SessionManager(context)
        val refreshLock = Any()

        // Cliente limpio (sin Authenticator) para renovar tokens sin bucles infinitos
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath
            val isPublicAuthRoute = path.endsWith("/auth/login") ||
                path.endsWith("/auth/register") ||
                path.endsWith("/auth/social") ||
                path.endsWith("/auth/refresh")

            val token = sessionManager.getAccessToken()
            val requestBuilder = original.newBuilder()
            if (!isPublicAuthRoute && !token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        // Renovación automática de JWT cuando el backend responde 401 Unauthorized
        val tokenAuthenticator = Authenticator { _, response ->
            val path = response.request.url.encodedPath
            if (path.contains("/api/v1/auth/")) {
                return@Authenticator null
            }
            // Evitar reintentos infinitos (máximo 1 reintento con refresh)
            if (response.priorResponse != null) {
                return@Authenticator null
            }

            synchronized(refreshLock) {
                val currentToken = sessionManager.getAccessToken()
                val failedAuthHeader = response.request.header("Authorization")

                // Si otro hilo ya refrescó el token, reintentar con el nuevo
                if (!currentToken.isNullOrBlank() && failedAuthHeader != "Bearer $currentToken") {
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = sessionManager.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    sessionManager.clearTokens()
                    return@Authenticator null
                }

                val payload = JSONObject().put("refresh_token", refreshToken).toString()
                val refreshRequest = Request.Builder()
                    .url("${BASE_URL}api/v1/auth/refresh")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                try {
                    refreshClient.newCall(refreshRequest).execute().use { refreshResp ->
                        if (refreshResp.isSuccessful) {
                            val bodyStr = refreshResp.body?.string().orEmpty()
                            val json = JSONObject(bodyStr)
                            val newAccess = json.optString("access_token")
                            val newRefresh = json.optString("refresh_token", refreshToken)
                            if (newAccess.isNotBlank()) {
                                sessionManager.updateTokens(newAccess, newRefresh)
                                return@Authenticator response.request.newBuilder()
                                    .header("Authorization", "Bearer $newAccess")
                                    .build()
                            }
                        }
                        sessionManager.clearTokens()
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
