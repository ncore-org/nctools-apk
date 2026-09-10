package eu.nctools.app.data.api.auth

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import eu.nctools.app.data.model.UserDto

/**
 * Auth API mirroring nctools.eu endpoints under /api/auth. The mobile app shares the same
 * local PostgreSQL account system, so a user logged in on the web is a user in
 * the app. Passwords are validated server-side with the same scrypt scheme.
 */
interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/session")
    suspend fun session(): SessionResponse

    @POST("api/mobile/auth/google")
    suspend fun googleLogin(@Body body: GoogleExchangeRequest): AuthResponse

    data class RegisterRequest(val name: String, val email: String, val password: String)
    data class LoginRequest(val email: String, val password: String)
    data class GoogleExchangeRequest(val idToken: String)

    data class AuthResponse(val user: UserDto?, val token: String?, val error: String?)
    data class SessionResponse(val user: UserDto?)
}