package eu.nctools.app.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import eu.nctools.app.data.model.UpdateResponse
import eu.nctools.app.data.api.NctoolsApi
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Update delivery + integrity. The VPS backend serves a signed APK descriptor
 * (url + sha256). We download to a FileProvider cache only if the SHA-256
 * matches the manifest the backend signed — so a tampered/cracked APK serving
 * a mismatched hash is rejected and never offered for install.
 */
@Singleton
class UpdateManager @Inject constructor(
    private val context: Context,
    private val api: NctoolsApi,
    private val client: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var deviceId: String? = null

    sealed interface UpdateState {
        data object Idle : UpdateState
        data class Available(val response: UpdateResponse, val enforced: Boolean) : UpdateState
        data object Downloading : UpdateState
        data class Ready(val file: File, val versionName: String) : UpdateState
        data class Failed(val reason: String) : UpdateState
    }

    fun checkForUpdates() {
        scope.launch {
            try {
                registerDevice()
                val res = api.checkUpdates(currentVersion = BuildConfig.VERSION_CODE)
                if (res.latestVersionCode > BuildConfig.VERSION_CODE) {
                    _state.value = UpdateState.Available(res, res.enforced)
                }
            } catch (e: Exception) {
                Log.w("UpdateManager", "update check failed", e)
            }
        }
    }

    private suspend fun registerDevice() {
        val id = deviceId ?: run {
            val uuid = context.getSharedPreferences("nctools_prefs", Context.MODE_PRIVATE)
                .getString("device_id", null) ?: UUID.randomUUID().toString().also { saved ->
                context.getSharedPreferences("nctools_prefs", Context.MODE_PRIVATE)
                    .edit().putString("device_id", saved).apply()
            }
            deviceId = uuid; uuid
        }
        try {
            api.registerDevice(eu.nctools.app.data.model.DeviceRegisterRequest(
                deviceId = id,
                appVersionCode = BuildConfig.VERSION_CODE,
                appVersionName = BuildConfig.VERSION_NAME,
                installToken = computeInstallToken(),
                packageName = context.packageName,
            ))
        } catch (e: Exception) { Log.w("UpdateManager", "device registration failed", e) }
    }

    /** Lightweight device fingerprint used by the backend anti-crack gate. */
    private fun computeInstallToken(): String {
        val body = "${Build.MODEL}|${Build.BRAND}|${BuildConfig.VERSION_CODE}|${context.packageName}"
        return sha256(body)
    }

    fun downloadAndInstall(state: UpdateState.Available) {
        scope.launch {
            _state.value = UpdateState.Downloading
            try {
                val apk = state.response.apk
                    ?: run { _state.value = UpdateState.Failed("No update descriptor"); return@launch }
                val file = downloadWithChecksum(apk.url, apk.sha256, apk.sizeBytes)
                _state.value = UpdateState.Ready(file, state.response.latestVersionName)
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(e.message ?: "Download failed")
            }
        }
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private suspend fun downloadWithChecksum(url: String, expectedSha: String, sizeBytes: Long): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val dest = File(dir, "nctools-release.apk").apply { delete() }
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body!!.byteStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val actual = sha256(dest.readBytes())
            if (actual != expectedSha) {
                dest.delete()
                error("Checksum mismatch — update rejected")
            }
            if (sizeBytes > 0 && dest.length() != sizeBytes) {
                dest.delete()
                error("Size mismatch — update rejected")
            }
            dest
        }

    companion object {
        fun sha256(input: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256").digest(input)
            return md.joinToString("") { "%02x".format(it) }.uppercase()
        }
    }
}