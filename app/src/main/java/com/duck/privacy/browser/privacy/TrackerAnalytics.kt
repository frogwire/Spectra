package com.duck.privacy.browser.privacy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TrackerCategory(val displayName: String, val colorHex: Long) {
    ADS("Ad Networks & Banners", 0xFFFF6B6B),
    ANALYTICS("Telemetry & Analytics", 0xFF5BC0EB),
    FINGERPRINTING("Fingerprint Probes", 0xFF9D4EDD),
    SOCIAL("Social Pixels & Tracking", 0xFFF2C14E),
    OTHER("Other Trackers", 0xFF8E8A9F)
}

data class DomainStat(
    val domain: String,
    val count: Long,
    val category: TrackerCategory
)

data class AnalyticsState(
    val totalBlocked: Long = 0L,
    val estimatedMbSaved: Double = 0.0,
    val categoryCounts: Map<TrackerCategory, Long> = emptyMap(),
    val topBlockedDomains: List<DomainStat> = emptyList()
)

object TrackerAnalytics {

    private const val PREFS_NAME = "spectra_tracker_analytics"
    private const val KEY_TOTAL_BLOCKED = "total_blocked"
    private const val KEY_DOMAIN_COUNTS = "domain_counts"

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val domainCountMap = HashMap<String, Long>()
    private var prefsInitialized = false

    fun init(context: Context) {
        if (prefsInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.getLong(KEY_TOTAL_BLOCKED, 0L)
        val rawDomainCounts = prefs.getStringSet(KEY_DOMAIN_COUNTS, emptySet()) ?: emptySet()

        synchronized(domainCountMap) {
            rawDomainCounts.forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val count = parts[1].toLongOrNull() ?: 0L
                    domainCountMap[parts[0]] = count
                }
            }
        }
        prefsInitialized = true
        recalculateState(total)
    }

    fun recordBlockedDomain(domain: String, context: Context?) {
        val registrable = AdBlocker.registrableDomain(domain.lowercase())
        val newTotal: Long

        synchronized(domainCountMap) {
            val current = domainCountMap[registrable] ?: 0L
            domainCountMap[registrable] = current + 1
            newTotal = (_state.value.totalBlocked + 1)
        }

        recalculateState(newTotal)

        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            synchronized(domainCountMap) {
                val set = domainCountMap.entries
                    .sortedByDescending { it.value }
                    .take(30)
                    .map { "${it.key}=${it.value}" }
                    .toSet()
                prefs.edit {
                    putLong(KEY_TOTAL_BLOCKED, newTotal)
                    putStringSet(KEY_DOMAIN_COUNTS, set)
                }
            }
        }
    }

    fun classifyCategory(domain: String): TrackerCategory {
        val lower = domain.lowercase()
        return when {
            ADS_KEYWORDS.any { lower.contains(it) } -> TrackerCategory.ADS
            ANALYTICS_KEYWORDS.any { lower.contains(it) } -> TrackerCategory.ANALYTICS
            FINGERPRINT_KEYWORDS.any { lower.contains(it) } -> TrackerCategory.FINGERPRINTING
            SOCIAL_KEYWORDS.any { lower.contains(it) } -> TrackerCategory.SOCIAL
            else -> TrackerCategory.OTHER
        }
    }

    fun clearAnalytics(context: Context) {
        synchronized(domainCountMap) {
            domainCountMap.clear()
        }
        _state.value = AnalyticsState()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
    }

    private fun recalculateState(total: Long) {
        val categoryMap = HashMap<TrackerCategory, Long>()
        val statsList = ArrayList<DomainStat>()

        synchronized(domainCountMap) {
            domainCountMap.forEach { (domain, count) ->
                val cat = classifyCategory(domain)
                categoryMap[cat] = (categoryMap[cat] ?: 0L) + count
                statsList.add(DomainStat(domain, count, cat))
            }
        }

        val topList = statsList.sortedByDescending { it.count }.take(10)
        val mbSaved = if (total > 0) (total * 0.045).coerceAtLeast(0.1) else 0.0

        _state.value = AnalyticsState(
            totalBlocked = total,
            estimatedMbSaved = mbSaved,
            categoryCounts = categoryMap,
            topBlockedDomains = topList
        )
    }

    private val ADS_KEYWORDS = setOf(
        "doubleclick", "googlesyndication", "adnxs", "rubiconproject", "criteo",
        "popads", "taboola", "outbrain", "amazon-adsystem", "adservice", "adsystem",
        "adform", "pubmatic", "openx", "appnexus", "casalemedia", "ad-delivery",
        "smartadserver", "adroll", "bidswitch", "advertising"
    )

    private val ANALYTICS_KEYWORDS = setOf(
        "analytics", "telemetry", "segment.io", "mixpanel", "hotjar", "clarity",
        "scorecardresearch", "quantserve", "chartbeat", "statcounter", "plausible",
        "matomo", "amplitude", "heap.io", "newrelic", "datadog", "google-analytics",
        "googletagmanager", "gtm"
    )

    private val FINGERPRINT_KEYWORDS = setOf(
        "fingerprint", "threatmetrix", "iovation", "augur", "device-api",
        "canvas", "font-probe", "audio-probe", "bot-detect", "recaptcha", "hcaptcha"
    )

    private val SOCIAL_KEYWORDS = setOf(
        "facebook.net", "connect.facebook", "tiktok", "twitter.com", "linkedin",
        "pinterest", "snapchat", "disqus", "addthis", "sharethis"
    )
}
