package app.frogwire.spectra.privacy

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayInputStream

/**
 * Third-party aware tracker blocker.
 *
 * Rules (assets/hosts.txt):
 *   example.com          block example.com and all subdomains
 *   example.com/path     block only URLs on that host (or subdomains) whose path starts with /path
 *   @@cdn.example.com    never block this host or its subdomains (overrides broader rules)
 *
 * Requests are only blocked when they are third-party to the page: a different registrable
 * domain AND not owned by the same company (assets/entities.txt). This mirrors how DuckDuckGo's
 * protection avoids breaking a site's own first-party resources.
 */
object AdBlocker {

    private data class Rule(val wholeDomain: Boolean, val pathPrefixes: List<String>) {
        fun matches(path: String): Boolean = wholeDomain || pathPrefixes.any { path.startsWith(it) }
    }

    // Built once in init(), then published as immutable maps; read from WebView network threads.
    @Volatile private var rules: Map<String, Rule> = emptyMap()
    @Volatile private var exceptions: Set<String> = emptySet()
    @Volatile private var entityByDomain: Map<String, String> = emptyMap()

    // Global lifetime counter. Per-page stats live on each Tab.
    private val _totalBlockedCount = MutableStateFlow(0L)
    val totalBlockedCount: StateFlow<Long> = _totalBlockedCount.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            loadRules(context)
            loadEntities(context)
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRules(context: Context) {
        val wholeDomains = HashSet<String>()
        val paths = HashMap<String, MutableList<String>>()
        val allow = HashSet<String>()

        context.assets.open("hosts.txt").bufferedReader().useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim().lowercase()
                when {
                    line.isEmpty() || line.startsWith("#") -> Unit
                    line.startsWith("@@") -> allow.add(line.removePrefix("@@"))
                    '/' in line -> {
                        val host = line.substringBefore('/')
                        paths.getOrPut(host) { mutableListOf() }.add("/" + line.substringAfter('/'))
                    }
                    else -> wholeDomains.add(line)
                }
            }
        }

        rules = (wholeDomains + paths.keys).associateWith { host ->
            Rule(wholeDomain = host in wholeDomains, pathPrefixes = paths[host].orEmpty())
        }
        exceptions = allow
    }

    /** Line format: `Entity Name: domain1 domain2 ...` */
    private fun loadEntities(context: Context) {
        val map = HashMap<String, String>()
        context.assets.open("entities.txt").bufferedReader().useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#") || ':' !in line) return@forEach
                val entity = line.substringBefore(':').trim()
                line.substringAfter(':').trim().split(Regex("\\s+")).forEach { domain ->
                    if (domain.isNotEmpty()) map[registrableDomain(domain.lowercase())] = entity
                }
            }
        }
        entityByDomain = map
    }

    /**
     * Returns the blocklist entry matching [uri], or null if the request should be allowed.
     * [pageHost] is the host of the top-level page; requests first-party to it are never blocked.
     * Thread-safe; called from WebView's background network threads.
     */
    fun matchBlockedDomain(uri: Uri, pageHost: String?): String? {
        val host = uri.host?.lowercase() ?: return null
        if (pageHost != null && isSameParty(host, pageHost.lowercase())) return null

        val path = uri.encodedPath?.lowercase() ?: "/" // rules are stored lowercase
        var testDomain = host
        while (testDomain.contains(".")) {
            // Most specific match wins, so an exception on a subdomain beats a parent rule
            if (testDomain in exceptions) return null
            rules[testDomain]?.let { rule -> if (rule.matches(path)) return testDomain }
            testDomain = testDomain.substringAfter(".", "")
        }
        return null
    }

    private fun isSameParty(requestHost: String, pageHost: String): Boolean {
        val a = registrableDomain(requestHost)
        val b = registrableDomain(pageHost)
        if (a == b) return true
        val entityA = entityByDomain[a] ?: return false
        return entityA == entityByDomain[b]
    }

    /**
     * Approximates eTLD+1 (e.g. news.bbc.co.uk -> bbc.co.uk). Android has no public suffix
     * list API; this covers common two-part country suffixes, which is enough for party checks.
     */
    internal fun registrableDomain(host: String): String {
        if (host.isEmpty() || host[0].isDigit() && host.all { it.isDigit() || it == '.' }) return host
        if (':' in host) return host // IPv6
        val labels = host.trimEnd('.').split('.')
        if (labels.size <= 2) return host
        val lastTwo = labels.takeLast(2).joinToString(".")
        val keep = if (lastTwo in MULTI_PART_SUFFIXES) 3 else 2
        return labels.takeLast(keep).joinToString(".")
    }

    private val MULTI_PART_SUFFIXES = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk", "me.uk",
        "com.au", "net.au", "org.au", "edu.au", "gov.au",
        "co.nz", "org.nz", "co.jp", "ne.jp", "or.jp", "ac.jp",
        "co.kr", "or.kr", "co.in", "net.in", "org.in", "co.za", "co.il",
        "com.br", "com.mx", "com.ar", "com.co", "com.tr", "com.cn", "com.hk",
        "com.tw", "com.sg", "com.my", "com.ph", "com.vn", "com.ua", "com.pl",
        "co.id", "co.th"
    )

    /** Atomically increments the global counter (safe from concurrent network threads). */
    fun recordBlock() {
        _totalBlockedCount.update { it + 1 }
    }

    /**
     * Instantly terminates network dispatch by returning an empty 200 HTTP response.
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            mapOf("Cache-Control" to "no-store, no-cache, must-revalidate"),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
