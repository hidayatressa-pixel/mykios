# MYKIOS Sale-Readiness Hardening

## Fixed in this package
- Removed profile-name developer bypass that granted PRO.
- Split app into `free` and `pro` build flavors. Legacy PRO builds remain supported; the free build can now also unlock PRO only from a RevenueCat-verified entitlement.
- Removed the legacy WebView upgrade/payment flow. Upgrade is now a native RevenueCat Offering -> Google Play purchase flow with verified CustomerInfo entitlement.
- Replaced plain-text password/PIN storage with salted PBKDF2-HMAC-SHA256 hashes. Existing plain-text PINs migrate after the first successful verification.
- Removed Room `fallbackToDestructiveMigration()` to prevent silent database deletion on missing migrations.
- Disabled cleartext HTTP and added a Network Security Config that denies cleartext by default.
- Removed emulator-local `http://10.0.2.2:5000/` production endpoint; optional AI client now fails closed against a non-routable HTTPS placeholder until a real HTTPS backend is configured.
- Added the missing FileProvider for camera profile photos and scoped it to app-specific `Pictures/` only.
- Removed unnecessary READ_EXTERNAL_STORAGE permission. WRITE_EXTERNAL_STORAGE remains only through API 28 for legacy export compatibility.
- Excluded authentication/session preferences from Android cloud/device backup.
- Enabled R8 minification for release builds.

## Distribution model
- `free` flavor: feature-limited build; unlocks PRO only when RevenueCat reports the configured `pro` entitlement active.
- `pro` flavor: legacy full paid APK/direct-distribution edition remains supported.
- Native purchase, restore-purchases, Offering price display, launch refresh, CustomerInfo listener, and cached verified entitlement are implemented.
- Production activation still requires the RevenueCat public Android SDK key plus a configured Offering/package/product linked to the `pro` entitlement in the store dashboard.

## Required verification on a developer machine
The provided environment could not download Gradle 9.4.1, so run:

    gradlew.bat clean assembleFreeRelease assembleProRelease

Then test both APKs on a physical Android 12+ device: registration, PIN lock/unlock, POS transaction, stock changes, debt, CSV/XLSX import/export, receipt generation/printing, barcode scan, backup/restore, profile camera/gallery, and app restart.

## Database note
The current Room database is version 5. Destructive fallback was removed. Future schema changes must include explicit Room migrations before increasing the version.

## V3 compile hardening
- Removed orphaned Retrofit/cloud-AI client (`RetrofitClient`, `AiService`, `LocalAiEngine`). The in-app `AiEngine` remains local/offline-first.
- Removed unused Retrofit dependencies from the Android app.
- Added a Room index for `detail_transaksi.transaksiId` to avoid full scans on parent updates.
- Enabled Room schema export to `app/schemas` through KSP for migration review/versioning.


## RevenueCat / Shipaton hardening (2026-09-18)
- Added native Offering loading and dynamic store price display.
- Added PurchaseParams-based checkout with explicit cancel/error states.
- Added user-triggered Restore Purchases.
- Added entitlement refresh at application start and when MainActivity resumes.
- Added UpdatedCustomerInfoListener so verified entitlement changes update local feature gating.
- Removed the obsolete local HTML/WebView upgrade screen from the active upgrade flow.
- PRO feature gates continue to use SessionManager.isPro(), which now accepts either the legacy PRO flavor or the last RevenueCat-verified active entitlement.
- CI builds intentionally work without a production RevenueCat key; an empty key fails closed and cannot unlock PRO.
