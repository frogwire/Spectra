package app.frogwire.spectra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.downloads.DownloadItem
import app.frogwire.spectra.downloads.DownloadOffer
import app.frogwire.spectra.downloads.DownloadStatus
import app.frogwire.spectra.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DownloadsSheet(
    items: List<DownloadItem>,
    onRefresh: () -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit,
    onClearList: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }

    // Poll DownloadManager for progress while this sheet is on screen
    LaunchedEffect(Unit) {
        while (true) {
            onRefresh()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .sheetSurface()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 20.dp)
    ) {
        SheetHandle()
        SheetHeader(title = "Downloads", subtitle = "Files are saved to your Downloads folder", onDone = onDismiss)
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No downloads yet", fontSize = 15.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    DownloadRow(item, onOpen = { onOpen(item) }, onDelete = { confirmDelete = item })
                }
            }
            SheetTextAction("Clear list (files stay in Downloads)", VioletText, onClearList)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = SpectraSurface,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Delete ${item.fileName}?") },
            text = { Text("Removes the file from your phone. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    onDelete(item)
                }) { Text("Delete", color = ShieldRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = TextMain) }
            }
        )
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SpectraBgDark)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.status == DownloadStatus.Failed) Icons.Rounded.ErrorOutline else Icons.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = if (item.status == DownloadStatus.Failed) ShieldRed else SpectraCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.fileName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isIncognito) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = "Incognito+", tint = VioletText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(statusLine(item), fontSize = 12.sp, color = if (item.status == DownloadStatus.Failed) ShieldRed else TextMuted, maxLines = 1)
            }
            if (item.status == DownloadStatus.Running) {
                Spacer(modifier = Modifier.height(6.dp))
                val fraction = if (item.bytesTotal > 0) (item.bytesDone.toFloat() / item.bytesTotal).coerceIn(0f, 1f) else null
                val barModifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                if (fraction != null) {
                    LinearProgressIndicator(progress = { fraction }, modifier = barModifier, color = SpectraCyan, trackColor = SpectraSurface)
                } else {
                    LinearProgressIndicator(modifier = barModifier, color = SpectraCyan, trackColor = SpectraSurface)
                }
            }
        }
        if (item.status == DownloadStatus.Done) {
            ToolbarIcon(Icons.AutoMirrored.Rounded.OpenInNew, "Open ${item.fileName}", iconSize = 20.dp, onClick = onOpen)
        }
        ToolbarIcon(Icons.Rounded.Delete, "Delete ${item.fileName}", tint = TextMuted, iconSize = 20.dp, onClick = onDelete)
    }
}

private fun statusLine(item: DownloadItem): String = when (item.status) {
    DownloadStatus.Running ->
        if (item.bytesTotal > 0) "${formatBytes(item.bytesDone)} of ${formatBytes(item.bytesTotal)}"
        else "Downloading… ${formatBytes(item.bytesDone)}"
    DownloadStatus.Done -> listOf(formatBytes(item.bytesTotal.takeIf { it > 0 } ?: item.bytesDone), item.host)
        .filter { it.isNotEmpty() }.joinToString(" · ")
    DownloadStatus.Failed -> "Failed"
}

internal fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

/** "Download this file?" prompt shown before anything is fetched. */
@Composable
fun DownloadConfirmDialog(offer: DownloadOffer, onDownload: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = SpectraSurface,
        titleContentColor = TextMain,
        textContentColor = TextMuted,
        title = { Text("Download this file?") },
        text = {
            Column {
                Text(offer.fileName, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val details = listOf(formatBytes(offer.contentLength), offer.host).filter { it.isNotEmpty() }
                if (details.isNotEmpty()) Text(details.joinToString(" · "), fontSize = 13.sp)
                if (offer.isIncognito) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "The file is saved to Downloads. Spectra forgets it when Incognito+ closes, " +
                            "and no system download history is kept.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("Download", color = SpectraCyan) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = TextMain) } }
    )
}
