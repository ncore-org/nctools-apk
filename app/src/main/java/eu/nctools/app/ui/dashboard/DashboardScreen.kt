package eu.nctools.app.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.nctools.app.core.tools.ToolCatalog
import eu.nctools.app.ui.auth.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen(
    viewModel: AuthViewModel,
    onOpenTool: (String) -> Unit,
) {
    // Tools are a static registry — mirrors the nctools.eu catalog.
    val toolList = remember { ToolCatalog.all }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user

    // Google Drive linking — same flow as the web dashboard.
    val driveSignIn: DriveViewModel = hiltViewModel()
    val drivePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        driveSignIn.handleResult(result.resultCode, result.data)
    }
    val driveLinked by driveSignIn.linked.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("nctools", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            user?.name ?: user?.email ?: "Dashboard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    OutlinedButton(onClick = { viewModel.logout() }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sign out", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text("Your private document workspace", style = MaterialTheme.typography.titleMedium)
            Text(
                "Convert PDFs, scan, merge and OCR — everything stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            // Google Drive linking card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (driveLinked) "Google Drive connected" else "Connect Google Drive",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (driveLinked) {
                        OutlinedButton(onClick = { driveSignIn.signOut() }) { Text("Disconnect") }
                    } else {
                        Button(onClick = {
                            driveSignIn.signInIntent()?.let { intent ->
                                drivePicker.launch(intent)
                            }
                        }) { Text("Connect") }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(toolList) { tool ->
                    Card(
                        onClick = { onOpenTool(tool.slug) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text(tool.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}