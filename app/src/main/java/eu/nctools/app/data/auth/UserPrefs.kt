package eu.nctools.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "nctools_prefs")

/**
 * Account/consent preferences stored via DataStore (non-secret).
 * The session token lives in AuthTokenStore (encrypted); this holds consent,
 * drive-linking flags and the last status.
 */
@Singleton
class UserPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.preferencesDataStore

    private object Keys {
        val adsConsent = booleanPreferencesKey("ads_consent_granted")
        val analyticsConsent = booleanPreferencesKey("analytics_consent_granted")
        val driveLinked = booleanPreferencesKey("drive_linked")
        val driveEmail = stringPreferencesKey("drive_email")
        val lastTool = stringPreferencesKey("last_tool")
    }

    val adsConsent: Flow<Boolean> = store.data.map { it[Keys.adsConsent] ?: false }
    val analyticsConsent: Flow<Boolean> = store.data.map { it[Keys.analyticsConsent] ?: false }
    val driveLinked: Flow<Boolean> = store.data.map { it[Keys.driveLinked] ?: false }
    val driveEmail: Flow<String?> = store.data.map { it[Keys.driveEmail] }

    suspend fun grantAdsConsent() { store.edit { it[Keys.adsConsent] = true } }
    suspend fun grantAnalyticsConsent() { store.edit { it[Keys.analyticsConsent] = true } }
    suspend fun revokeAllConsent() {
        store.edit {
            it[Keys.adsConsent] = false
            it[Keys.analyticsConsent] = false
        }
    }
    suspend fun setDriveLinked(email: String?) {
        store.edit {
            it[Keys.driveLinked] = email != null
            if (email == null) it.remove(Keys.driveEmail) else it[Keys.driveEmail] = email
        }
    }
    suspend fun setLastTool(tool: String) { store.edit { it[Keys.lastTool] = tool } }
}