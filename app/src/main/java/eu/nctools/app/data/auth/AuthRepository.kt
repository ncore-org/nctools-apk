package eu.nctools.app.data.auth

import eu.nctools.app.data.api.auth.AuthApi
import eu.nctools.app.data.model.UserDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Authentication + session state shared across screens.
 * Mirrors the nctools.eu model. On login we persist the encrypted bearer token
 * and expose the current user as a StateFlow for the Compose UI.
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

    suspend fun login(email: String, password: String): Boolean {
        _authError.value = null
        return try {
            val res = api.login(AuthApi.LoginRequest(email.trim(), password))
            if (res.token != null && res.user != null) {
                persistAndSet(res.user, res.token)
                true
            } else {
                _authError.value = res.error ?: "Invalid credentials."
                false
            }
        } catch (e: Exception) {
            _authError.value = "Network error — please try again."
            false
        }
    }

    suspend fun register(name: String, email: String, password: String): Boolean {
        _authError.value = null
        return try {
            val res = api.register(AuthApi.RegisterRequest(name.trim(), email.trim(), password))
            if (res.token != null && res.user != null) {
                persistAndSet(res.user, res.token)
                true
            } else {
                _authError.value = res.error ?: "Registration failed."
                false
            }
        } catch (e: Exception) {
            _authError.value = "Network error — please try again."
            false
        }
    }

    suspend fun googleLogin(idToken: String): Boolean {
        _authError.value = null
        return try {
            val res = api.googleLogin(AuthApi.GoogleExchangeRequest(idToken))
            if (res.token != null && res.user != null) {
                persistAndSet(res.user, res.token)
                true
            } else {
                _authError.value = res.error ?: "Google sign-in failed."
                false
            }
        } catch (e: Exception) {
            _authError.value = "Google sign-in unavailable."
            false
        }
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