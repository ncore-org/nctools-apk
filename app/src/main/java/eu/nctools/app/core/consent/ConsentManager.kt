package eu.nctools.app.core.consent

import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Google's User Messaging Platform (UMP) for first-run consent.
 * The app also applies the same decision locally (UserPrefs) so video ads only
 * initialize after the user opts in to (at least non-personalized) ads.
 */
@Singleton
class ConsentManager @Inject constructor(private val context: Context) {

    private var consentInfo: ConsentInformation? = null

    fun load() {
        val request = ConsentRequestParameters.Builder().build()
        consentInfo = UserMessagingPlatform.getConsentInformation(context)
        UserMessagingPlatform.loadConsentForm(
            context,
            onConsentFormLoadSuccess = { form ->
                if (consentInfo?.isConsentFormAvailable == true) {
                    form.show(context) { }
                }
            },
            onConsentFormLoadFailure = { },
        )
        consentInfo = UserMessagingPlatform.getConsentInformation(context)
    }
}