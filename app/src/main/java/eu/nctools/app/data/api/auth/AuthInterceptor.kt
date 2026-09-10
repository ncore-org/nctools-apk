package eu.nctools.app.data.api.auth

import okhttp3.Interceptor
import okhttp3.Response
import eu.nctools.app.data.auth.AuthTokenStore

/**
 * Adds the session bearer token to authorized requests.
 * Backend auth matches the nctools.eu session model (sha256-validated httpOnly
 * bearer tokens). Anonymous calls simply omit the header.
 */
class AuthInterceptor(private val tokenStore: AuthTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()
        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}