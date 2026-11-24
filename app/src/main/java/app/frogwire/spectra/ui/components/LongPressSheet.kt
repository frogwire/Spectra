package app.frogwire.spectra.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.tabs.LongPressTarget
import app.frogwire.spectra.ui.theme.*

/** Actions for a long-pressed link or image. */
@Composable
fun LongPressSheet(
    target: LongPressTarget,
    incognitoSupported: Boolean,
    onOpenInNewTab: (url: String, incognito: Boolean) -> Unit,
    onCopy: (label: String, text: String) -> Unit,
    onSaveImage: (url: String) -> Unit
) {
    Column(
        modifier = Modifier
            .sheetSurface()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        SheetHandle()

        val heading = target.link ?: target.image?.let { if (it.startsWith("data:")) "Image on this page" else it }
        if (heading != null) {
            Text(
                text = heading,
                fontSize = 13.sp,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        target.link?.let { link ->
            MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open in new tab") { onOpenInNewTab(link, target.isIncognito) }
            if (!target.isIncognito && incognitoSupported) {
                MenuRow(Icons.Rounded.VisibilityOff, "Open in Incognito+ tab", tint = VioletText) { onOpenInNewTab(link, true) }
            }
            MenuRow(Icons.Rounded.ContentCopy, "Copy link") { onCopy("Link", link) }
        }

        target.image?.let { image ->
            MenuRow(Icons.Rounded.Download, "Save image", tint = SpectraCyan) { onSaveImage(image) }
            if (!image.startsWith("data:")) {
                MenuRow(Icons.Rounded.Image, "Open image in new tab") { onOpenInNewTab(image, target.isIncognito) }
                MenuRow(Icons.Rounded.ContentCopy, "Copy image address") { onCopy("Image address", image) }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, tint: Color = TextMain, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 16.sp, color = TextMain)
    }
}
