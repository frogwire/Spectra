package com.duck.privacy.browser.privacy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import java.net.URISyntaxException

class PrivacyWebViewClient(
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onTrackerBlocked: (String) -> Unit,
    /** Called on the UI thread; the WebView is dead and must be destroyed and replaced. */
    private val onRendererGone: (didCrash: Boolean) -> Unit,
    /** True if the user turned protection off for this site (thread-safe; read on network threads). */
    private val isProtectionDisabled: (host: String?) -> Boolean
) : WebViewClient() {

    /** Guards against https -> http -> https redirect loops. */
    @Volatile
    private var lastUpgradedUrl: String? = null

    /** Host of the current top-level page, used to tell first-party from third-party requests. */
    @Volatile
    private var pageHost: String? = null

    /**
     * Runs on WebView background threads for every request (documents, images, scripts...).
     */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val uri = request?.url ?: return null

        // A new top-level document defines the first party for everything it loads
        if (request.isForMainFrame) pageHost = uri.host

        // 1. Drop third-party Tracker & Ad requests before network dispatch
        //    (unless the user turned protection off for this site)
        val host = pageHost
        if (!isProtectionDisabled(host)) {
            AdBlocker.matchBlockedDomain(uri, host)?.let { domain ->
                AdBlocker.recordBlock()
                onTrackerBlocked(domain)
                return AdBlocker.createEmptyResponse()
            }
        }

        // 2. HTTPS-Only (always on, even with protection off): never let a cleartext request out.
        if (uri.scheme.equals("http", ignoreCase = true)) {
            // Only a top-level document may trigger a navigation; sub-resources are simply dropped.
            if (request.isForMainFrame && view != null) {
                val httpsUrl = cleanLink(toHttps(uri), uri.host)
                if (httpsUrl != lastUpgradedUrl) {
                    lastUpgradedUrl = httpsUrl
                    view.post { view.loadUrl(httpsUrl) }
                }
            }
            return AdBlocker.createEmptyResponse()
        }

        return super.shouldInterceptRequest(view, request)
    }

    /**
     * Runs on the UI thread for main-frame navigations (link clicks, redirects).
     */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        return when (uri.scheme?.lowercase()) {
            "https" -> {
                // Strip click-tracking params (utm_*, fbclid, gclid...). Only for GET, since
                // re-issuing a POST navigation via loadUrl would drop its body.
                val url = uri.toString()
                val cleaned = if (request.method == "GET") cleanLink(url, uri.host) else url
                if (cleaned != url) {
                    view.loadUrl(cleaned)
                    true
                } else {
                    false
                }
            }
            "about", "blob", "data" -> false
            "http" -> {
                val httpsUrl = cleanLink(toHttps(uri), uri.host)
                if (httpsUrl != lastUpgradedUrl) {
                    lastUpgradedUrl = httpsUrl
                    view.loadUrl(httpsUrl)
                }
                true
            }
            "intent", "mailto", "tel", "sms", "geo", "market" -> {
                // Only hand off to other apps on an explicit user gesture.
                if (request.hasGesture()) launchExternal(view, uri)
                true
            }
            // file:, content:, javascript:, chrome:, custom schemes -> blocked
            else -> true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            // Also covers back/forward loads served from cache without a main-frame intercept
            pageHost = url.toUri().host
            onPageStarted(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            if (url != lastUpgradedUrl) lastUpgradedUrl = null
            onPageFinished(url)
        }
    }

    /**
     * The renderer crashed or was killed by the system to reclaim memory. Without returning true
     * here, Android kills the whole app. Pages can trigger this on purpose, so it must be handled.
     */
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onRendererGone(detail.didCrash())
        return true
    }

    /** Strips tracking params unless protection is off for the destination site. */
    private fun cleanLink(url: String, destinationHost: String?): String =
        if (isProtectionDisabled(destinationHost)) url else TrackingParamStripper.strip(url)

    private fun toHttps(uri: Uri): String = uri.buildUpon().scheme("https").build().toString()

    /**
     * Safely launches a non-web URI in another app. intent:// URIs are sanitized so a web page
     * cannot target specific (possibly non-exported-by-intent) components or grant URI permissions.
     */
    private fun launchExternal(view: WebView, uri: Uri) {
        val intent = try {
            if (uri.scheme.equals("intent", ignoreCase = true)) {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                    component = null
                    selector = null
                    clipData = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
        } catch (e: URISyntaxException) {
            return
        }

        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // strips any FLAG_GRANT_* URI permission flags

        try {
            view.context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (fallbackUrl != null && fallbackUrl.startsWith("https://", ignoreCase = true)) {
                view.loadUrl(fallbackUrl)
            }
        } catch (e: SecurityException) {
            // Target refused the launch; ignore.
        }
    }
}
