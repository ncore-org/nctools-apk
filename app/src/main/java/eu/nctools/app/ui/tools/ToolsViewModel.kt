package eu.nctools.app.ui.tools

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.nctools.app.core.ads.AdGate
import eu.nctools.app.core.tools.ToolCatalog
import eu.nctools.app.core.tools.ToolsEngine
import eu.nctools.app.data.api.NctoolsApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ToolUiState(
    val slug: String = "",
    val running: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val engine: ToolsEngine,
    private val adGate: AdGate,
    private val api: NctoolsApi,
) : ViewModel() {

    private val _state = MutableStateFlow(ToolUiState())
    val state: StateFlow<ToolUiState> = _state.asStateFlow()

    /** Exposed so the UI can build concrete conversion lambdas. */
    fun toolEngine(): ToolsEngine = engine

    val title: String get() = ToolCatalog.bySlug(_state.value.slug)?.title ?: ""

    fun configure(slug: String) {
        _state.value = _state.value.copy(slug = slug)
    }

    /**
     * Start the requested conversion. Every interaction is gated behind one
     * 15s interstitial ad (AdGate), which runs the tool once dismissed.
     * Fails safe: if ads are unavailable the tool still runs.
     */
    fun convert(activity: Activity, conversion: suspend () -> ToolsEngine.Result) {
        if (_state.value.running) return
        _state.value = _state.value.copy(running = true, error = null, resultPath = null)
        adGate.begin(activity) {
            viewModelScope.launch {
                try {
                    val res = conversion()
                    _state.value = _state.value.copy(resultPath = res.output.absolutePath)
                    reportEvent(adShown = true)
                } catch (e: Exception) {
                    _state.value = _state.value.copy(error = e.message ?: "Conversion failed. Please try again.")
                } finally {
                    _state.value = _state.value.copy(running = false)
                }
            }
        }
    }

    fun clearResult() { _state.value = _state.value.copy(resultPath = null) }

    private fun reportEvent(adShown: Boolean) {
        viewModelScope.launch {
            try {
                api.reportEvent(NctoolsApi.ReportEventRequest(_state.value.slug, adShown, 15_000))
            } catch (_: Exception) {}
        }
    }
}