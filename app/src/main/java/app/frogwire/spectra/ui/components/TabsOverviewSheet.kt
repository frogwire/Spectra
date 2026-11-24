package app.frogwire.spectra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.frogwire.spectra.tabs.Tab
import app.frogwire.spectra.ui.theme.*

@Composable
fun TabsOverviewSheet(
    standardTabs: List<Tab>,
    incognitoTabs: List<Tab>,
    activeTab: Tab?,
    initialIsIncognito: Boolean,
    onSelectTab: (Tab) -> Unit,
    onCloseTab: (Tab) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showIncognitoSection by remember { mutableStateOf(initialIsIncognito) }
    val currentTabs = if (showIncognitoSection) incognitoTabs else standardTabs
    val accent = if (showIncognitoSection) VioletText else SpectraCyan

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(20.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextMuted.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Standard / Incognito+ switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SpectraBgDark)
                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            SegmentButton(
                modifier = Modifier.weight(1f),
                label = "Tabs · ${standardTabs.size}",
                selected = !showIncognitoSection,
                selectedColor = SpectraViolet,
                onClick = { showIncognitoSection = false }
            )
            SegmentButton(
                modifier = Modifier.weight(1f),
                label = "Incognito+ · ${incognitoTabs.size}",
                selected = showIncognitoSection,
                selectedColor = IncognitoViolet,
                showIncognitoIcon = true,
                onClick = { showIncognitoSection = true }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentTabs) { tab ->
                val isSelected = tab.id == activeTab?.id
                val tabTitle by tab.title.collectAsState()
                val tabUrl by tab.currentUrl.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (showIncognitoSection) Color(0xFF1B1829) else SpectraBgDark)
                        .border(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) accent else HairlineBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(role = Role.Button) { onSelectTab(tab) }
                        .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = tabTitle.ifBlank { "New tab" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (tabUrl == "about:blank" || tabUrl.isEmpty()) "Home" else tabUrl,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                    ToolbarIcon(
                        icon = Icons.Rounded.Close,
                        description = "Close tab ${tabTitle.ifBlank { "New tab" }}",
                        tint = TextMuted,
                        iconSize = 20.dp,
                        onClick = { onCloseTab(tab) }
                    )
                }
            }
        }

        if (showIncognitoSection) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Everything from these tabs is wiped when they close.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // New tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (showIncognitoSection) IncognitoViolet else SpectraSurface2)
                .clickable(role = Role.Button) { onNewTab(showIncognitoSection) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showIncognitoSection) "New Incognito+ tab" else "New tab",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SegmentButton(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    showIncognitoIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIncognitoIcon) {
            Icon(
                imageVector = Icons.Rounded.VisibilityOff,
                contentDescription = null,
                tint = if (selected) Color.White else VioletText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else if (showIncognitoIcon) VioletText else TextMuted
        )
    }
}
