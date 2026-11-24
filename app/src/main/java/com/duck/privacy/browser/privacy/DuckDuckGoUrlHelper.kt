package com.duck.privacy.browser.privacy

import java.net.URLEncoder

object DuckDuckGoUrlHelper {
    private const val DDG_BASE = "https://duckduckgo.com/?q="
    private const val DDG_PARAMS = "&t=aura_android&kp=-2&kn=1"

    /**
     * Intelligently parses user input into either a direct URL or a DuckDuckGo search.
     * Fully preserves DuckDuckGo !bang commands (!w, !gh, !yt) which DDG resolves server-side.
     */
    fun resolveQuery(input: String): String {
        val query = input.trim()
        if (query.isEmpty()) return "about:blank"

        // 1. Direct explicit schemes (HTTPS-only: upgrade cleartext up front)
        // Typed / shared links also get click-tracking params stripped.
        if (query.startsWith("https://", ignoreCase = true)) {
            return TrackingParamStripper.strip(query)
        }
        if (query.startsWith("http://", ignoreCase = true)) {
            return TrackingParamStripper.strip("https://" + query.substring("http://".length))
        }

        // 2. Direct domains (e.g. news.ycombinator.com, wikipedia.org, localhost:3000)
        val hasDomainStructure = query.contains(".") && !query.contains(" ")
        val isLocalhost = query.startsWith("localhost", ignoreCase = true)

        if (hasDomainStructure || isLocalhost) {
            return "https://$query"
        }

        // 3. DuckDuckGo Search Query
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }

        return "$DDG_BASE$encoded$DDG_PARAMS"
    }

    /**
     * Extracts clean display host for the omnibar capsule.
     */
    fun formatDisplayUrl(url: String): String {
        if (url.startsWith("https://duckduckgo.com/?q=")) {
            val query = url.substringAfter("?q=").substringBefore("&")
            val decoded = try {
                java.net.URLDecoder.decode(query, "UTF-8")
            } catch (e: Exception) {
                query
            }
            return "DDG: $decoded"
        }

        return url.replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .trimEnd('/')
    }
}
