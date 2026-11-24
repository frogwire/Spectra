package app.frogwire.spectra.privacy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Builds the document-start script (assets/page_shields.js) that enforces the
 * fingerprint shield and the WebRTC leak guard inside every page and iframe.
 *
 * Noise seeds are random and kept only in memory. Standard and Incognito+ tabs get
 * different seeds, so a site can't link an Incognito+ visit to a normal one; the
 * Incognito+ seed is renewed whenever that session is wiped, and both are renewed on burn.
 */
class PageShields(context: Context) {

    private val template: String =
        context.assets.open("page_shields.js").bufferedReader().use { it.readText() }

    private var standardSeed = newSeed()
    private var incognitoSeed = newSeed()

    fun renewIncognitoSeed() {
        incognitoSeed = newSeed()
    }

    fun renewAllSeeds() {
        standardSeed = newSeed()
        incognitoSeed = newSeed()
    }

    /** Returns the script to inject, or null when both shields are off. */
    fun buildScript(
        fingerprintShield: Boolean,
        webRtcLeakGuard: Boolean,
        sitesWithProtectionOff: Collection<String>,
        isIncognito: Boolean
    ): String? {
        if (!fingerprintShield && !webRtcLeakGuard) return null
        val config = JSONObject()
            .put("fp", fingerprintShield)
            .put("webrtc", webRtcLeakGuard)
            .put("seed", if (isIncognito) incognitoSeed else standardSeed)
            .put("off", JSONArray(sitesWithProtectionOff.sorted()))
        return template.replace(PLACEHOLDER, config.toString())
    }

    private companion object {
        const val PLACEHOLDER = "__SPECTRA_CONFIG__"
        private val random = SecureRandom()

        fun newSeed(): String {
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
