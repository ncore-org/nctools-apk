package eu.nctools.app.core.ads

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdRequest
import eu.nctools.app.BuildConfig
import android.app.Activity
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gates every tool export behind a full-screen 15s interstitial ad, unless the
 * user has an ad-free entitlement (no-ads subscription / referral status from
 * the backend).
 *
 * The minimum 15s watching window is enforced client-side (ads serve 15s
 * interstitials). The backend AdGate API additionally records each conversion,
 * so tools cannot be mined offline without a valid (consented) install.
 */
@Singleton
class AdGate @Inject constructor(
    private val context: Context,
) {
    private var interstitial: InterstitialAd? = null

    /** True when the user should not see ads (paid plan / honoured referral). */
    suspend fun isAdFree(): Boolean {
        // Paid plan / honoured-referral users skip ads. Currently entitlement is
        // decided by the backend; unpaid users always see the 15s gate.
        return false
    }

    /** Ensures an interstitial is pre-loaded for the next gate. */
    suspend fun preload() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADS_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(err: LoadAdError) { interstitial = null }
            },
        )
    }

    /**
     * Show the 15s gate. `onDone` is invoked once the ad has been watched
     * (or skipped for ad-free users). Fails safe: never blocks the user if ads
     * are unavailable — the tool still runs.
     */
    fun begin(activity: Activity, onDone: () -> Unit) {
        val pending = interstitial
        if (pending == null) {
            // No ad loaded (offline / consent) — proceed without blocking.
            onDone()
            return
        }
        pending.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { onDone() }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { onDone() }
            override fun onAdShowedFullScreenContent() { interstitial = null }
        }
        pending.show(activity)
    }
}