package app.frogwire.spectra.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.ui.theme.*

/** Bottom-bar menu: new tabs, Incognito+, downloads, dashboard, settings. */
@Composable
fun MenuSheet(
    isIncognito: Boolean,
    downloadCount: Int,
    onNewTab: () -> Unit,
    onToggleIncognito: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .sheetSurface()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        SheetHandle()
        MenuItem(Icons.Rounded.Add, if (isIncognito) "New Incognito+ tab" else "New tab", onClick = onNewTab)
        if (isIncognito) {
            MenuItem(Icons.Rounded.Public, "Back to normal tabs", onClick = onToggleIncognito)
        } else {
            MenuItem(Icons.Rounded.VisibilityOff, "Go Incognito+", tint = VioletText, onClick = onToggleIncognito)
        }
        MenuItem(
            Icons.Rounded.Download,
            "Downloads",
            detail = if (downloadCount > 0) "$downloadCount" else null,
            onClick = onOpenDownloads
        )
        MenuItem(Icons.Rounded.Insights, "Privacy dashboard", onClick = onOpenDashboard)
        MenuItem(Icons.Rounded.Settings, "Settings", onClick = onOpenSettings)
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = TextMain,
    detail: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, color = TextMain, modifier = Modifier.weight(1f))
        if (detail != null) Text(detail, fontSize = 14.sp, color = TextMuted)
    }
}
