package eu.nctools.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import eu.nctools.app.core.update.UpdateManager
import eu.nctools.app.data.auth.AuthRepository
import javax.inject.Inject

/**
 * nctools application entry point.
 * Initializes ads (GDPR-consent gated), crash reporting and the update check
 * against the nctools.backend update API.
 */
@HiltAndroidApp
class NctoolsApp : Application() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var updateManager: UpdateManager

    override fun onCreate() {
        super.onCreate()

        // Crash reporting always on (non-personalized, no consent needed).
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Ads load only after consent (see ConsentGate). Pre-initialize AdMob.
        MobileAds.initialize(this) { }

        // Kick off a background update check without blocking startup.
        updateManager.checkForUpdates()
    }
}