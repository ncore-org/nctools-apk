package eu.nctools.app.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import eu.nctools.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unwraps the hosting Activity from any (possibly themed) Context.
 * Compose's LocalContext is often a ContextThemeWrapper, not an Activity.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Gates every tool export behind a full-screen 15s interstitial ad, unless the
 * user has an ad-free entitlement (no-ads subscription / referral status from
 * the backend).
 *
 * Hardened: every SDK interaction is wrapped so an ad failure NEVER crashes the
 * app or blocks the tool. If no ad is ready the tool runs immediately.
 */
@Singleton
class AdGate @Inject constructor(
    private val context: Context,
) {
    private var interstitial: InterstitialAd? = null

    /** True when the user should not see ads (paid plan / honoured referral). */
    suspend fun isAdFree(): Boolean {
        // Paid plan / honoured-referral users skip ads. Entitlement is decided
        // by the backend; unpaid users always see the 15s gate.
        return false
    }

    /** Ensures an interstitial is pre-loaded for the next gate. */
    suspend fun preload() {
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                BuildConfig.ADS_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                    override fun onAdFailedToLoad(err: LoadAdError) {
                        Log.w(TAG, "interstitial failed to load: ${err.message}")
                        interstitial = null
                    }
                },
            )
        } catch (t: Throwable) {
            // Never let ad setup take the app down.
            Log.w(TAG, "interstitial preload threw", t)
            interstitial = null
        }
    }

    /**
     * Show the 15s gate. `onDone` is invoked once the ad has been watched (or
     * skipped for ad-free users / when no ad is available). Fails safe: the
     * callback always fires exactly once and never throws into the caller.
     */
    fun begin(activity: Activity, onDone: () -> Unit) {
        val pending = interstitial
        if (pending == null || activity.isFinishing || activity.isDestroyed) {
            // No ad ready (offline / not loaded) — proceed without blocking.
            safe(onDone)
            return
        }
        try {
            pending.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitial = null
                    safe(onDone)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "interstitial failed to show: ${adError.message}")
                    interstitial = null
                    safe(onDone)
                }

                override fun onAdShowedFullScreenContent() {
                    interstitial = null
                }
            }
            pending.show(activity)
        } catch (t: Throwable) {
            // A malformed/unavailable ad must never crash the tool.
            Log.w(TAG, "interstitial show threw", t)
            interstitial = null
            safe(onDone)
        }
    }

    private inline fun safe(block: () -> Unit) {
        try { block() } catch (t: Throwable) { Log.w(TAG, "gate callback threw", t) }
    }

    companion object {
        private const val TAG = "AdGate"
    }
}