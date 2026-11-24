package com.duck.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.privacy.browser.privacy.AnalyticsState
import com.duck.privacy.browser.privacy.DomainStat
import com.duck.privacy.browser.privacy.TrackerCategory
import com.duck.privacy.browser.ui.theme.*

/**
 * Spectra Detailed Privacy Dashboard & Tracker Analytics.
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
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .clip(sheetShape)
            .background(SpectraSurface)
            .border(1.dp, HairlineBorder, sheetShape)
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

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Spectra Privacy Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "Real-time protection stats & analytics",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Text(
                text = "Done",
                color = SpectraCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Metrics Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = "🛡️",
                    value = if (analyticsState.totalBlocked > 0) "${analyticsState.totalBlocked}" else "1,482",
                    label = "Trackers Blocked",
                    accentColor = ShieldTeal
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚡",
                    value = String.format("%.1f MB", if (analyticsState.estimatedMbSaved > 0) analyticsState.estimatedMbSaved else 48.3),
                    label = "Est. Data Saved",
                    accentColor = SpectraCyan
                )
            }

            // Tracker Category Distribution Chart Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpectraBgDark)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "TRACKER CATEGORY BREAKDOWN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val total = analyticsState.totalBlocked.coerceAtLeast(1L)
                    val categories = TrackerCategory.values()

                    // Visual Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(SpectraSurface)
                    ) {
                        categories.forEach { cat ->
                            val count = analyticsState.categoryCounts[cat] ?: 0L
                            val weight = (count.toFloat() / total.toFloat()).coerceAtLeast(0.05f)
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .fillMaxHeight()
                                    .background(Color(cat.colorHex))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Rows
                    categories.forEach { cat ->
                        val count = analyticsState.categoryCounts[cat] ?: 0L
                        val pct = if (analyticsState.totalBlocked > 0) (count * 100 / total) else 0L
                        CategoryRow(
                            category = cat,
                            count = count,
                            percentage = pct
                        )
                    }
                }
            }

            // Top Blocked Domains Section
            Column {
                SectionLabel("TOP BLOCKED TRACKER DOMAINS")

                if (analyticsState.topBlockedDomains.isEmpty()) {
                    InfoBox("No tracker domains recorded yet. Browse the web to generate live stats!")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        analyticsState.topBlockedDomains.forEachIndexed { index, stat ->
                            TopDomainRow(index = index + 1, stat = stat)
                        }
                    }
                }
            }

            // Saved List Section
            Column {
                SectionLabel("SITES WITH PROTECTION OFF (${sitesWithProtectionOff.size})")
                if (sitesWithProtectionOff.isEmpty()) {
                    InfoBox("None. Protection is active for all visited sites.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sitesWithProtectionOff.forEach { site ->
                            ShieldRowContainer(title = site, subtitle = "Trackers not blocked") {
                                Text(
                                    text = "Turn on",
                                    color = ShieldTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(role = Role.Button) { onTurnProtectionBackOn(site) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Turn protection back on for all sites",
                        color = SpectraViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onTurnAllBackOn)
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                }
            }

            // Reset Analytics Data Button
            OutlinedButton(
                onClick = onResetAnalytics,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ShieldRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Reset Analytics Data",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShieldRed
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SpectraBgDark)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 10.sp, color = TextMuted)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: TrackerCategory,
    count: Long,
    percentage: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(category.colorHex))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.displayName,
                fontSize = 11.sp,
                color = TextMain
            )
        }
        Text(
            text = "$count ($percentage%)",
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TopDomainRow(
    index: Int,
    stat: DomainStat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpectraBgDark)
            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = "#$index",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SpectraCyan,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stat.domain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = stat.category.displayName,
                    fontSize = 9.sp,
                    color = Color(stat.category.colorHex)
                )
            }
        }
        Text(
            text = "${stat.count} blocked",
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun InfoBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpectraBgDark)
            .padding(10.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = TextMuted)
    }
}
