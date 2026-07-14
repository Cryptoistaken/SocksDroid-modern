# SocksDroid-modern — Fork Plan & Discovery

> **Source:** https://github.com/AlexeyRF/SocksDroid-modern
> **Fork goal:** Modernize the codebase (security, UI, architecture) and re-skin to look like SuperProxy / a modern proxy app.

---

## 1. Codebase Overview

### Project Structure

```
SocksDroid-modern/
├── build.gradle                          # Root build (AGP 9.2.1)
├── settings.gradle                       # Single :app module
├── gradle.properties                     # AndroidX, Jetifier
├── gradle/wrapper/
├── app/
│   ├── build.gradle                      # App config (targetSdk 34, minSdk 21)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── aidl/net/typeblog/socks/
│       │   └── IVpnService.aidl          # IPC interface (isRunning, stop)
│       ├── java/net/typeblog/socks/
│       │   ├── MainActivity.java         # Launcher (extends Activity — NOT AppCompatActivity)
│       │   ├── ProfileFragment.java      # Settings UI (deprecated PreferenceFragment)
│       │   ├── SocksVpnService.java      # Core VPN service (separate :vpn process)
│       │   ├── SocksApplication.java     # Material You DynamicColors init
│       │   ├── BootReceiver.java         # Auto-connect on BOOT_COMPLETED
│       │   ├── AppSelector.java          # Per-app proxy chooser dialog
│       │   ├── System.java               # JNI native method declarations
│       │   └── util/
│       │       ├── Constants.java
│       │       ├── Profile.java          # Data model backed by SharedPreferences
│       │       ├── ProfileFactory.java   # WeakReference cache
│       │       ├── ProfileManager.java   # CRUD singleton
│       │       ├── Routes.java           # VPN route builder (all/CN/RU)
│       │       └── Utility.java          # exec, kill, startVpn, pdnsd config
│       ├── jni/
│       │   ├── Android.mk               # 4 NDK modules
│       │   ├── Application.mk
│       │   ├── system.cpp               # JNI glue (sendfd, exec, getABI)
│       │   ├── libancillary/            # Unix socket fd-passing library
│       │   ├── badvpn/                  # BadVPN project (tun2socks, lwIP, etc.)
│       │   └── pdnsd/                   # DNS-over-TCP caching daemon
│       └── res/
│           ├── layout/                  # main.xml, app_item.xml
│           ├── xml/settings.xml         # PreferenceScreen (deprecated)
│           ├── values/                  # strings, arrays, routes, styles
│           ├── values-ru/               # Russian localization
│           ├── drawable*/               # Static launcher icon
│           └── mipmap*/                 # Launcher icons
```

### Tech Stack

| Category | Current | Desired |
|----------|---------|---------|
| Language | Java 17 + C/C++ | Kotlin + C/C++ (keep native) |
| Build | Gradle 9.4.1, AGP 9.2.1 | Same (current is fine) |
| UI | XML + PreferenceFragment (deprecated) | Jetpack Compose + Material3 |
| Architecture | Activity + Fragment + Service (no pattern) | MVVM with ViewModel + Kotlin Flows |
| Navigation | Raw FragmentManager | Jetpack Navigation Compose |
| Native | NDK Build (Android.mk) | Keep as-is (tun2socks + pdnsd) |
| IPC | AIDL | Keep AIDL (works fine) |

---

## 2. Critical Security Issues (MUST FIX)

| # | Issue | Location | Severity | Fix |
|---|-------|----------|----------|-----|
| 1 | Credentials passed as CLI args to native process — world-readable via /proc/pid/cmdline | SocksVpnService.java:244-248 | CRITICAL | Pipe credentials via the UNIX socket or env vars |
| 2 | Plaintext credential storage in SharedPreferences | Profile.java:30-62 | CRITICAL | EncryptedSharedPreferences |
| 3 | `strcpy` buffer overflow in tun2socks | tun2socks.c:411 | CRITICAL | snprintf with bounds check |
| 4 | JNI `system()` backdoor (arbitrary exec) | system.cpp:39-43 | CRITICAL | Remove dead code or add whitelist |
| 5 | No TLS for SOCKS5 traffic | Protocol level | HIGH | SOCKS5 over TLS or SSH tunnel |
| 6 | Exported VPN service + AIDL `stop()` | AndroidManifest.xml:33, IVpnService.aidl | HIGH | Make non-exported or add caller check |
| 7 | No R8/ProGuard on release | app/build.gradle:46 | HIGH | Enable minification |
| 8 | No NDK hardening flags | Android.mk | HIGH | Add -fstack-protector-strong, _FORTIFY_SOURCE |
| 9 | Logging of passwords in debug mode | SocksVpnService.java:263-265 | MEDIUM | Remove or guard debug logging |

---

## 3. Critical Code Quality Issues (MUST FIX)

| # | Issue | Location | Severity | Fix |
|---|-------|----------|----------|-----|
| 1 | `==` instead of `.equals()` for string comparison | ProfileManager.java:84 | CRITICAL | Use `.equals()` |
| 2 | Empty catch blocks (silent failures) | Multiple files | CRITICAL | Log exceptions or handle |
| 3 | `commit()` instead of `apply()` (blocking UI thread) | Profile.java (all setters) | HIGH | Use `apply()` |
| 4 | Polling loop every 1s instead of callbacks | ProfileFragment.java:71-77 | HIGH | LiveData/Flow/Messenger |
| 5 | Deprecated `startActivityForResult` | ProfileFragment.java:459 | HIGH | ActivityResultLauncher |
| 6 | Hardcoded `26.26.26.0/24` (not RFC 1918) | SocksVpnService.java:158 | MEDIUM | Use proper private IP range |
| 7 | Race condition in service binding | ProfileFragment.java:50-70 | MEDIUM | Proper lifecycle management |
| 8 | Dynamic Colors race on first launch | SocksApplication.java:16-17 | MEDIUM | Initialize prefs before read |
| 9 | Pervasive unsafe C (sprintf, strcpy) in BadVPN | badvpn/ (30+ locations) | MEDIUM | Replace with snprintf/strlcpy |

---

## 4. Architecture & UI Goals (SuperProxy-like)

### Phase 1: Foundation (Week 1)

- [ ] **Migrate to Kotlin** — rewrite all `java/` files to Kotlin
- [ ] **Fix security issues 1-4** (credential in argv, storage, system() backdoor, buf overflow)
- [ ] **Fix code quality issues 1-4** (== vs equals, empty catches, commit→apply, polling)
- [ ] `Activity` → `AppCompatActivity`
- [ ] `PreferenceFragment` → `PreferenceFragmentCompat` (temporary, will be replaced by Compose)
- [ ] `startActivityForResult` → `ActivityResultLauncher`
- [ ] Enable R8 for release builds

### Phase 2: Modern UI in Compose (Week 2-3)

- [ ] Add Jetpack Compose + Material3 dependencies
- [ ] Replace MainActivity with Compose-based Activity
- [ ] **Dashboard screen**: connection status (green/red indicator), server name, data usage, session duration, quick toggle button
- [ ] **Bottom Navigation**: Dashboard + Settings + About
- [ ] **Settings screen**: custom card-based settings (not PreferenceScreen)
- [ ] **Connection indicator**: animated pulsing dot or Lottie animation
- [ ] Proper dark theme with custom `values-night` overrides
- [ ] Edge-to-edge display with proper insets
- [ ] Adaptive icon (replace static PNGs)

### Phase 3: SuperProxy Polish (Week 3-4)

- [ ] **Server latency / ping display** on dashboard
- [ ] **Data usage chart** (bytes sent/received per session)
- [ ] **Splash / onboarding screen** (first-launch walkthrough)
- [ ] **Quick settings tile** for toggle
- [ ] **Export/import profiles**
- [ ] **Connection stats notification** with data counter
- [ ] Add **MaterialCardView** + `ConstraintLayout` everywhere
- [ ] Lottie connection animations

### Stretch Goals

- [ ] Replace BadVPN with a maintained tun2socks alternative (e.g. tun2proxy or Leaf)
- [ ] Add SOCKS5 over TLS support
- [ ] WireGuard-based tunneling option
- [ ] Auto region/ping-based server selection

---

## 5. Architecture Target (Post-Fork)

```
app/
├── src/main/
│   ├── java/net/typeblog/socks/          (after Kotlin migration)
│   │   ├── MainActivity.kt              # Compose Activity
│   │   ├── SocksVpnService.kt           # Keep as-is (core VPN)
│   │   ├── BootReceiver.kt              # Keep as-is
│   │   ├── System.kt                    # Keep as-is (JNI)
│   │   ├── service/
│   │   │   └── VpnConnectionManager.kt  # New: manages service lifecycle
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Theme.kt             # M3 theme + dynamic colors
│   │   │   │   ├── Color.kt
│   │   │   │   └── Type.kt
│   │   │   ├── navigation/
│   │   │   │   └── AppNavigation.kt     # NavHost + bottom nav
│   │   │   ├── dashboard/
│   │   │   │   ├── DashboardScreen.kt
│   │   │   │   └── DashboardViewModel.kt
│   │   │   ├── settings/
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   ├── profiles/
│   │   │   │   └── ProfileScreen.kt
│   │   │   └── components/
│   │   │       ├── ConnectionIndicator.kt
│   │   │       ├── StatusCard.kt
│   │   │       └── DataUsageChart.kt
│   │   └── data/
│   │       ├── Profile.kt               # Rewrite with EncryptedSharedPreferences
│   │       ├── ProfileManager.kt
│   │       └── ProfileRepository.kt     # New: clean data layer
│   ├── jni/                             # Keep entirely unchanged
│   ├── aidl/                            # Keep as-is
│   └── res/                             # Keep strings, routes; remove layouts
```

### Key Architectural Decisions

1. **Keep the native JNI layer untouched** — tun2socks, pdnsd, system.cpp are battle-tested. Only fix the credential passing mechanism.
2. **Rewrite the entire Java layer in Kotlin** — the current Java code is ~1000 LOC, manageable for a rewrite.
3. **MVVM with ViewModel + StateFlow** — clean separation of UI and business logic.
4. **Jetpack Navigation Compose** — for multi-screen navigation.
5. **EncryptedSharedPreferences** — for all credential storage.
6. **Singleshot AIDL callbacks** — replace polling with Binder DeathRecipient + callback.

---

## 6. Dependencies to Add

```kotlin
// build.gradle (app)
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Preference (temporary, remove Phase 2)
    implementation("androidx.preference:preference-ktx:1.2.1")
}
```

---

## 7. Reference: Key Files for Each Goal

| Goal | Key Files to Modify |
|------|---------------------|
| Fix credential in argv | `SocksVpnService.java:244-248`, `system.cpp` |
| Encrypted storage | `Profile.java`, `ProfileManager.java` |
| Kill `system()` backdoor | `system.cpp:39-43` |
| Fix strcpy overflow | `tun2socks.c:411` |
| Compose migration | `MainActivity.java` → `MainActivity.kt` + new compose files |
| Dashboard UI | New: `DashboardScreen.kt`, `DashboardViewModel.kt` |
| Bottom navigation | New: `AppNavigation.kt` |
| Replace PreferenceScreen | `settings.xml` → `SettingsScreen.kt` |
| Connection animation | New: `ConnectionIndicator.kt` |


