# Play Store Release Report

## Build Status
**Status:** SUCCESS
**Build Tool:** Gradle (assembleRelease)
**Minify/R8:** Enabled (with JavascriptInterface proguard rules)
**Signer:** APK Signature Scheme v1, v2, v3 (apksigner verified)

## Application Details
**Package Name:** `com.guptrix.app`
**Version Code:** `1`
**Version Name:** `1.0`
**Min SDK:** `23` (Android 6.0 Marshmallow)
**Target SDK:** `34` (Android 14)

## Keystore Details
**Alias:** `guptrix`
**Key Size:** RSA 4096-bit
**Validity:** 10,000 Days
**Location:** `android-app/keystore/release.keystore`

## Release Artifacts (APKs)
Since this app is a pure Kotlin/Java application utilizing the system WebView and contains no bundled native C++ libraries (`.so` files), the ABI splits (`arm64-v8a` and `armeabi-v7a`) generated identical universal APKs. 

**Locations:**
* `android-app/app/build/outputs/apk/release/app-arm64-v8a-release.apk`
* `android-app/app/build/outputs/apk/release/app-armeabi-v7a-release.apk`
* `android-app/app/build/outputs/apk/release/app-universal-release.apk`

### Hashes (SHA-256)
```text
d01fc92cd10054a5e2b3911d5906ee662d1c4bfe5a7e1873522a2ccf23f4a87b  app-arm64-v8a-release.apk
d01fc92cd10054a5e2b3911d5906ee662d1c4bfe5a7e1873522a2ccf23f4a87b  app-armeabi-v7a-release.apk
d01fc92cd10054a5e2b3911d5906ee662d1c4bfe5a7e1873522a2ccf23f4a87b  app-universal-release.apk
```

## Signature Verification (`apksigner verify -v`)
```text
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```
*Note: Warnings about `META-INF/*.version` files not being protected by signature are standard for AndroidX libraries and do not affect Play Store readiness.*

## Play Store Readiness Checklist
- [x] Application ID verified (`com.guptrix.app`)
- [x] Version Code and Version Name set
- [x] Target SDK set to 34 (Required by Play Store)
- [x] Min SDK set to 23
- [x] ProGuard/R8 Minification Enabled
- [x] Critical JavascriptInterfaces kept via custom Proguard rules
- [x] Custom Keystore Generated (RSA 4096-bit, 10,000 days validity)
- [x] APKs Signed with v1, v2, and v3 schemas
- [x] No Debug configurations remaining in release build
- [x] No plaintext HTTP traffic (Network Security Config / WebView MIXED_CONTENT_NEVER_ALLOW)
- [x] WebView Security Hardened (No file access, No content access)
