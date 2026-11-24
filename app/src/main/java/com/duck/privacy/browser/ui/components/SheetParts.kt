package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.ui.theme.*

/** Shape shared by every bottom sheet. */
internal val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Standard sheet container: surface, hairline border, rounded top. */
internal fun Modifier.sheetSurface(): Modifier = this
    .fillMaxWidth()
    .clip(SheetShape)
    .background(SpectraSurface)
    .border(1.dp, HairlineBorder, SheetShape)

@Composable
internal fun ColumnScope.SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(TextMuted.copy(alpha = 0.4f))
            .align(Alignment.CenterHorizontally)
    )
}

@Composable
internal fun SheetHeader(title: String, subtitle: String? = null, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontFamily = SpectraDisplay,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, color = TextMuted)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = MinTouch)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onDone)
                .padding(horizontal = 14.dp)
        ) {
            Text(text = "Done", color = SpectraCyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMuted,
        modifier = Modifier
            .padding(top = 20.dp, bottom = 8.dp, start = 4.dp)
            .semantics { heading() }
    )
}

@Composable
internal fun SheetInfoBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SpectraBgDark)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(text = text, fontSize = 13.sp, lineHeight = 18.sp, color = TextMuted)
    }
}

/** A compact text action such as "Turn on", sized as a full touch target. */
@Composable
internal fun SheetTextAction(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = MinTouch)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Text(text = text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
