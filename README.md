# Bowly — mobile client

Kotlin Multiplatform client for **[Bowly](https://github.com/DunderTheDragon/bowlyAppBackend)** — a self-hosted calorie tracker with virtual batch meals (*patelnie*).

Targets **Android** (APK) and **iOS** (Xcode).

The backend API runs separately — see the [backend repository](https://github.com/DunderTheDragon/bowlyAppBackend).

---

## Download Android APK

**[Download latest APK](https://github.com/DunderTheDragon/bowlyApp/releases/latest/download/composeApp-debug.apk)**

All releases: [github.com/DunderTheDragon/bowlyApp/releases](https://github.com/DunderTheDragon/bowlyApp/releases)

After installing the app, set your backend URL in the app (e.g. `http://192.168.1.171:8742` for a LAN server).

---

## Publishing a new release

Releases are built automatically by GitHub Actions.

**Option A — tag push (creates a public Release with APK):**

```bash
git tag v1.0.0
git push origin v1.0.0
```

**Option B — manual build in Actions:**

GitHub → **Actions** → **Release APK** → **Run workflow** → download the APK from workflow artifacts.

---

## Build locally

### Android

```bash
./gradlew :composeApp:assembleDebug
```

Output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### iOS

Open [`iosApp`](./iosApp) in Xcode and run from there.

---

## Project layout

| Path | Purpose |
|------|---------|
| [`composeApp/src/commonMain`](./composeApp/src/commonMain/kotlin) | Shared UI and business logic |
| [`composeApp/src/androidMain`](./composeApp/src/androidMain/kotlin) | Android-specific code |
| [`composeApp/src/iosMain`](./composeApp/src/iosMain/kotlin) | iOS-specific code |
| [`iosApp`](./iosApp/iosApp) | iOS app entry point |

---

## License

Same as the Bowly project — see backend repository for details.
