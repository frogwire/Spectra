package app.frogwire.spectra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.frogwire.spectra.tabs.TabManager

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
