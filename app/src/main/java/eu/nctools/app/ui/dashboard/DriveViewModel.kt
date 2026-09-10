package eu.nctools.app.ui.dashboard

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.nctools.app.data.drive.DriveRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DriveViewModel @Inject constructor(
    private val driveRepository: DriveRepository,
) : ViewModel() {

    val linked: StateFlow<Boolean> get() = driveRepository.linked

    fun signInIntent(): Intent? = driveRepository.signInRequest()

    fun handleResult(resultCode: Int, data: Intent?): Boolean =
        driveRepository.handleSignInResult(resultCode, data)

    fun signOut() = driveRepository.signOut()
}