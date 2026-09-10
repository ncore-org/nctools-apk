package eu.nctools.app.core.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Google's User Messaging Platform (UMP) for first-run consent.
 * Calls happen from an Activity (UMP requires one for the consent form).
 */
@Singleton
class ConsentManager @Inject constructor(private val context: Context) {

    private fun info(): ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    /** True when the SDK says consent hasn't been finalized yet. */
    fun isConsentRequired(): Boolean =
        info().consentStatus == ConsentInformation.ConsentStatus.REQUIRED ||
            info().consentStatus == ConsentInformation.ConsentStatus.UNKNOWN

    /**
     * Kick off consent info update. [onReady] fires when consent state is known
     * or the request fails; the caller should then decide whether to show form.
     */
    fun requestConsent(activity: Activity, onReady: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info().requestConsentInfoUpdate(
            activity, params,
            { onReady(isConsentRequired()) },
            { onReady(isConsentRequired()) },
        )
    }

    fun loadAndShowForm(activity: Activity) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            object : UserMessagingPlatform.OnConsentFormLoadSuccessListener {
                override fun onConsentFormLoadSuccess(consentForm: com.google.android.ump.ConsentForm) {
                    consentForm.show(activity, object : com.google.android.ump.ConsentForm.OnConsentFormDismissedListener {
                        override fun onConsentFormDismissed(loadError: com.google.android.ump.FormError?) {}
                    })
                }
            },
            object : UserMessagingPlatform.OnConsentFormLoadFailureListener {
                override fun onConsentFormLoadFailure(error: com.google.android.ump.FormError) {}
            },
        )
    }
}