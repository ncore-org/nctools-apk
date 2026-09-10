# nctools — Android app

**Private document tools, on your device.**

The official Android companion to [nctools.eu](https://nctools.eu). Built with
**Kotlin + Gradle (Kotlin DSL)**, Jetpack Compose, MVVM + Hilt. All conversions
run locally — your files never leave your device, matching the website's
privacy promise.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blueviolet)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/minSdk-26-brightgreen)](https://developer.android.com)

---

## Features

| Area | Detail |
|------|--------|
| **Tools** | Full website parity: paste→PDF, PDF→Word, PDF→Excel, PDF→OCR, OCR→text, photo scanner, images→PDF, merge PDF (`ToolsEngine`) |
| **Privacy** | Zero uploads — PDFBox & Tesseract run on-device |
| **Auth** | Register / login / Google sign-in against the shared nctools.eu backend (`AuthRepository`) |
| **Dashboard** | Tool grid + account identity, mirroring the web dashboard |
| **Google Drive** | Connect Drive, save conversions into a private `nctools` folder (`drive.file` scope) |
| **Ads** | Every conversion gated behind one 15s interstitial (`AdGate`), GDPR consent via UMP/UserPrefs |
| **Updates** | VPS update API + SHA-256 verified APK delivery (`UpdateManager`) — anti-crack: checksum-mismatched APKs are rejected |
| **Security** | Encrypted Keystore-backed bearer token (`AuthTokenStore`) |

## Tech stack

- Language: **Kotlin 2.0.21**, JVM 17
- UI: **Jetpack Compose** (Material 3), Navigation Compose
- DI: **Hilt**
- Network: Retrofit + OkHttp + Moshi
- PDF: **pdfbox-android**; OCR: **tess-two**; Spreadsheet/Word: minimal OPC writers built in
- Drive: Google Play Services Auth + Drive API v3
- Ads: Google Mobile Ads SDK; Crashlytics + Analytics

## Repo layout

```
app/src/main/java/eu/nctools/app/
├── NctoolsApp.kt, MainActivity.kt
├── core/
│   ├── ads/AdGate.kt            # 15s ad gate before each export
│   ├── consent/ConsentManager.kt # GDPR UMP wrapper
│   ├── tools/ToolsEngine.kt     # all 8 conversions (PDFBox)
│   ├── tools/OcrEngine.kt       # Tesseract OCR
│   ├── tools/ToolCatalog.kt     # static registry (matches web catalog)
│   └── update/UpdateManager.kt  # verified update delivery
├── data/
│   ├── api/…                    # Retrofit: NctoolsApi, AuthApi, AuthInterceptor
│   ├── auth/…                   # AuthRepository, AuthTokenStore (encrypted), UserPrefs
│   ├── drive/DriveRepository.kt # Google Drive file upload
│   └── model/                   # DTOs
└── ui/
    ├── AppRoot.kt               # nav graph
    ├── consent/ConsentGate.kt   # first-run GDPR
    ├── auth/…                   # login / register
    ├── dashboard/…              # tool grid + Drive link
    └── tools/ToolDetailScreen.kt # per-tool UI (ad-gated)
```

## Build

```bash
# In Android Studio: File → Open → this folder, then Run.
# Or CLI:
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release (R8 + shrinking)
```

Prerequisites:
1. **Android SDK** (platform 35), JDK 17.
2. **`app/google-services.json`** — from Firebase (Analytics + Crashlytics). It is
   intentionally git-ignored (contains API keys). Without it the Firebase plugins
   do not configure; see `.example` in this repo.
3. **`local.properties`** with `sdk.dir=…` (Studio generates it automatically).
4. **AdMob unit ID** — set the real interstitial id in `app/build.gradle.kts`
   (`ADS_UNIT_ID`) and the app-level id in `AndroidManifest.xml`. The shipped
   value is a placeholder.
5. **Backend** — the app talks to `https://nctools.eu` (`BuildConfig.API_ORIGIN`).

## Tesseract data

OCR requires `eng.traineddata` under `app/src/main/assets/tessdata/`. Download
from the [tessdata repo](https://github.com/tesseract-ocr/tessdata) (~15 MB)
and drop it at that path. Without it OCR degrades gracefully.

## Update & anti-crack

`UpdateManager` registers each install with the VPS endpoint
`/api/mobile/devices` (device fingerprint + install token), then polls
`/api/mobile/updates`. When a newer APK exists, the backend returns a **signed
descriptor with SHA-256**; the client downloads and verifies every byte before
offering install. A tampered or re-signed APK fails the checksum and is
rejected — cracking requires the server-issued valid descriptor.

## License

MIT — see [LICENSE](LICENSE). Copyright © 2026 Nit Corporation s. r. o.