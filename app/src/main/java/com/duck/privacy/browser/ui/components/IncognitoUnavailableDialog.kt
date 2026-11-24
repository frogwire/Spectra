package com.duck.privacy.browser.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.duck.privacy.browser.ui.theme.IncognitoViolet
import com.duck.privacy.browser.ui.theme.SpectraSurface
import com.duck.privacy.browser.ui.theme.TextMain
import com.duck.privacy.browser.ui.theme.TextMuted

/**
 * Shown instead of opening incognito when the device's WebView can't isolate incognito data.
 */
@Composable
fun IncognitoUnavailableDialog(
    webViewVersion: String?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val versionText = webViewVersion?.let { " (version $it)" } ?: ""
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = IncognitoViolet) },
        title = { Text("Incognito+ unavailable") },
        text = {
            Text(
                "Your Android System WebView$versionText is too old to keep Incognito+ " +
                    "cookies and site data separate from normal tabs. Update it to use Incognito+."
            )
        },
        confirmButton = {
            TextButton(onClick = onUpdate) { Text("Update WebView", color = IncognitoViolet) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now", color = TextMuted) }
        },
        containerColor = SpectraSurface,
        titleContentColor = TextMain,
        textContentColor = TextMuted
    )
}
