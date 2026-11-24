package com.duck.privacy.browser.tabs

import android.content.Context
import android.content.MutableContextWrapper
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.webkit.ProfileStore
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.duck.privacy.browser.privacy.AdBlocker
import com.duck.privacy.browser.privacy.DuckDuckGoUrlHelper
import com.duck.privacy.browser.privacy.PageShields
import com.duck.privacy.browser.privacy.PrivacyWebViewClient
import com.duck.privacy.browser.privacy.SiteAllowlist
import com.duck.privacy.browser.privacy.TrackerAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns all tabs and their WebViews. Lives in [com.duck.privacy.browser.BrowserViewModel] so it
 * survives Activity recreation; the hosting Activity attaches itself via [attachHost].
 */
class TabManager(private val appContext: Context) {

    val standardTabs = mutableStateListOf<Tab>()
    val incognitoTabs = mutableStateListOf<Tab>()

    private val _activeTab = MutableStateFlow<Tab?>(null)
    val activeTab: StateFlow<Tab?> = _activeTab.asStateFlow()

    private val _isIncognitoActive = MutableStateFlow(false)
    val isIncognitoActive: StateFlow<Boolean> = _isIncognitoActive.asStateFlow()

    /**
     * Set when the user tried to open incognito but this device's WebView can't isolate it.
     * The UI shows an explanation and clears it via [dismissIncognitoUnavailable].
     */
    private val _incognitoUnavailable = MutableStateFlow(false)
    val incognitoUnavailable: StateFlow<Boolean> = _incognitoUnavailable.asStateFlow()

    /**
     * Incognito needs WebView profiles (MULTI_PROFILE, WebView 123+) for its own cookie jar,
     * cache and storage. Without them, incognito would silently share state with standard tabs,
     * so we fail closed and don't offer it at all.
     */
    val isIncognitoSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    fun dismissIncognitoUnavailable() {
        _incognitoUnavailable.value = false
    }

    /** Activity context while one is attached; WebViews need it for dialogs, <select> popups, autofill. */
    private var hostContext: Context = appContext

    /** Per-site "protection off" settings. Must be initialized before the first tab is created. */
    val siteAllowlist = SiteAllowlist(appContext)

    /**
     * Fingerprint shield + WebRTC leak guard run as a document-start script (WebView 90+).
     * On older WebViews they can't be enforced, and the UI says so instead of offering them.
     */
    val isPageShieldSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    private val pageShields = PageShields(appContext)
    private val shieldScripts = HashMap<String, ScriptHandler>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        AdBlocker.init(appContext)
        TrackerAnalytics.init(appContext)

        // "Wipe cookies on close": the app can be killed (e.g. swiped away) without a clean
        // shutdown, so also wipe at the next launch, before any page loads.
        if (siteAllowlist.state.value.wipeCookiesOnClose) wipeStandardCookiesAndStorage()

        createTab(url = "about:blank", isIncognito = false)

        // Apply protection switches to every open tab as soon as they change.
        scope.launch {
            var previous: SiteAllowlist.State? = null
            siteAllowlist.state.collect { state ->
                val prev = previous
                previous = state
                if (prev == null) return@collect // tabs are configured when they're created

                val shieldsChanged = prev.fingerprintShield != state.fingerprintShield ||
                    prev.webRtcLeakGuard != state.webRtcLeakGuard
                val sitesChanged = prev.standardOff != state.standardOff ||
                    prev.incognitoOff != state.incognitoOff ||
                    prev.incognitoOn != state.incognitoOn
                val cookiesChanged = prev.blockThirdPartyCookies != state.blockThirdPartyCookies
                if (prev.safeBrowsing != state.safeBrowsing) {
                    allTabs().forEach { it.webView.settings.safeBrowsingEnabled = state.safeBrowsing }
                }

                if (shieldsChanged || sitesChanged) refreshPageShields()
                if (cookiesChanged) allTabs().forEach { applyCookiePolicy(it.webView, it.isIncognito) }
                // Per-site changes reload on their own (setSiteProtection etc.)
                if (shieldsChanged || cookiesChanged) reloadActivePage()
            }
        }
    }

    /** Called when the ViewModel is cleared: stops listening and closes everything. */
    fun shutdown() {
        scope.cancel()
        destroyAll()
    }

    // region Page shields & cookie policy

    /** (Re)installs the shield script on one tab. Takes effect on the tab's next page load. */
    private fun installPageShields(tab: Tab) {
        try {
            shieldScripts.remove(tab.id)?.remove()
            if (!isPageShieldSupported) return
            val state = siteAllowlist.state.value
            val script = pageShields.buildScript(
                fingerprintShield = state.fingerprintShield,
                webRtcLeakGuard = state.webRtcLeakGuard,
                sitesWithProtectionOff = state.offSites(tab.isIncognito),
                isIncognito = tab.isIncognito
            ) ?: return
            shieldScripts[tab.id] = WebViewCompat.addDocumentStartJavaScript(tab.webView, script, setOf("*"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshPageShields() = allTabs().forEach { installPageShields(it) }

    private fun applyCookiePolicy(webView: WebView, isIncognito: Boolean) {
        // Incognito+ always blocks third-party cookies.
        val block = isIncognito || siteAllowlist.state.value.blockThirdPartyCookies
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !block)
    }

    private fun reloadActivePage() {
        val tab = _activeTab.value ?: return
        if (tab.currentUrl.value != "about:blank") tab.webView.reload()
    }

    private fun wipeStandardCookiesAndStorage() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
    }

    // endregion

    // region Host (Activity) lifecycle

    fun attachHost(activity: Context) {
        hostContext = activity
        allTabs().forEach { (it.webView.context as? MutableContextWrapper)?.baseContext = activity }
    }

    /** Call from Activity.onDestroy so WebViews don't leak the destroyed Activity. */
    fun detachHost() {
        hostContext = appContext
        allTabs().forEach { (it.webView.context as? MutableContextWrapper)?.baseContext = appContext }
    }

    // endregion

    /**
     * Creates and activates a tab. Returns null (and signals [incognitoUnavailable]) if an
     * incognito tab is requested on a WebView that can't isolate it.
     */
    fun createTab(url: String = "about:blank", isIncognito: Boolean = _isIncognitoActive.value): Tab? {
        if (isIncognito && !isIncognitoSupported) {
            _incognitoUnavailable.value = true
            return null
        }
        val tab = buildTab(isIncognito)

        if (url != "about:blank") {
            tab.webView.loadUrl(url)
        }

        if (isIncognito) incognitoTabs.add(tab) else standardTabs.add(tab)
        activate(tab)
        return tab
    }

    /** Creates a fully wired Tab + WebView without adding it to any list. */
    private fun buildTab(
        isIncognito: Boolean,
        recentCrashes: Int = 0,
        lastCrashAtMs: Long = 0L
    ): Tab {
        val webView = createConfiguredWebView(isIncognito)
        val tab = Tab(
            isIncognito = isIncognito,
            webView = webView,
            recentCrashes = recentCrashes,
            lastCrashAtMs = lastCrashAtMs
        )

        webView.webViewClient = PrivacyWebViewClient(
            onPageStarted = { startedUrl ->
                tab.currentUrl.value = startedUrl
                tab.title.value = DuckDuckGoUrlHelper.formatDisplayUrl(startedUrl)
                tab.isLoading.value = true
                tab.blockedTrackersCount.value = 0
                tab.pageBlockedDomains.value = emptyList()
                tab.canGoBack.value = webView.canGoBack()
                tab.canGoForward.value = webView.canGoForward()
            },
            onPageFinished = { finishedUrl ->
                tab.currentUrl.value = finishedUrl
                tab.title.value = webView.title ?: DuckDuckGoUrlHelper.formatDisplayUrl(finishedUrl)
                tab.isLoading.value = false
                tab.canGoBack.value = webView.canGoBack()
                tab.canGoForward.value = webView.canGoForward()
            },
            // Called from WebView network threads -> use atomic updates
            onTrackerBlocked = { domain ->
                tab.blockedTrackersCount.update { it + 1 }
                tab.pageBlockedDomains.update { if (domain in it) it else listOf(domain) + it }
                TrackerAnalytics.recordBlockedDomain(domain, appContext)
            },
            onRendererGone = { didCrash -> recoverFromRendererGone(tab, didCrash) },
            isProtectionDisabled = { host -> siteAllowlist.isProtectionDisabled(host, isIncognito) }
        )

        installPageShields(tab)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrEmpty() && !title.startsWith("http")) {
                    tab.title.value = title
                }
            }
        }

        return tab
    }

    fun selectTab(tab: Tab) = activate(tab)

    /**
     * Turns protection on/off for [host] in the active tab's mode (standard or incognito) and
     * reloads the page so the change takes effect immediately.
     */
    fun setSiteProtection(host: String, enabled: Boolean) {
        val tab = _activeTab.value ?: return
        siteAllowlist.setProtectionEnabled(host, tab.isIncognito, enabled)
        refreshPageShields()
        tab.webView.reload()
    }

    /** Removes [siteKey] from the saved "protection off" list (from the Settings sheet). */
    fun turnProtectionBackOn(siteKey: String) {
        siteAllowlist.setProtectionEnabled(siteKey, incognito = false, enabled = true)
        refreshPageShields()
        reloadActiveTabIfOnSite(siteKey)
    }

    /** Clears the whole "protection off" list, saved and incognito. */
    fun turnProtectionBackOnForAllSites() {
        siteAllowlist.clearAll()
        refreshPageShields()
        _activeTab.value?.webView?.reload()
    }

    private fun reloadActiveTabIfOnSite(siteKey: String) {
        val tab = _activeTab.value ?: return
        val host = tab.currentUrl.value.toUri().host ?: return
        if (siteAllowlist.keyFor(host) == siteKey) tab.webView.reload()
    }

    /** Single entry point for switching the visible tab; resumes any deferred reload. */
    private fun activate(tab: Tab) {
        _activeTab.value = tab
        _isIncognitoActive.value = tab.isIncognito
        tab.pendingUrl?.let { url ->
            tab.pendingUrl = null
            tab.webView.loadUrl(url)
        }
    }

    /**
     * The tab's renderer process died (crash or OOM kill). A dead WebView can't be reused, so
     * swap in a fresh one at the same position. The visible tab reloads immediately; background
     * tabs reload lazily when selected. A page that crashes repeatedly is not reloaded.
     */
    private fun recoverFromRendererGone(dead: Tab, didCrash: Boolean) {
        val list = if (dead.isIncognito) incognitoTabs else standardTabs
        val index = list.indexOfFirst { it.id == dead.id }
        if (index < 0) return // already closed or recovered

        val url = dead.pendingUrl ?: dead.currentUrl.value
        val wasActive = _activeTab.value?.id == dead.id

        val now = SystemClock.elapsedRealtime()
        val crashes = when {
            !didCrash -> dead.recentCrashes // killed for memory: not the page's fault
            now - dead.lastCrashAtMs < CRASH_LOOP_WINDOW_MS -> dead.recentCrashes + 1
            else -> 1
        }
        val crashLoop = crashes >= MAX_CRASHES_IN_WINDOW

        destroyTabInternal(dead, rendererDead = true)

        val replacement = buildTab(
            isIncognito = dead.isIncognito,
            recentCrashes = if (crashLoop) 0 else crashes,
            lastCrashAtMs = if (didCrash) now else dead.lastCrashAtMs
        )
        list[index] = replacement

        if (crashLoop || url == "about:blank") {
            if (crashLoop) {
                Toast.makeText(appContext, "This page keeps crashing and was closed", Toast.LENGTH_LONG).show()
            }
        } else if (wasActive) {
            replacement.webView.loadUrl(url)
        } else {
            replacement.pendingUrl = url
            replacement.currentUrl.value = url
            replacement.title.value = dead.title.value
        }

        if (wasActive) activate(replacement)
    }

    fun closeTab(tab: Tab) {
        val targetList = if (tab.isIncognito) incognitoTabs else standardTabs
        val index = targetList.indexOf(tab)
        if (index < 0) return
        targetList.removeAt(index)

        destroyTabInternal(tab)

        // Last incognito tab gone -> wipe everything the incognito session stored
        if (tab.isIncognito && incognitoTabs.isEmpty()) {
            wipeIncognitoData()
        }

        if (_activeTab.value?.id == tab.id) {
            if (targetList.isNotEmpty()) {
                val nextIndex = (index - 1).coerceAtLeast(0)
                activate(targetList[nextIndex])
            } else {
                val otherList = if (tab.isIncognito) standardTabs else incognitoTabs
                if (otherList.isNotEmpty()) {
                    activate(otherList.first())
                } else {
                    createTab(url = "about:blank", isIncognito = tab.isIncognito)
                }
            }
        }
    }

    fun setIncognitoMode(active: Boolean) {
        val list = if (active) incognitoTabs else standardTabs
        if (list.isEmpty()) {
            createTab(url = "about:blank", isIncognito = active)
        } else {
            activate(list.last())
        }
    }

    /**
     * Complete Session Purge (Flame / Wipe Button)
     * Drops all caches, cookies, storage and resets the browser to home.
     */
    fun purgeAllSessions() {
        // HTTP cache is shared per profile, so clear it once via any default-profile WebView.
        standardTabs.firstOrNull()?.webView?.clearCache(true)

        destroyAll()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()

        // The per-site list records which sites were visited, so the purge erases it too.
        siteAllowlist.clearAll()

        // New fingerprint noise: nothing links the next session to this one.
        pageShields.renewAllSeeds()

        _isIncognitoActive.value = false
        createTab(url = "about:blank", isIncognito = false)
    }

    /** Destroys every tab and wipes incognito data. Leaves no active tab. */
    fun destroyAll() {
        allTabs().forEach { destroyTabInternal(it) }
        standardTabs.clear()
        incognitoTabs.clear()
        wipeIncognitoData()

        if (siteAllowlist.state.value.wipeCookiesOnClose) wipeStandardCookiesAndStorage()

        _activeTab.value = null
    }

    private fun allTabs(): List<Tab> = standardTabs + incognitoTabs

    private fun destroyTabInternal(tab: Tab, rendererDead: Boolean = false) {
        shieldScripts.remove(tab.id)?.let { handler ->
            if (!rendererDead) runCatching { handler.remove() }
        }
        try {
            // A WebView whose renderer is gone may only be detached and destroyed.
            if (!rendererDead) {
                tab.webView.stopLoading()
                tab.webView.clearHistory()
            }
            (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
            (tab.webView.context as? MutableContextWrapper)?.baseContext = appContext
            tab.webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // region Incognito profile isolation

    /**
     * Incognito tabs run in a separate WebView profile with their own cookie jar, cache and
     * web storage, so nothing leaks into (or out of) standard tabs.
     */
    private fun wipeIncognitoData() {
        siteAllowlist.clearIncognito()
        pageShields.renewIncognitoSeed()
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return
        val store = ProfileStore.getInstance()
        store.getProfile(INCOGNITO_PROFILE)?.let { profile ->
            profile.cookieManager.removeAllCookies(null)
            profile.webStorage.deleteAllData()
        }
        try {
            // Also drops the profile's HTTP cache. Fails if a WebView still references it.
            store.deleteProfile(INCOGNITO_PROFILE)
        } catch (_: IllegalStateException) {
            // Cookies and storage were already cleared above.
        }
    }

    // endregion

    private fun createConfiguredWebView(isIncognito: Boolean): WebView {
        // Never create an incognito WebView that would share the default profile.
        check(!isIncognito || isIncognitoSupported) { "Incognito requires WebView profile support" }

        return WebView(MutableContextWrapper(hostContext)).apply {
            // Must be set before any other use of the WebView
            if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                ProfileStore.getInstance().getOrCreateProfile(INCOGNITO_PROFILE)
                WebViewCompat.setProfile(this, INCOGNITO_PROFILE)
            }

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Third-party cookies: follows the "Block third-party cookies" switch
            applyCookiePolicy(this, isIncognito)

            settings.apply {
                javaScriptEnabled = true
                // Incognito storage lives in its own profile and is wiped with it.
                domStorageEnabled = true
                databaseEnabled = false
                @Suppress("DEPRECATION")
                saveFormData = false
                setGeolocationEnabled(false)
                safeBrowsingEnabled = siteAllowlist.state.value.safeBrowsing

                allowFileAccess = false
                allowContentAccess = false

                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false

                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            }
        }
    }

    private companion object {
        const val INCOGNITO_PROFILE = "incognito"
        const val CRASH_LOOP_WINDOW_MS = 30_000L
        const val MAX_CRASHES_IN_WINDOW = 2
    }
}
