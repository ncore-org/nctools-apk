package eu.nctools.app.data.auth

import eu.nctools.app.data.api.auth.AuthApi
import eu.nctools.app.data.model.UserDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

/**
 * Authentication + session state shared across screens.
 * Mirrors the nctools.eu model. On login we persist the encrypted bearer token
 * and expose the current user as a StateFlow for the Compose UI.
 *
 * Error handling distinguishes transport failures (no internet / DNS) from
 * server-side rejections (401 invalid credentials, 409 email taken, ...) so the
 * UI can show an actionable message instead of a generic "network error".
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: AuthTokenStore,
) {
    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val isAuthenticated: Boolean get() = _user.value != null

    init {
        hydrate()
    }

    private fun hydrate() {
        val token = tokenStore.getToken()
        if (token != null) {
            _user.value = UserDto(
                id = tokenStore.getUserId(),
                email = tokenStore.getUserEmail(),
                name = tokenStore.getUserEmail()?.substringBefore("@"),
            )
        }
    }

    /** Called at cold start: restore a persisted session (non-blocking). */
    fun restoreSession() = hydrate()

    suspend fun login(email: String, password: String): Boolean {
        _authError.value = null
        return runAuth("Sign-in failed.") {
            api.login(AuthApi.LoginRequest(email.trim().lowercase(), password))
        }
    }

    suspend fun register(name: String, email: String, password: String): Boolean {
        _authError.value = null
        return runAuth("Registration failed.") {
            api.register(AuthApi.RegisterRequest(name.trim(), email.trim().lowercase(), password))
        }
    }

    suspend fun googleLogin(idToken: String): Boolean {
        _authError.value = null
        return runAuth("Google sign-in failed.") {
            api.googleLogin(AuthApi.GoogleExchangeRequest(idToken))
        }
    }

    /**
     * Shared request pipeline: persists the session on success, maps transport
     * vs. server errors to a human message on failure.
     */
    private suspend fun runAuth(
        fallback: String,
        call: suspend () -> AuthApi.AuthResponse,
    ): Boolean {
        return try {
            val res = call()
            if (res.token != null && res.user != null) {
                persistAndSet(res.user, res.token)
                true
            } else {
                _authError.value = res.error?.humanize() ?: fallback
                false
            }
        } catch (e: HttpException) {
            // Server responded with 4xx/5xx — read the structured error code.
            _authError.value = e.serverError()?.humanize()
                ?: when (e.code()) {
                    401 -> "Incorrect email or password."
                    409 -> "That email is already registered."
                    403 -> "This account has been disabled."
                    in 500..599 -> "nctools is temporarily unavailable. Try again shortly."
                    else -> "$fallback (error ${e.code()})"
                }
            false
        } catch (e: IOException) {
            // Transport-level: no connectivity, DNS failure, TLS problem.
            _authError.value = "Can't reach nctools.eu. Check your internet connection and try again."
            false
        } catch (e: Exception) {
            _authError.value = fallback
            false
        }
    }

    /** Extract the `error` code from an error response body, e.g. {"error":"email-taken"}. */
    private fun HttpException.serverError(): String? = try {
        val body = response()?.errorBody()?.string()
        body?.let { Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
    } catch (_: Exception) {
        null
    }

    /** Map backend error codes to user-facing English messages. */
    private fun String.humanize(): String = when (this) {
        "invalid-credentials" -> "Incorrect email or password."
        "email-taken" -> "That email is already registered — try signing in."
        "weak-password" -> "Your password must be at least 8 characters."
        "invalid-email" -> "Please enter a valid email address."
        "account-disabled" -> "This account has been disabled."
        "missing-fields" -> "Please fill in every field."
        "invalid-json" -> "Something went wrong. Please try again."
        else -> replace('-', ' ').replaceFirstChar { it.uppercase() } + "."
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenStore.clear()
        _user.value = null
    }

    private fun persistAndSet(user: UserDto, token: String) {
        tokenStore.save(token, user.id ?: "", user.email ?: "")
        _user.value = user
    }

    /** Returns true when a session exists on the backend. */
    suspend fun refreshSession(): Boolean {
        val token = tokenStore.getToken() ?: return false
        return try {
            val res = api.session()
            if (res.user != null) { _user.value = res.user; true } else false
        } catch (_: Exception) { false }
    }
}