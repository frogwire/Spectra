package app.frogwire.spectra.privacy

import android.net.Uri

/**
 * Removes click-tracking / campaign parameters from navigation URLs
 * (e.g. ?utm_source=..&fbclid=..), which let sites and ad networks link your visit to a click.
 */
object TrackingParamStripper {

    private val exactNames = setOf(
        // Ad click IDs
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
        "twclid", "ttclid", "li_fat_id", "igshid", "epik", "rdt_cid", "srsltid",
        // Email / marketing automation
        "mc_cid", "mc_eid", "_hsenc", "_hsmi", "mkt_tok", "vero_id", "vero_conv",
        "oly_anon_id", "oly_enc_id", "rb_clickid", "s_cid", "_openstat", "wickedid"
    )

    private val prefixes = listOf("utm_", "pk_", "mtm_")

    private fun isTrackingParam(name: String): Boolean {
        val lower = name.lowercase()
        return lower in exactNames || prefixes.any { lower.startsWith(it) }
    }

    /**
     * Returns [url] without tracking parameters. Works on the raw encoded query so the
     * remaining parameters keep their exact original encoding.
     */
    fun strip(url: String): String {
        val uri = Uri.parse(url)
        if (uri.isOpaque) return url
        val query = uri.encodedQuery ?: return url

        val kept = query.split('&').filter { part ->
            val name = Uri.decode(part.substringBefore('='))
            part.isNotEmpty() && !isTrackingParam(name)
        }
        if (kept.size == query.split('&').size) return url

        return uri.buildUpon()
            .encodedQuery(kept.joinToString("&").ifEmpty { null })
            .build()
            .toString()
    }
}
