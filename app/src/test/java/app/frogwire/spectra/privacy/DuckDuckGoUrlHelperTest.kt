package app.frogwire.spectra.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Address-bar parsing. Inputs that start with http(s):// go through TrackingParamStripper,
 * which uses android.net.Uri and can't run in a plain JVM test, so they aren't covered here.
 */
class DuckDuckGoUrlHelperTest {

    @Test
    fun `blank input goes home`() {
        assertEquals("about:blank", DuckDuckGoUrlHelper.resolveQuery(""))
        assertEquals("about:blank", DuckDuckGoUrlHelper.resolveQuery("   "))
    }

    @Test
    fun `bare domains open over https`() {
        assertEquals("https://wikipedia.org", DuckDuckGoUrlHelper.resolveQuery("wikipedia.org"))
        assertEquals("https://news.ycombinator.com", DuckDuckGoUrlHelper.resolveQuery("  news.ycombinator.com "))
        assertEquals("https://localhost:3000", DuckDuckGoUrlHelper.resolveQuery("localhost:3000"))
    }

    @Test
    fun `words become a DuckDuckGo search`() {
        val url = DuckDuckGoUrlHelper.resolveQuery("crystal elk")
        assertTrue(url, url.startsWith("https://duckduckgo.com/?q=crystal+elk&"))
    }

    @Test
    fun `a phrase with a dot and spaces is a search, not an address`() {
        val url = DuckDuckGoUrlHelper.resolveQuery("what is node.js")
        assertTrue(url, url.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun `bangs are kept for DuckDuckGo to resolve`() {
        val url = DuckDuckGoUrlHelper.resolveQuery("!w privacy")
        assertTrue(url, url.startsWith("https://duckduckgo.com/?q=%21w+privacy&"))
    }

    @Test
    fun `search URLs display as the query`() {
        val url = DuckDuckGoUrlHelper.resolveQuery("crystal elk")
        assertEquals("DDG: crystal elk", DuckDuckGoUrlHelper.formatDisplayUrl(url))
    }

    @Test
    fun `page URLs display without scheme, www or trailing slash`() {
        assertEquals("example.com", DuckDuckGoUrlHelper.formatDisplayUrl("https://www.example.com/"))
        assertEquals("example.com/page", DuckDuckGoUrlHelper.formatDisplayUrl("http://example.com/page"))
    }
}
