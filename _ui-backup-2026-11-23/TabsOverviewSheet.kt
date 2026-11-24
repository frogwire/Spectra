package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.tabs.Tab
import com.duck.privacy.browser.ui.theme.*

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextMuted.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Segment Switcher: Standard vs Incognito+
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SpectraBgDark)
                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!showIncognitoSection) SpectraViolet else Color.Transparent)
                    .clickable { showIncognitoSection = false }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STANDARD (${standardTabs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!showIncognitoSection) Color.White else TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showIncognitoSection) IncognitoViolet else Color.Transparent)
                    .clickable { showIncognitoSection = true }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🕶️ INCOGNITO+ (${incognitoTabs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showIncognitoSection) Color.White else IncognitoViolet,
                    fontFamily = FontFamily.Monospace
                )
            }
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (showIncognitoSection) Color(0xFF1B1829) else SpectraBgDark)
                        .border(
                            1.dp,
                            if (isSelected) {
                                if (showIncognitoSection) IncognitoViolet else SpectraCyan
                            } else HairlineBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onSelectTab(tab)
                            onDismiss()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Text(
                                    text = "Active",
                                    fontSize = 9.sp,
                                    color = if (showIncognitoSection) IncognitoViolet else ShieldTeal,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tabUrl,
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HairlineBorder))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showIncognitoSection) "INCOGNITO+ · WIPES ON CLOSE" else "Wipe on close option",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Close [✕]",
                                fontSize = 10.sp,
                                color = if (showIncognitoSection) IncognitoViolet else SpectraViolet,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onCloseTab(tab) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Open New Tab Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (showIncognitoSection) IncognitoViolet.copy(alpha = 0.5f) else HairlineBorder,
                    RoundedCornerShape(12.dp)
                )
                .clickable {
                    onNewTab(showIncognitoSection)
                    onDismiss()
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showIncognitoSection) "+ Open New Incognito+ Tab 🕶️" else "+ Open New Standard Tab",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (showIncognitoSection) IncognitoViolet else TextMain
            )
        }
    }
}
