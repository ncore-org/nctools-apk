package eu.nctools.app

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import eu.nctools.app.core.update.UpdateManager
import eu.nctools.app.data.auth.AuthRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * nctools application entry point.
 * Initializes the PDFBox native resource loader (required before any PDF work)
 * and a global crash logger that persists the last stack trace to disk so an
 * in-field crash is diagnostic even without Play Console.
 */
@HiltAndroidApp
class NctoolsApp : Application() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var updateManager: UpdateManager

    override fun onCreate() {
        super.onCreate()

        // pdfbox-android: MUST be initialized once before any PDF op, or
        // loading fonts / running the text stripper throws and crashes the app.
        PDFBoxResourceLoader.init(this)

        // Persist the last crash so a device-side report is always available.
        installCrashLogger()

        // Ads load only after consent (see ConsentGate). Pre-initialize AdMob.
        MobileAds.initialize(this) { }

        // Restore a persisted session and kick off a background update check.
        authRepository.restoreSession()
        updateManager.checkForUpdates()
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val stampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(filesDir, "last_crash.txt")
                val body = buildString {
                    append("time: ").append(stampFormatter.format(Date())).append('\n')
                    append("thread: ").append(thread.name).append('\n')
                    append("exception: ").append(throwable.javaClass.name).append(": ").append(throwable.message).append('\n')
                    append(throwable.stackTraceToString()).append('\n')
                }
                file.writeText(body)
                Log.e("NctoolsApp", "Uncaught exception on ${thread.name}", throwable)
            } catch (_: Exception) {
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}