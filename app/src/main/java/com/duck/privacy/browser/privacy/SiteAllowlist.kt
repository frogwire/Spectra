package com.duck.privacy.browser.privacy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Spectra Privacy Settings & Site Protection Allowlist.
 *
 * Manages per-site protection toggles and global feature switches matching the Spectra Privacy Panel:
 * 1. Block trackers (Third-party trackers and ads)
 * 2. Fingerprint shield (Randomizes canvas, audio and fonts)
 * 3. Block third-party cookies (First-party cookies still work)
 * 4. WebRTC leak guard (Hides local IP)
 * 5. Wipe cookies on close (Incognito+ behaviour for standard windows)
 */
class SiteAllowlist(context: Context) {

    data class State(
        val standardOff: Set<String> = emptySet(),
        val incognitoOff: Set<String> = emptySet(),
        val incognitoOn: Set<String> = emptySet(),
        val blockTrackers: Boolean = true,
        val fingerprintShield: Boolean = true,
        val blockThirdPartyCookies: Boolean = true,
        val webRtcLeakGuard: Boolean = true,
        val wipeCookiesOnClose: Boolean = false,
        /** Phishing/malware warnings via Android System WebView's Google Safe Browsing. */
        val safeBrowsing: Boolean = true
    ) {
        fun isOff(key: String, incognito: Boolean): Boolean =
            if (!incognito) key in standardOff
            else key !in incognitoOn && (key in standardOff || key in incognitoOff)

        /** Every site key whose protection is off in this mode (same rule as [isOff]). */
        fun offSites(incognito: Boolean): Set<String> =
            if (!incognito) standardOff else (standardOff + incognitoOff) - incognitoOn

        fun isSiteProtected(key: String, incognito: Boolean): Boolean =
            blockTrackers && !isOff(key, incognito)
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        State(
            standardOff = prefs.getStringSet(KEY_SITES, emptySet())!!.toSet(),
            blockTrackers = prefs.getBoolean(KEY_BLOCK_TRACKERS, true),
            fingerprintShield = prefs.getBoolean(KEY_FINGERPRINT_SHIELD, true),
            blockThirdPartyCookies = prefs.getBoolean(KEY_BLOCK_3P_COOKIES, true),
            webRtcLeakGuard = prefs.getBoolean(KEY_WEBRTC_LEAK_GUARD, true),
            wipeCookiesOnClose = prefs.getBoolean(KEY_WIPE_COOKIES_CLOSE, false),
            safeBrowsing = prefs.getBoolean(KEY_SAFE_BROWSING, true)
        )
    )
    val state: StateFlow<State> = _state.asStateFlow()

    fun keyFor(host: String): String = AdBlocker.registrableDomain(host.lowercase())

    fun isProtectionDisabled(host: String?, incognito: Boolean): Boolean {
        if (host.isNullOrEmpty()) return !_state.value.blockTrackers
        return _state.value.isOff(keyFor(host), incognito) || !_state.value.blockTrackers
    }

    fun setProtectionEnabled(host: String, incognito: Boolean, enabled: Boolean) {
        val key = keyFor(host)
        if (incognito) {
            _state.update {
                it.copy(
                    incognitoOff = if (enabled) it.incognitoOff - key else it.incognitoOff + key,
                    incognitoOn = if (enabled && key in it.standardOff) it.incognitoOn + key else it.incognitoOn - key
                )
            }
        } else {
            _state.update {
                it.copy(standardOff = if (enabled) it.standardOff - key else it.standardOff + key)
            }
            persist()
        }
    }

    fun setBlockTrackers(enabled: Boolean) {
        _state.update { it.copy(blockTrackers = enabled) }
        prefs.edit { putBoolean(KEY_BLOCK_TRACKERS, enabled) }
    }

    fun setFingerprintShield(enabled: Boolean) {
        _state.update { it.copy(fingerprintShield = enabled) }
        prefs.edit { putBoolean(KEY_FINGERPRINT_SHIELD, enabled) }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        _state.update { it.copy(blockThirdPartyCookies = enabled) }
        prefs.edit { putBoolean(KEY_BLOCK_3P_COOKIES, enabled) }
    }

    fun setWebRtcLeakGuard(enabled: Boolean) {
        _state.update { it.copy(webRtcLeakGuard = enabled) }
        prefs.edit { putBoolean(KEY_WEBRTC_LEAK_GUARD, enabled) }
    }

    fun setWipeCookiesOnClose(enabled: Boolean) {
        _state.update { it.copy(wipeCookiesOnClose = enabled) }
        prefs.edit { putBoolean(KEY_WIPE_COOKIES_CLOSE, enabled) }
    }

    fun clearIncognito() {
        _state.update { it.copy(incognitoOff = emptySet(), incognitoOn = emptySet()) }
    }

    fun setSafeBrowsing(enabled: Boolean) {
        _state.update { it.copy(safeBrowsing = enabled) }
        prefs.edit { putBoolean(KEY_SAFE_BROWSING, enabled) }
    }

    /** Clears the per-site "protection off" lists (saved and incognito). Keeps the five switches. */
    fun clearAll() {
        _state.update { it.copy(standardOff = emptySet(), incognitoOff = emptySet(), incognitoOn = emptySet()) }
        prefs.edit { remove(KEY_SITES) }
    }

    private fun persist() {
        prefs.edit { putStringSet(KEY_SITES, _state.value.standardOff) }
    }

    private companion object {
        const val PREFS_NAME = "spectra_privacy_settings"
        const val KEY_SITES = "protection_disabled_sites"
        const val KEY_BLOCK_TRACKERS = "block_trackers"
        const val KEY_FINGERPRINT_SHIELD = "fingerprint_shield"
        const val KEY_BLOCK_3P_COOKIES = "block_3p_cookies"
        const val KEY_WEBRTC_LEAK_GUARD = "webrtc_leak_guard"
        const val KEY_WIPE_COOKIES_CLOSE = "wipe_cookies_close"
        const val KEY_SAFE_BROWSING = "safe_browsing"
    }
}
