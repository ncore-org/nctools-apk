package eu.nctools.app.data.drive

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import eu.nctools.app.data.auth.UserPrefs
import java.io.File as JavaFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Google Drive integration using the auth-code flow (server reaches Drive via
 * the VPS-side token exchange). The app requests drive.file scope and uploads
 * converted documents into a private "nctools" folder.
 */
@Singleton
class DriveRepository @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val userPrefs: UserPrefs,
) {
    private val _linked = MutableStateFlow<Boolean>(false)
    val linked: StateFlow<Boolean> = _linked.asStateFlow()

    private var driveService: Drive? = null
    private var googleClient: GoogleSignInClient? = null

    init {
        // Refresh link state if we already hold an account.
        val acct = GoogleSignIn.getLastSignedInAccount(context)
        if (acct != null && userPrefs.driveLinked.first() == true) {
            _linked.value = true
            buildClient()
            buildDriveService(acct.email)
        }
    }

    private fun buildClient() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        googleClient = GoogleSignIn.getClient(context, options)
    }

    private fun buildDriveService(accountEmail: String?) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf("https://www.googleapis.com/auth/drive.file")
            ).apply { selectedAccountName = accountEmail }
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            driveService = Drive.Builder(
                transport, GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("nctools").build()
        } catch (_: Exception) {
            driveService = null
        }
    }

    /** Launch the Google account picker. Call from Activity. */
    fun signInRequest(): Intent? {
        buildClient()
        return googleClient?.signInIntent
    }

    /** Handle the Activity result from the picker. Returns true on success. */
    fun handleSignInResult(resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) return false
        val acct = GoogleSignIn.getSignedInAccountFromIntent(data).result
            ?: return false
        buildClient()
        buildDriveService(acct.email)
        _linked.value = true
        context.getSharedPreferences("nctools_prefs", Context.MODE_PRIVATE)
            .edit().putString("drive_email", acct.email ?: "").apply()
        return true
    }

    fun signOut() {
        googleClient?.signOut()
        _linked.value = false
        driveService = null
    }

    /** Upload a converted document into the private nctools folder on Drive. */
    suspend fun upload(file: JavaFile, mime: String, displayName: String): String? =
        withContext(Dispatchers.IO) {
            val svc = driveService ?: return@withContext null
            try {
                val folder = ensureFolder(svc)
                val body = File().setName(displayName)
                    .setParents(listOf(folder.id))
                    .setMimeType(mime)
                val media = FileContent(mime, file)
                val uploaded = svc.files().create(body, media).setFields("id,name").execute()
                uploaded.id
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun ensureFolder(svc: Drive): File {
        val q = "mimeType='application/vnd.google-apps.folder' and name='nctools' and trashed=false"
        val existing: FileList = svc.files().list().setQ(q).setFields("files(id,name)").execute()
        existing.files.firstOrNull()?.let { return it }
        val folder = File().setName("nctools").setMimeType("application/vnd.google-apps.folder")
        return svc.files().create(folder).setFields("id,name").execute()
    }
}