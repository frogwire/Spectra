package app.frogwire.spectra.privacy

import org.junit.Assert.assertEquals
import org.junit.Test

/** Registrable-domain matching (decides first vs third party) and tracker categories. */
class DomainAndCategoryTest {

    private fun domain(host: String) = AdBlocker.registrableDomain(host)

    @Test
    fun `subdomains collapse to the registrable domain`() {
        assertEquals("example.com", domain("example.com"))
        assertEquals("example.com", domain("www.example.com"))
        assertEquals("example.com", domain("a.b.c.example.com"))
    }

    @Test
    fun `multi-part suffixes keep three labels`() {
        assertEquals("bbc.co.uk", domain("www.bbc.co.uk"))
        assertEquals("example.com.au", domain("shop.example.com.au"))
    }

    @Test
    fun `IP addresses and single labels are left alone`() {
        assertEquals("192.168.1.10", domain("192.168.1.10"))
        assertEquals("::1", domain("::1"))
        assertEquals("localhost", domain("localhost"))
    }

    @Test
    fun `a trailing dot is ignored`() {
        assertEquals("example.com", domain("www.example.com."))
    }

    @Test
    fun `tracker domains get the right category`() {
        assertEquals(TrackerCategory.ADS, TrackerAnalytics.classifyCategory("ad.doubleclick.net"))
        assertEquals(TrackerCategory.ANALYTICS, TrackerAnalytics.classifyCategory("www.google-analytics.com"))
        assertEquals(TrackerCategory.FINGERPRINTING, TrackerAnalytics.classifyCategory("h.online-metrix.threatmetrix.com"))
        assertEquals(TrackerCategory.SOCIAL, TrackerAnalytics.classifyCategory("connect.facebook.net"))
        assertEquals(TrackerCategory.OTHER, TrackerAnalytics.classifyCategory("cdn.example.org"))
    }

    @Test
    fun `categories ignore letter case`() {
        assertEquals(TrackerCategory.ADS, TrackerAnalytics.classifyCategory("AD.DoubleClick.NET"))
    }
}
