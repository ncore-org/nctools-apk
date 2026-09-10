package eu.nctools.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.nctools.app.data.auth.AuthRepository
import eu.nctools.app.data.auth.AuthTokenStore
import eu.nctools.app.data.auth.UserPrefs
import eu.nctools.app.data.model.UserDto
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: UserDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val guestMode: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenStore: AuthTokenStore,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(user = repository.user.value))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.user.collect { user -> _state.value = _state.value.copy(user = user) }
        }
        viewModelScope.launch {
            userPrefs.guestMode.collect { guest ->
                _state.value = _state.value.copy(guestMode = guest)
            }
        }
    }

    fun login(email: String, password: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val ok = repository.login(email, password)
            if (ok) userPrefs.setGuestMode(false)
            _state.value = _state.value.copy(
                loading = false,
                error = repository.authError.value,
                user = if (ok) repository.user.value else _state.value.user,
            )
        }
    }

    fun register(name: String, email: String, password: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val ok = repository.register(name, email, password)
            if (ok) userPrefs.setGuestMode(false)
            _state.value = _state.value.copy(
                loading = false,
                error = repository.authError.value,
                user = if (ok) repository.user.value else _state.value.user,
            )
        }
    }

    /** Enter guest mode — every tool works on-device, no account required. */
    fun continueAsGuest() {
        viewModelScope.launch { userPrefs.setGuestMode(true) }
    }

    /** Leave guest mode and return to the sign-in screen. */
    fun exitGuest() {
        viewModelScope.launch { userPrefs.setGuestMode(false) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            userPrefs.setGuestMode(false)
            _state.value = _state.value.copy(user = null, error = null, guestMode = false)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}