package com.omnidapt.pd.real.network

import com.omnidapt.pd.BuildConfig
import com.omnidapt.pd.real.security.SecureSessionStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.omnidapt.pd.real.network.RefreshBody
import com.omnidapt.pd.real.security.AuthSession
import java.util.concurrent.TimeUnit

class ApiFactory(private val sessionStore: SecureSessionStore) {
    fun create(authenticated: Boolean = true, baseUrl: String = sessionStore.serverUrl): OminidaptApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .apply {
                if (authenticated) {
                    addInterceptor { chain ->
                        val token = sessionStore.load()?.accessToken
                        val request = if (token.isNullOrBlank()) {
                            chain.request()
                        } else {
                            chain.request().newBuilder()
                                .header("Authorization", "Bearer $token")
                                .build()
                        }
                        chain.proceed(request)
                    }
                    authenticator { _, response ->
                        if (responseCount(response) > 1) return@authenticator null
                        val old = sessionStore.load() ?: return@authenticator null
                        synchronized(sessionStore) {
                            val newest = sessionStore.load() ?: return@synchronized null
                            if (newest.accessToken != old.accessToken) {
                                return@synchronized response.request.newBuilder()
                                    .header("Authorization", "Bearer ${newest.accessToken}")
                                    .build()
                            }
                            val refreshApi = Retrofit.Builder()
                                .baseUrl(sessionStore.serverUrl.trimEnd('/') + "/")
                                .client(OkHttpClient())
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                                .create(OminidaptApi::class.java)
                            val token = runCatching {
                                refreshApi.refresh(RefreshBody(old.refreshToken)).execute()
                            }.getOrNull()
                            val body = token?.takeIf { it.isSuccessful }?.body()
                            if (body == null) {
                                sessionStore.clear()
                                null
                            } else {
                                sessionStore.save(
                                    AuthSession(
                                        accessToken = body.access_token,
                                        refreshToken = body.refresh_token,
                                        userId = body.user.id,
                                        username = body.user.username,
                                        displayName = body.user.display_name,
                                        role = body.user.role,
                                        mustChangePassword = body.user.must_change_password,
                                    ),
                                )
                                response.request.newBuilder()
                                    .header("Authorization", "Bearer ${body.access_token}")
                                    .build()
                            }
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        },
                    )
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OminidaptApi::class.java)
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
