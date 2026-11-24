package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.privacy.AnalyticsState
import com.duck.privacy.browser.privacy.DomainStat
import com.duck.privacy.browser.privacy.TrackerCategory
import com.duck.privacy.browser.ui.theme.*

/**
 * Full privacy dashboard: what Spectra has blocked, by category and by domain.
 * Shows only real, on-device counts; nothing here leaves the phone.
 */
@Composable
fun PrivacyDashboardScreen(
    analyticsState: AnalyticsState,
    sitesWithProtectionOff: List<String>,
    onTurnProtectionBackOn: (String) -> Unit,
    onTurnAllBackOn: () -> Unit,
    onResetAnalytics: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }
    val total = analyticsState.totalBlocked
    val hasData = total > 0

    Column(
        modifier = Modifier
            .sheetSurface()
            .fillMaxHeight(0.92f)
            .padding(horizontal = 20.dp)
    ) {
        SheetHandle()
        SheetHeader(
            title = "Privacy dashboard",
            subtitle = "Counted on this device only",
            onDone = onDismiss
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Headline metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.GppGood,
                    value = "%,d".format(total),
                    label = "Trackers blocked",
                    accentColor = ShieldTeal
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Speed,
                    value = String.format("%.1f MB", analyticsState.estimatedMbSaved),
                    label = "Data saved (est.)",
                    accentColor = SpectraCyan
                )
            }

            if (!hasData) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CrystalElk(modifier = Modifier.size(88.dp), crystals = 0)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nothing blocked yet. Browse a few sites and the breakdown fills in here.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Category breakdown
                SheetSectionLabel("By category")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpectraBgDark)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    val counts = TrackerCategory.values()
                        .map { it to (analyticsState.categoryCounts[it] ?: 0L) }
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }

                    // Stacked bar: only categories that actually have blocks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(SpectraSurface)
                            .semantics { contentDescription = "Tracker categories chart" }
                    ) {
                        counts.forEachIndexed { i, (cat, count) ->
                            Box(
                                modifier = Modifier
                                    .weight(count.toFloat())
                                    .fillMaxHeight()
                                    .padding(start = if (i == 0) 0.dp else 2.dp)
                                    .background(Color(cat.colorHex))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    counts.forEach { (cat, count) ->
                        CategoryRow(category = cat, count = count, percentage = count * 100 / total)
                    }
                }

                // Top domains
                SheetSectionLabel("Most blocked")
                if (analyticsState.topBlockedDomains.isEmpty()) {
                    SheetInfoBox("No tracker domains recorded yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        analyticsState.topBlockedDomains.forEachIndexed { index, stat ->
                            TopDomainRow(rank = index + 1, stat = stat)
                        }
                    }
                }
            }

            // Sites with protection off
            SheetSectionLabel("Sites with protection off · ${sitesWithProtectionOff.size}")
            SitesWithProtectionOff(sitesWithProtectionOff, onTurnProtectionBackOn, onTurnAllBackOn)

            if (hasData) {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouch)
                        .padding(top = 24.dp)
                ) {
                    Text("Reset stats", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ShieldRed)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = SpectraSurface,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Reset tracker stats?") },
            text = { Text("Clears the counts on this dashboard. Your protection settings stay as they are.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetAnalytics()
                }) { Text("Reset", color = ShieldRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = TextMain) }
            }
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SpectraBgDark)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 13.sp, color = TextMuted, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 26.sp,
            fontFamily = SpectraDisplay,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryRow(category: TrackerCategory, count: Long, percentage: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(category.colorHex))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = category.displayName,
            fontSize = 14.sp,
            color = TextMain,
            modifier = Modifier.weight(1f)
        )
        Text(text = "%,d".format(count), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        Text(
            text = "  $percentage%",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.widthIn(min = 44.dp)
        )
    }
}

@Composable
private fun TopDomainRow(rank: Int, stat: DomainStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SpectraBgDark)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            fontSize = 14.sp,
            fontFamily = SpectraDisplay,
            fontWeight = FontWeight.Bold,
            color = SpectraCyan,
            modifier = Modifier.widthIn(min = 22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.domain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(stat.category.colorHex))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stat.category.displayName, fontSize = 12.sp, color = TextMuted)
            }
        }
        Text(text = "%,d".format(stat.count), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
    }
}
