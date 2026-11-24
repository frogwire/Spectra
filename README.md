# Spectra — Privacy-First Browser (Android) 🦌🛡️

Light passes through a crystal and leaves nothing behind. **Spectra** is a lightweight, ultra-fast, and privacy-first Android browser built with **Jetpack Compose** and **Android System WebView**, featuring **DuckDuckGo** as the default search engine and the **Crystal Elk** mascot identity.

> “Browse freely, leave no fingerprint.”

---

## ⚡ Core Features & Privacy Highlights

* **Prism Glow Design System:** Dark-first UI (`#0f0e15`), Violet (`#6d4aff`) → Cyan (`#5bc0eb`) prism glow toolbar indicators, and high-contrast accessibility standards.
* **Socket-Level Ad & Tracker Blocker:** Native, in-browser engine using DuckDuckGo Tracker Radar & EasyPrivacy hostlists. Blocks telemetry and ads *before* network requests leave the device.
* **Spectra Privacy Panel (5-Toggle Protection):**
  - **Block trackers:** Third-party trackers and ads.
  - **Fingerprint shield:** Randomizes canvas, audio, and font readouts.
  - **Block third-party cookies:** First-party cookies still work seamlessly.
  - **WebRTC leak guard:** Prevents local IP address leaks.
  - **Wipe cookies on close:** Applies optional Incognito+ wipe-on-close to every window.
* **3-State Security Shield:** Dynamic address bar indicator:
  - 🛡️ **Protected** (Teal `#2ec4a6`) — All protections active.
  - ⚠️ **Partial Protection** (Yellow `#f2c14e`) — Per-site protection paused or mixed content.
  - ❌ **Connection Warning** (Red `#ff6b6b`) — Insecure cleartext HTTP.
* **Incognito+ Mode:**
  - Violet-themed private window (`FLAG_SECURE` enabled to block screenshots & switcher previews).
  - Ephemeral memory partition: Zero disk cache (`LOAD_NO_CACHE`), zero persistent cookies, no form autofill.
  - Keyboard privacy flag (`IME_FLAG_NO_PERSONALIZED_LEARNING`).
* **One-Tap Purge (`🔥`):** Destroys active cookies, WebStorage, RAM caches, and open tabs in a single tap.
* **DuckDuckGo Omnibar:** Ergonomic bottom toolbar featuring DuckDuckGo `!bang` shortcuts (`!w`, `!gh`, `!yt`).

---

## 📁 Project Architecture

```
privacy-browser/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── hosts.txt            <-- DDG Tracker Radar & EasyPrivacy blocklist
│   │   │   └── entities.txt         <-- Entity mapping for 1st vs 3rd party rules
│   │   ├── java/com/duck/privacy/browser/
│   │   │   ├── MainActivity.kt      <-- Core Activity & Compose container
│   │   │   ├── privacy/
│   │   │   │   ├── AdBlocker.kt     <-- Native tracker blocker engine
│   │   │   │   ├── PrivacyWebViewClient.kt <-- Request interceptor & HTTPS upgrade
│   │   │   │   ├── DuckDuckGoUrlHelper.kt  <-- Smart URL vs. DDG search resolver with !bangs
│   │   │   │   ├── SiteAllowlist.kt <-- 5-toggle Spectra Privacy Panel state manager
│   │   │   │   └── TrackingParamStripper.kt <-- Click-tracking parameter cleaner
│   │   │   ├── tabs/
│   │   │   │   ├── Tab.kt           <-- Reactive tab state model
│   │   │   │   └── TabManager.kt    <-- Dual-segment tab manager (Standard vs. Incognito+)
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   ├── HomeView.kt            <-- Crystal Elk mascot & search capsule
│   │   │       │   ├── Omnibar.kt             <-- Bottom bar, 3-state shield & prism glow
│   │   │       │   ├── PrivacyShieldSheet.kt  <-- Spectra 5-toggle Privacy Panel
│   │   │       │   ├── SettingsSheet.kt       <-- Privacy Dashboard & allowlist manager
│   │   │       │   └── TabsOverviewSheet.kt   <-- Dual-segment tab switcher (Incognito+)
│   │   │       └── theme/
│   │   │           └── Theme.kt     <-- Spectra Prism Dark palette (#0F0E15 / Violet & Cyan)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🔤 Fonts

Space Grotesk (wordmark, headings) and Inter (interface text) are bundled, not fetched at runtime.
Put the static `.ttf` files in `app/src/main/assets/fonts/` with the names listed in the `README.txt` there.
Missing files fall back to the system sans-serif, so the app builds without them.

---

## 🚀 How to Build & Run

### 1. Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 3. Test the page-shield script (Node.js only)
```bash
node app/src/test/js/page_shields.test.js
```
Fingerprint shield and WebRTC leak guard live in `app/src/main/assets/page_shields.js`, injected at
document start into every frame (needs Android System WebView 90+; older WebViews show them as unavailable).

### 4. Install on Connected Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
