package eu.nctools.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import eu.nctools.app.ui.AppRoot
import eu.nctools.app.ui.theme.NctoolsTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Branded OS-level splash (icon on brand background) that seamlessly
        // hands off to the Compose animated intro.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NctoolsTheme {
                AppRoot()
            }
        }
    }
}