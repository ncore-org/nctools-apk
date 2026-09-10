package eu.nctools.app.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.nctools.app.core.consent.ConsentManager
import eu.nctools.app.data.auth.UserPrefs
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConsentState(
    val resolved: Boolean = false,
    val adsEnabled: Boolean = false,
)

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _consent = MutableStateFlow(ConsentState())
    val consent: StateFlow<ConsentState> = _consent.asStateFlow()

    init {
        viewModelScope.launch {
            val ads = userPrefs.adsConsent.collect {}
            _consent.value = ConsentState(resolved = true, adsEnabled = false)
        }
    }

    fun acceptAll() {
        viewModelScope.launch {
            userPrefs.grantAdsConsent()
            userPrefs.grantAnalyticsConsent()
            _consent.value = ConsentState(resolved = true, adsEnabled = true)
        }
    }

    fun essentialOnly() {
        viewModelScope.launch {
            userPrefs.grantAdsConsent() // non-personalized ads only
            // analytics stays off
            _consent.value = ConsentState(resolved = true, adsEnabled = true)
        }
    }

    fun reject() {
        viewModelScope.launch {
            userPrefs.revokeAllConsent()
            _consent.value = ConsentState(resolved = true, adsEnabled = false)
        }
    }
}