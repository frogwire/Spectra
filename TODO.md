# Spectra development to-do

Snapshot from Nov 24, 2026. The live version, with status dropdowns and comments, is
[Spectra development to-do](https://claude.ai/artifact/7WCrkmLHV1ScQSMsUUGLdf); update that one first.

## To do

### High
- [x] **Build in Android Studio and fix compile errors** (Build): four rounds of changes haven't been compiled: UI, fonts, settings and dashboard, page shields.
- [ ] **Put the project under git** (Build): no version control yet; the backups are a hand-made folder.
- [ ] **Test the page shields on a real phone** (Privacy): browserleaks.com canvas, webrtc and webgl pages, each switch on and off.
- [ ] **Rename the package from `com.duck.privacy.browser`** (Release): "duck" suggests DuckDuckGo; pick Spectra's own ID before any release.

### Medium
- [ ] **Check offscreen canvas image export on a device** (Privacy): the convertToBlob path isn't covered by the Node test.
- [ ] **Add the Space Grotesk and Inter font files** (Design): file names are in `app/src/main/assets/fonts/README.txt`; include both OFL licenses.
- [ ] **Make the crystal elk app icon** (Design): the manifest still uses Android's stock compass icon.
- [ ] **Add Kotlin unit tests** (Build): the README mentions testDebugUnitTest, but none exist. Start with SiteAllowlist and the URL helper.
- [ ] **Add a licenses screen** (Release): font OFL texts, AndroidX and other third-party credits.
- [ ] **Write `docs/privacy.md`** (Release): say what stays on the device: settings and tracker counts.
- [ ] **Run a trademark search on "Spectra"** (Release): for software and browsers.
- [ ] **Bring the project readme in line with the app** (Docs): the project readme describes a desktop ungoogled-Chromium build; the code is an Android WebView app.

### Low
- [ ] **Add a WebRTC strict mode** (Privacy): in the spec: turns WebRTC off entirely.
- [ ] **Extend the fingerprint shield to background workers** (Privacy): the riskiest fix; it may break sites.
- [ ] **Add a splash screen with the full elk** (Design): from the brand spec.
- [ ] **Privacy-preserving sync** (Privacy): Beta roadmap: local encrypted store only.
- [ ] **Decide how Spectra is funded** (Business): options: search affiliate, donations, sponsorships, with no tracking.
- [ ] **Delete `_ui-backup-2026-11-23`** (Build): once the build works and the project is in git.

## Done (Nov 23, 2026)

- Safe Browsing switch: new Security section in Settings; on by default.
- More fingerprint and WebRTC coverage: GPU model hidden from WebGL, local IPs removed from WebRTC stats, offscreen canvas noised.
- Decision: skip on-page font detection. The only fix changes text layout and would break sites.
- Fingerprint shield and WebRTC guard enforced: document-start script with per-site, per-session noise; Node tests in `app/src/test/js`.
- Third-party cookie switch works: was always blocked regardless of the switch; Incognito+ stays locked on.
- Settings no longer reset: burn and "turn all back on" used to wipe every switch to defaults.
- Wipe cookies on close made reliable: also wipes at next launch, covering a swiped-away app.
- Settings and dashboard redesigned: real switch states, dashboard now reachable, fake stats removed, reset asks first.
- Font support: Space Grotesk and Inter load from assets, falling back to the system font.
- UI refresh: shield icons per state, 48dp touch targets, drawn crystal elk, burn confirmation.
