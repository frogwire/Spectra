package app.frogwire.spectra.tabs

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow

data class Tab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isIncognito: Boolean,
    val webView: WebView,
    val currentUrl: MutableStateFlow<String> = MutableStateFlow("about:blank"),
    val title: MutableStateFlow<String> = MutableStateFlow(if (isIncognito) "Incognito Tab" else "New Tab"),
    /** Trackers blocked on the current page of this tab (reset on each navigation). */
    val blockedTrackersCount: MutableStateFlow<Int> = MutableStateFlow(0),
    /** Distinct tracker domains blocked on the current page of this tab, newest first. */
    val pageBlockedDomains: MutableStateFlow<List<String>> = MutableStateFlow(emptyList()),
    val canGoBack: MutableStateFlow<Boolean> = MutableStateFlow(false),
    val canGoForward: MutableStateFlow<Boolean> = MutableStateFlow(false),
    val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false),
    /** Renderer crashes within the crash-loop window (carried over when a tab is recovered). */
    val recentCrashes: Int = 0,
    /** elapsedRealtime() of the last renderer crash, or 0. */
    val lastCrashAtMs: Long = 0L
) {
    /** URL to load the next time this tab becomes active (set after a background renderer loss). */
    var pendingUrl: String? = null
}
