package eu.nctools.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import eu.nctools.app.core.update.UpdateManager
import eu.nctools.app.data.auth.AuthRepository
import javax.inject.Inject

/**
 * nctools application entry point.
 * Initializes ads (GDPR-consent gated), session restore and the update check
 * against the nctools.backend update API.
 */
@HiltAndroidApp
class NctoolsApp : Application() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var updateManager: UpdateManager

    override fun onCreate() {
        super.onCreate()

        // Ads load only after consent (see ConsentGate). Pre-initialize AdMob.
        MobileAds.initialize(this) { }

        // Restore a persisted session and kick off a background update check.
        authRepository.restoreSession()
        updateManager.checkForUpdates()
    }
}