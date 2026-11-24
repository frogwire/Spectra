package app.frogwire.spectra.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Per-site "protection off" rules (SiteAllowlist.State), standard vs Incognito+. */
class SiteProtectionStateTest {

    private val empty = SiteAllowlist.State()

    @Test
    fun `every site is protected by default`() {
        assertFalse(empty.isOff("example.com", incognito = false))
        assertFalse(empty.isOff("example.com", incognito = true))
        assertTrue(empty.isSiteProtected("example.com", incognito = false))
    }

    @Test
    fun `defaults have every protection on except wipe on close`() {
        assertTrue(empty.blockTrackers)
        assertTrue(empty.fingerprintShield)
        assertTrue(empty.blockThirdPartyCookies)
        assertTrue(empty.webRtcLeakGuard)
        assertTrue(empty.safeBrowsing)
        assertFalse(empty.wipeCookiesOnClose)
    }

    @Test
    fun `turning a site off in standard mode carries into Incognito+`() {
        val state = empty.copy(standardOff = setOf("broken.com"))
        assertTrue(state.isOff("broken.com", incognito = false))
        assertTrue(state.isOff("broken.com", incognito = true))
    }

    @Test
    fun `turning a site off in Incognito+ does not touch standard mode`() {
        val state = empty.copy(incognitoOff = setOf("broken.com"))
        assertFalse(state.isOff("broken.com", incognito = false))
        assertTrue(state.isOff("broken.com", incognito = true))
    }

    @Test
    fun `Incognito+ can turn a standard-off site back on for its session`() {
        val state = empty.copy(standardOff = setOf("broken.com"), incognitoOn = setOf("broken.com"))
        assertTrue(state.isOff("broken.com", incognito = false))
        assertFalse(state.isOff("broken.com", incognito = true))
    }

    @Test
    fun `offSites matches isOff in both modes`() {
        val state = empty.copy(
            standardOff = setOf("a.com", "b.com"),
            incognitoOff = setOf("c.com"),
            incognitoOn = setOf("b.com")
        )
        assertEquals(setOf("a.com", "b.com"), state.offSites(incognito = false))
        assertEquals(setOf("a.com", "c.com"), state.offSites(incognito = true))
        for (site in listOf("a.com", "b.com", "c.com", "d.com")) {
            for (incognito in listOf(false, true)) {
                assertEquals(
                    "$site incognito=$incognito",
                    state.isOff(site, incognito),
                    site in state.offSites(incognito)
                )
            }
        }
    }

    @Test
    fun `a site is not protected when tracker blocking is off globally`() {
        val state = empty.copy(blockTrackers = false)
        assertFalse(state.isSiteProtected("example.com", incognito = false))
    }
}
