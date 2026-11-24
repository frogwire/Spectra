package com.duck.privacy.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.duck.privacy.browser.tabs.TabManager

/**
 * Keeps tabs (and their WebViews) alive across Activity recreation
 * (dark-mode toggle, locale change, fold/unfold, multi-window resize).
 */
class BrowserViewModel(app: Application) : AndroidViewModel(app) {
    val tabManager = TabManager(app)

    override fun onCleared() {
        tabManager.shutdown()
    }
}
