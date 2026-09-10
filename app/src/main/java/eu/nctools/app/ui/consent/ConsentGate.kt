package eu.nctools.app.ui.consent

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * First-run GDPR consent gate (matches the website banner). Ads & personalized
 * analytics are only enabled after the user accepts; rejecting still allows
 * the free tools (they are gated by the non-personalized 15s ad).
 */
@Composable
fun ConsentGate(
    viewModel: ConsentViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val consent by viewModel.consent.collectAsStateWithLifecycle()
    if (consent.resolved) {
        content()
    } else {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("We value your privacy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "nctools is free thanks to advertising. With your consent we may show " +
                            "personalized ads; you can always choose essential-cookies-only and still " +
                            "use every tool. Your files never leave your device."
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::acceptAll) { Text("Accept all") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::essentialOnly) {
                        Text("Essential only", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = viewModel::reject) { Text("Reject") }
                }
            },
        )
    }
}