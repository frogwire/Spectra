package com.duck.privacy.browser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import com.duck.privacy.browser.privacy.AdBlocker
import com.duck.privacy.browser.privacy.DuckDuckGoUrlHelper
import com.duck.privacy.browser.privacy.TrackerAnalytics
import com.duck.privacy.browser.tabs.TabManager
import com.duck.privacy.browser.ui.components.*
import com.duck.privacy.browser.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private val tabManager: TabManager get() = viewModel.tabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // WebViews outlive the Activity (in the ViewModel); give them this Activity's context
        tabManager.attachHost(this)

        // Handle incoming URL intents (from other apps) only on first launch, not on recreation
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            PrivacyBrowserTheme {
                BrowserApp(tabManager = tabManager, activity = this)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        tabManager.detachHost()
        super.onDestroy()
    }

    /**
     * The Activity is exported, so any app can send it an explicit intent with any data URI.
     * Only accept http(s) VIEW intents (blocks javascript:, file:, content:, intent: ...).
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            tabManager.createTab(
                url = DuckDuckGoUrlHelper.resolveQuery(uri.toString()),
                isIncognito = false
            )
        }
    }

    /** Installed WebView provider's version, for the incognito-unavailable message. */
    fun webViewVersion(): String? = WebViewCompat.getCurrentWebViewPackage(this)?.versionName

    /** Opens the Play Store page for whichever package currently provides WebView. */
    fun openWebViewUpdate() {
        val pkg = WebViewCompat.getCurrentWebViewPackage(this)?.packageName
            ?: "com.google.android.webview"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
        } catch (e: ActivityNotFoundException) {
            // No Play Store: open the listing as a web page in this browser
            tabManager.createTab(
                url = "https://play.google.com/store/apps/details?id=$pkg",
                isIncognito = false
            )
        }
    }

    fun applyScreenSecurity(isIncognito: Boolean) {
        if (isIncognito) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
fun BrowserApp(
    tabManager: TabManager,
    activity: MainActivity
) {
    val activeTab by tabManager.activeTab.collectAsState()
    val isIncognitoActive by tabManager.isIncognitoActive.collectAsState()
    val totalBlocked by AdBlocker.totalBlockedCount.collectAsState()
    val incognitoUnavailable by tabManager.incognitoUnavailable.collectAsState()

    var showShieldSheet by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPurgeConfirm by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(false) }
    val analytics by TrackerAnalytics.state.collectAsState()
    val anySheetOpen = showShieldSheet || showTabsSheet || showSettingsSheet || showDashboard
    val closeSheets = {
        showShieldSheet = false
        showTabsSheet = false
        showSettingsSheet = false
        showDashboard = false
    }

    // Synchronize Android FLAG_SECURE whenever incognito state changes
    LaunchedEffect(isIncognitoActive) {
        activity.applyScreenSecurity(isIncognitoActive)
    }

    val currentUrl = activeTab?.currentUrl?.collectAsState()?.value ?: "about:blank"
    val canGoBack = activeTab?.canGoBack?.collectAsState()?.value ?: false
    val canGoForward = activeTab?.canGoForward?.collectAsState()?.value ?: false
    val isLoading = activeTab?.isLoading?.collectAsState()?.value ?: false
    val tabBlockedCount = activeTab?.blockedTrackersCount?.collectAsState()?.value ?: 0
    val pageBlockedDomains = activeTab?.pageBlockedDomains?.collectAsState()?.value ?: emptyList()

    val isHome = currentUrl == "about:blank" || currentUrl.isEmpty()

    // Per-site protection state for the current page
    val siteProtection by tabManager.siteAllowlist.state.collectAsState()
    val siteHost = if (isHome) null else currentUrl.toUri().host
    val siteKey = siteHost?.let { tabManager.siteAllowlist.keyFor(it) }
    val tabIsIncognito = activeTab?.isIncognito ?: isIncognitoActive
    val protectionEnabled = siteKey == null || !siteProtection.isOff(siteKey, tabIsIncognito)

    // Handle Android system back gesture
    BackHandler(enabled = true) {
        when {
            anySheetOpen -> closeSheets()
            canGoBack -> activeTab?.webView?.goBack()
            !isHome -> activeTab?.webView?.loadUrl("about:blank")
            else -> activity.finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpectraBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Top Slim Progress Indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (isIncognitoActive) IncognitoViolet else SpectraViolet,
                    trackColor = SpectraSurface
                )
            }

            // Main Webview or Home Screen Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isHome) {
                    HomeView(
                        isIncognito = isIncognitoActive,
                        totalBlocked = totalBlocked,
                        privacyState = siteProtection,
                        pageShieldsSupported = tabManager.isPageShieldSupported,
                        onNavigate = { target ->
                            val resolved = DuckDuckGoUrlHelper.resolveQuery(target)
                            activeTab?.webView?.loadUrl(resolved)
                        }
                    )
                } else {
                    activeTab?.let { tab ->
                        // key() forces a fresh AndroidView per tab; otherwise factory runs only
                        // once and switching tabs would keep showing the previous WebView.
                        key(tab.id) {
                            AndroidView(
                                factory = {
                                    (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
                                    tab.webView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Bottom Ergonomic Omnibar & Toolbar
            Omnibar(
                urlText = if (isHome) "" else DuckDuckGoUrlHelper.formatDisplayUrl(currentUrl),
                isIncognito = isIncognitoActive,
                blockedCount = tabBlockedCount,
                protectionEnabled = protectionEnabled,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                tabCount = if (isIncognitoActive) tabManager.incognitoTabs.size else tabManager.standardTabs.size,
                onSearch = { query ->
                    val resolved = DuckDuckGoUrlHelper.resolveQuery(query)
                    activeTab?.webView?.loadUrl(resolved)
                },
                onBack = { activeTab?.webView?.goBack() },
                onForward = { activeTab?.webView?.goForward() },
                onPurge = { closeSheets(); showPurgeConfirm = true },
                onOpenTabs = { closeSheets(); showTabsSheet = true },
                onOpenShield = { closeSheets(); showShieldSheet = true },
                onOpenSettings = { closeSheets(); showSettingsSheet = true },
                onToggleIncognito = {
                    tabManager.setIncognitoMode(!isIncognitoActive)
                }
            )
        }

        // Dimmed backdrop: makes sheets read as overlays; tap outside to close
        AnimatedVisibility(visible = anySheetOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = closeSheets
                    )
            )
        }

        // Slide-up Settings Sheet
        AnimatedVisibility(
            visible = showSettingsSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            // pointerInput makes the sheet a hit target so taps on it don't reach the backdrop
            modifier = Modifier.align(Alignment.BottomCenter).pointerInput(Unit) {}
        ) {
            SettingsSheet(
                currentSiteKey = siteKey,
                isIncognito = tabIsIncognito,
                incognitoSupported = tabManager.isIncognitoSupported,
                pageShieldsSupported = tabManager.isPageShieldSupported,
                currentSiteProtected = protectionEnabled,
                onCurrentSiteProtectionChange = { enabled ->
                    siteHost?.let { tabManager.setSiteProtection(it, enabled) }
                },
                privacyState = siteProtection,
                onToggleBlockTrackers = { tabManager.siteAllowlist.setBlockTrackers(it) },
                onToggleFingerprintShield = { tabManager.siteAllowlist.setFingerprintShield(it) },
                onToggleBlockThirdPartyCookies = { tabManager.siteAllowlist.setBlockThirdPartyCookies(it) },
                onToggleWebRtcLeakGuard = { tabManager.siteAllowlist.setWebRtcLeakGuard(it) },
                onToggleWipeCookiesOnClose = { tabManager.siteAllowlist.setWipeCookiesOnClose(it) },
                onToggleSafeBrowsing = { tabManager.siteAllowlist.setSafeBrowsing(it) },
                totalBlocked = analytics.totalBlocked,
                onOpenDashboard = {
                    showSettingsSheet = false
                    showDashboard = true
                },
                sitesWithProtectionOff = siteProtection.standardOff.sorted(),
                onTurnProtectionBackOn = { tabManager.turnProtectionBackOn(it) },
                onTurnAllBackOn = { tabManager.turnProtectionBackOnForAllSites() },
                onDismiss = { showSettingsSheet = false }
            )
        }

        // Slide-up Privacy Shield Inspector Bottom Sheet
        AnimatedVisibility(
            visible = showShieldSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            // pointerInput makes the sheet a hit target so taps on it don't reach the backdrop
            modifier = Modifier.align(Alignment.BottomCenter).pointerInput(Unit) {}
        ) {
            PrivacyShieldSheet(
                domain = DuckDuckGoUrlHelper.formatDisplayUrl(currentUrl),
                siteKey = siteKey,
                isIncognito = tabIsIncognito,
                pageShieldsSupported = tabManager.isPageShieldSupported,
                privacyState = siteProtection,
                onProtectionChange = { enabled ->
                    siteHost?.let { tabManager.setSiteProtection(it, enabled) }
                },
                onToggleBlockTrackers = { tabManager.siteAllowlist.setBlockTrackers(it) },
                onToggleFingerprintShield = { tabManager.siteAllowlist.setFingerprintShield(it) },
                onToggleBlockThirdPartyCookies = { tabManager.siteAllowlist.setBlockThirdPartyCookies(it) },
                onToggleWebRtcLeakGuard = { tabManager.siteAllowlist.setWebRtcLeakGuard(it) },
                onToggleWipeCookiesOnClose = { tabManager.siteAllowlist.setWipeCookiesOnClose(it) },
                blockedTrackers = pageBlockedDomains,
                onOpenDashboard = {
                    showShieldSheet = false
                    showDashboard = true
                },
                onDismiss = { showShieldSheet = false }
            )
        }

        // Slide-up full Privacy Dashboard
        AnimatedVisibility(
            visible = showDashboard,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).pointerInput(Unit) {}
        ) {
            PrivacyDashboardScreen(
                analyticsState = analytics,
                sitesWithProtectionOff = siteProtection.standardOff.sorted(),
                onTurnProtectionBackOn = { tabManager.turnProtectionBackOn(it) },
                onTurnAllBackOn = { tabManager.turnProtectionBackOnForAllSites() },
                onResetAnalytics = { TrackerAnalytics.clearAnalytics(activity) },
                onDismiss = { showDashboard = false }
            )
        }

        // Slide-up Tabs Overview Bottom Sheet
        AnimatedVisibility(
            visible = showTabsSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            // pointerInput makes the sheet a hit target so taps on it don't reach the backdrop
            modifier = Modifier.align(Alignment.BottomCenter).pointerInput(Unit) {}
        ) {
            TabsOverviewSheet(
                standardTabs = tabManager.standardTabs,
                incognitoTabs = tabManager.incognitoTabs,
                activeTab = activeTab,
                initialIsIncognito = isIncognitoActive,
                onSelectTab = { tab ->
                    tabManager.selectTab(tab)
                    showTabsSheet = false
                },
                onCloseTab = { tab ->
                    tabManager.closeTab(tab)
                },
                onNewTab = { isIncognito ->
                    tabManager.createTab(url = "about:blank", isIncognito = isIncognito)
                    showTabsSheet = false
                },
                onDismiss = { showTabsSheet = false }
            )
        }

        if (showPurgeConfirm) {
            AlertDialog(
                onDismissRequest = { showPurgeConfirm = false },
                containerColor = SpectraSurface,
                titleContentColor = TextMain,
                textContentColor = TextMuted,
                title = { Text("Burn this session?") },
                text = {
                    Text("Closes every tab and deletes cookies, site storage and cache. This can't be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPurgeConfirm = false
                        tabManager.purgeAllSessions()
                    }) { Text("Burn it", color = ShieldRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showPurgeConfirm = false }) { Text("Cancel", color = TextMain) }
                }
            )
        }

        if (incognitoUnavailable) {
            IncognitoUnavailableDialog(
                webViewVersion = remember { activity.webViewVersion() },
                onUpdate = {
                    tabManager.dismissIncognitoUnavailable()
                    activity.openWebViewUpdate()
                },
                onDismiss = { tabManager.dismissIncognitoUnavailable() }
            )
        }
    }
}
