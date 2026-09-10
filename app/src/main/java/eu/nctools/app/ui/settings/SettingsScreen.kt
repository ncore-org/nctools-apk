package eu.nctools.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.nctools.app.BuildConfig
import eu.nctools.app.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    isGuest: Boolean,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val verify = remember { false }
    val adsStatus = if (verify) "Ad-supported" else "Free, ad-supported"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Account row
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isGuest) "Guest mode" else (state.user?.name ?: state.user?.email ?: "Signed in"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (isGuest) "All tools work — sign in to sync to the cloud."
                            else state.user?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isGuest) {
                        Button(onClick = onSignIn) { Text("Sign in") }
                    } else {
                        OutlinedButton(onClick = { viewModel.logout() }) { Text("Sign out") }
                    }
                }
            }

            // Appearance
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    LabeledRow(icon = Icons.Filled.Palette, title = "Theme") {
                        Text("System default", style = MaterialTheme.typography.bodyMedium)
                    }
                    LabeledRow(icon = Icons.Filled.Language, title = "App language") {
                        Text("Detect (system)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Storage & privacy
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    LabeledRow(icon = Icons.Filled.Storage, title = "Storage") {
                        Text("On-device only", style = MaterialTheme.typography.bodyMedium)
                    }
                    LabeledRow(icon = Icons.Filled.Lock, title = "Privacy") {
                        Text("Zero uploads", style = MaterialTheme.typography.bodyMedium)
                    }
                    LabeledRow(icon = Icons.Filled.Cloud, title = "Cloud backup") {
                        Text(if (isGuest) "Sign in to enable" else "Google Drive", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Cloud export helper
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Export to cloud", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "When a conversion finishes you can share it straight to Dropbox, OneDrive, " +
                            "Box or Google Drive using your installed cloud apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            // Launches the system share sheet — any installed cloud target.
                            putExtra(Intent.EXTRA_TEXT, "Export finished files with nctools")
                        }
                        context.startActivity(Intent.createChooser(send, "Export with nctools"))
                    }) {
                        Text("Open share sheet")
                    }
                }
            }

            // About
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    LabeledRow(icon = Icons.Filled.Info, title = "Version") {
                        Text("1.0.0 (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "nctools — Private document tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        content()
    }
}