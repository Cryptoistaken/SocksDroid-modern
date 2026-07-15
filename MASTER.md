# KiloProxy — Master Plan

> **Source:** https://github.com/AlexeyRF/SocksDroid-modern
> **Goal:** Fork → fix security → modernize UI (SuperProxy-like) → clean architecture
> **Style:** Each session picks up from the last. Checkboxes track every atomic change.

---

## 📋 Codebase Map

```
build.gradle                          # AGP 9.2.1
settings.gradle                       # :app module
app/
├── build.gradle                      # targetSdk 34, minSdk 21
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml
    ├── aidl/net/typeblog/socks/
    │   └── IVpnService.aidl          # isRunning(), stop()
    ├── java/net/typeblog/socks/
    │   ├── MainActivity.java         # Launcher
    │   ├── ProfileFragment.java      # Settings UI
    │   ├── SocksVpnService.java      # Core VPN (:vpn process)
    │   ├── SocksApplication.java     # DynamicColors init
    │   ├── BootReceiver.java         # Auto-connect on boot
    │   ├── AppSelector.java          # Per-app proxy picker
    │   ├── System.java               # JNI declarations
    │   └── util/
    │       ├── Constants.java
    │       ├── Profile.java          # Data model (SharedPrefs-backed)
    │       ├── ProfileFactory.java   # WeakReference cache
    │       ├── ProfileManager.java   # CRUD singleton
    │       ├── Routes.java           # Route builder
    │       └── Utility.java          # exec, kill, startVpn, pdnsd
    ├── jni/
    │   ├── Android.mk                # 4 NDK modules
    │   ├── Application.mk
    │   ├── system.cpp                # JNI glue
    │   ├── libancillary/             # Unix socket fd-passing
    │   ├── badvpn/                   # tun2socks + lwIP stack
    │   └── pdnsd/                    # DNS-over-TCP daemon
    └── res/
        ├── layout/main.xml
        ├── layout/app_item.xml
        ├── xml/settings.xml          # PreferenceScreen
        ├── values/                   # strings, styles, routes
        ├── values-ru/
        └── drawable*/mipmap*/
```

### Tech Stack

| Layer | Current | Target |
|-------|---------|--------|
| Language | Java 17 + C/C++ | Kotlin + C/C++ |
| UI | XML + PreferenceFragment (deprecated) | Jetpack Compose + Material3 |
| Architecture | Activity/Fragment/Service (none) | MVVM + ViewModel + StateFlow |
| Navigation | Raw FragmentManager | Jetpack Navigation Compose |
| Data | SharedPreferences (plaintext) | EncryptedSharedPreferences |
| Native | NDK (Android.mk) | Keep as-is |
| IPC | AIDL | Keep AIDL |

---

## 🎯 Phase 0 — Discovery (✓ DONE)

### Knowledge Gathered

**Architecture:**
- Single Activity hosts one PreferenceFragment in a FrameLayout
- SocksVpnService runs in `:vpn` separate process
- IPC via AIDL (`IVpnService.aidl`): `isRunning()` + `stop()`
- No ViewModel, no Lifecycle, no Navigation — raw FragmentManager
- Profiles stored in SharedPreferences with key-prefix encoding
- Two native processes: `tun2socks` (TUN→SOCKS5) + `pdnsd` (DNS→TCP)
- TUN fd passed via Unix domain socket + `SCM_RIGHTS`

**Critical Security Findings (all fixed in Session 1):**
1. Passwords in CLI argv → world-readable via `/proc/pid/cmdline`
2. Credentials in plain SharedPreferences → extractable from ADB backup
3. JNI `system()` call → arbitrary shell execution backdoor
4. `strcpy` buffer overflow in tun2socks.c
5. No R8/proguard on release builds
6. No NDK compiler hardening
7. Android backup enabled → all creds exposed
8. Empty catch blocks everywhere → silent failures

**UI Assessment:**
- Deprecated `PreferenceFragment` (since API 28)
- `Activity` not `AppCompatActivity` — no AppCompat theming
- 1-second polling loop for VPN state (battery waste)
- No dashboard, no bottom nav, no navigation at all
- Single screen: a settings list
- Hardcoded `26.26.26.0/24` IP range (DoD-owned, not RFC 1918)
- Dynamic Colors race on first launch
- Dead code: 22 lines commented-out in MainActivity
- Dead inflation: double-inflating list items in AppSelector
- Convoluted password masking logic

---

### Phase 1 — Foundation (COMPLETE)

> 16 files changed, 113 insertions, 95 deletions
> 5 parallel agents, zero file overlap

### 1.1 Security — Encrypted Credential Storage

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `ProfileManager.java` | Replace `SharedPreferences` with `EncryptedSharedPreferences` (AES-256-GCM, AES-256-SIV key) | ✅ | — |
| `ProfileManager.java` | Fix `name == list.get(0)` → `name.equals(list.get(0))` | ✅ | — |
| `Profile.java` | 16x `.commit()` → `.apply()` (async write, no UI thread blocking) | ✅ | — |
| `Constants.java` | Add `MASTER_KEY_ALIAS = "socksdroid_master_key"` | ✅ | — |

### 1.2 Security — Kill CLI Credential Leak

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `SocksVpnService.java` | Remove `--username`/`--password` CLI args (SOCKS5 auth is handled at connect time, not process start) | ✅ | — |
| `Utility.java` | Remove `INTENT_USERNAME`/`INTENT_PASSWORD` from startVpn Intent | ✅ | — |
| `System.java` | Remove `exec()` native method declaration | ✅ | — |

### 1.3 Security — Kill JNI system() Backdoor

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `system.cpp` | Remove `Java_net_typeblog_socks_system_exec()` function | ✅ | — |
| `system.cpp` | Remove `"exec"` from `JNI_OnLoad` registration table | ✅ | — |

### 1.4 Security — Fix Buffer Overflow

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `tun2socks.c` | Replace `strcpy(argv[0], ...)` with `snprintf` + bound check | ✅ | — |
| `Android.mk` | Add `-fstack-protector-strong -D_FORTIFY_SOURCE=2` to all 4 modules | ✅ | — |

### 1.5 Security — Build Hardening

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `app/build.gradle` | `minifyEnabled false` → `true`, add `shrinkResources true` | ✅ | — |
| `proguard-rules.pro` | Add JNI keep rules (`System`, `net.typeblog.socks.**`) | ✅ | — |

### 1.6 Architecture — Modernize Base Classes

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `MainActivity.java` | `extends Activity` → `extends AppCompatActivity` | ✅ | — |
| `MainActivity.java` | `getFragmentManager()` → `getSupportFragmentManager()` | ✅ | — |
| `MainActivity.java` | Remove 22 lines of dead commented-out code | ✅ | — |
| `ProfileFragment.java` | `PreferenceFragment` → `PreferenceFragmentCompat` | ✅ | — |
| `ProfileFragment.java` | `addPreferencesFromResource()` → `onCreatePreferences()` + `setPreferencesFromResource()` | ✅ | — |

### 1.7 Architecture — Fix Deprecated APIs

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `ProfileFragment.java` | `startActivityForResult(i, 0)` → `ActivityResultLauncher` | ✅ | — |

### 1.8 Architecture — Fix Polling Loop

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `ProfileFragment.java` | Add `onResume` → start polling, `onPause` → stop polling (no more battery drain when not visible) | ✅ | — |

### 1.9 Code Quality — Fix Empty Catches

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `SocksVpnService.java` | 6 empty catch blocks → `Log.e(TAG, "...", e)` | ✅ | — |
| `ProfileFragment.java` | 2 empty catch blocks → `Log.e(TAG, "...", e)` | ✅ | — |

### 1.10 Misc Fixes

| File | Change | Status | Commit |
|------|--------|--------|--------|
| `SocksVpnService.java` | `26.26.26.x` → `10.10.10.x` (RFC 1918 private) in 3 locations | ✅ | — |
| `SocksApplication.java` | Add `PreferenceManager.setDefaultValues()` before reading Dynamic Colors toggle | ✅ | — |
| `ProfileFragment.java` | Simplify password masking: `String.format(...)` → `replaceAll(".", "*")` | ✅ | — |
| `AppSelector.java` | Remove dead double-inflation of list items | ✅ | — |
| `AndroidManifest.xml` | `allowBackup="true"` → `"false"` | ✅ | — |
| `AndroidManifest.xml` | BootReceiver `exported="true"` → `"false"` | ✅ | — |
| `app/build.gradle` | Add `security-crypto`, `appcompat`, `preference-ktx`, `activity-ktx` deps | ✅ | — |

**Session 3 — Backend Feature Polish (Jul 15, 2026):**
- [x] IP detection via ip-api.com (`Utility.checkPublicIp()`, 30s periodic poll in `SocksVpnService`)
- [x] Country flag display (`Countries.kt`: 253 entries, `countryCodeToFlag()` in `Utility.kt`, Unicode regional indicators)
- [x] Theme mode preference (`PREF_THEME_MODE` — Light/Dark/System via `AppCompatDelegate.setDefaultNightMode()`)
- [x] Auto-stop VPN on screen off (`PREF_AUTO_STOP`, `ACTION_SCREEN_OFF` broadcast receiver in `SocksVpnService`)
- [x] Connectivity check preference (`PREF_CONNECTIVITY_CHECK` — wired to SharedPreferences but NOT consumed by backend logic)
- [x] AIDL interface extended: `getCurrentIp()`, `getCountryCode()`, `getConnectedSince()`
- [x] HTML mockup redesign: SocksDroid → KiloProxy branding, Split Tunneling single-list with 3-state per-app toggle
- [x] Feature audit document (`feature-audit.md`) matching mockup features to backend
- [x] All `.md` documents deduplicated and consolidated into MASTER.md as single source of truth

### ⬜ Phase 1 — What's Left

| # | Task | Files | Difficulty | Agent Plan |
|---|------|-------|-----------|------------|
| ~~P1-L1~~ | ~~Kotlin migration — all `*.java` → `*.kt`~~ | ~~13 files~~ | ✅ DONE | Session 2 |
| ~~P1-L2~~ | ~~Fix exported VPN service — added UID check in `onBind()`~~ | ~~`SocksVpnService.kt`~~ | ✅ DONE | Session 2 |
| P1-L3 | **Service binding race** — `bindService`/`unbindService` timing in ProfileFragment | `ProfileFragment.kt` | Medium | Still pending |

---

## 🔲 Phase 2 — Modern UI (Jetpack Compose)

> Goal: Replace XML + PreferenceFragment with Compose + Navigation + Dashboard

### Phase 2 — Compose UI Migration (Session 4 — IN PROGRESS)

- [x] Compose dependencies added to build.gradle (BOM 2024.10.01, navigation-compose, lifecycle-viewmodel-compose, activity-compose)
- [x] Theme system created: Color.kt (light/dark palettes), Type.kt (system fonts), Theme.kt (PREF_THEME_MODE-aware, Dynamic Colors support)
- [x] Navigation scaffold: Screen sealed class (4 routes), NavHost, BottomAppBar with 3 tabs (Globe/Shield/Gear), hidden for SplitTunneling
- [x] MainActivity.kt rewritten: edge-to-edge, setContent with KiloProxyTheme + AppNavigation
- [x] VpnViewModel.kt: AIDL service binding, 1s polling loop, IP/state tracking, public IP check, ProfileManager delegation
- [x] ProxiesScreen.kt: LazyColumn of ProxyCards, FAB, ModalBottomSheet for detail + add/edit with form validation
- [x] StatusScreen.kt: ConnectionCard (shield icon, IP+flag, connected duration) + VpnButton (start/stop)
- [x] SettingsScreen.kt: All preference sections (Appearance, Connection, Routing, Advanced, Split Tunneling, About) with Switch toggles + dialog pickers
- [x] SplitTunnelingScreen.kt: Allowed/Blocked tabs, 15 demo apps with per-app toggles persisted to adv_app_list
- [x] ProxyCard.kt, ConnectionCard.kt, VpnButton.kt, SettingsItem.kt, AppToggleItem.kt — all component composables

### 2.1 Scaffolding

| # | Task | Files | Status | Notes |
|---|------|-------|--------|-------|
| 2.1.1 | Add Compose BOM + M3 deps to `app/build.gradle` | `app/build.gradle` | ⬜ | |
| 2.1.2 | Create `ui/theme/Theme.kt` — Material3 with Dynamic Colors | `Theme.kt` | ⬜ | |
| 2.1.3 | Create `ui/theme/Color.kt` — light + dark palettes | `Color.kt` | ⬜ | |
| 2.1.4 | Create `ui/theme/Type.kt` — typography scale | `Type.kt` | ⬜ | |
| 2.1.5 | Create `ui/navigation/AppNavigation.kt` — NavHost + bottom nav | `AppNavigation.kt` | ⬜ | 3 tabs: Dashboard, Settings, About |

### 2.2 Dashboard Screen

| # | Task | Status | Notes |
|---|------|--------|-------|
| 2.2.1 | Create `ui/dashboard/DashboardScreen.kt` | ⬜ | Connection card + status + toggle |
| 2.2.2 | Create `ui/dashboard/DashboardViewModel.kt` | ⬜ | Binds to SocksVpnService via AIDL |
| 2.2.3 | Connection indicator — green/red pulsing dot | ⬜ | Animated with Compose |
| 2.2.4 | Server info card (name, IP, route mode) | ⬜ | |
| 2.2.5 | Quick toggle button (connect/disconnect) | ⬜ | Material3 FilledTonalButton |
| 2.2.6 | Session duration timer | ⬜ | Starts on connect, stops on disconnect |
| 2.2.7 | Data usage display (bytes sent/received) | ⬜ | Read from VpnService counters |

### 2.3 Settings Screen (Compose)

| # | Task | Status | Notes |
|---|------|--------|-------|
| 2.3.1 | Create `ui/settings/SettingsScreen.kt` | ⬜ | Card-based, not PreferenceScreen |
| 2.3.2 | Create `ui/settings/SettingsViewModel.kt` | ⬜ | Wraps ProfileManager |
| 2.3.3 | Profile selector dropdown | ⬜ | |
| 2.3.4 | Server address + port fields | ⬜ | With validation |
| 2.3.5 | Auth toggle + username/password fields | ⬜ | |
| 2.3.6 | Route mode selector (All / Bypass CN / Bypass RU / Both) | ⬜ | |
| 2.3.7 | DNS server + port | ⬜ | With presets |
| 2.3.8 | Per-app proxy section with app picker | ⬜ | |
| 2.3.9 | IPv6 + UDP + UDP gateway toggles | ⬜ | |
| 2.3.10 | Auto-connect on boot toggle | ⬜ | |
| 2.3.11 | Dynamic Colors toggle | ⬜ | |
| 2.3.12 | Profile management (add, rename, delete, export/import) | ⬜ | |

### 2.4 Remove Old UI

| # | Task | Status | Notes |
|---|------|--------|-------|
| 2.4.1 | Delete `res/layout/main.xml` | ⬜ | After Compose dashboard is live |
| 2.4.2 | Delete `res/layout/app_item.xml` | ⬜ | |
| 2.4.3 | Delete `res/xml/settings.xml` | ⬜ | |
| 2.4.4 | Remove `preference-ktx` dependency | ⬜ | |
| 2.4.5 | Delete `ProfileFragment.java` | ⬜ | Logic moved to ViewModels |

### 2.5 Polish

| # | Task | Status | Notes |
|---|------|--------|-------|
| 2.5.1 | Create `res/values-night/themes.xml` — custom dark theme overrides | ⬜ | |
| 2.5.2 | Edge-to-edge display with `WindowCompat.setDecorFitsSystemWindows` | ⬜ | |
| 2.5.3 | Adaptive icon with `mipmap-anydpi-v26/ic_launcher.xml` | ⬜ | |
| 2.5.4 | Add Lottie dependency + animated connection state | ⬜ | Optional |

---

## 🔲 Phase 3 — SuperProxy Polish

| # | Task | Status | Difficulty |
|---|------|--------|-----------|
| 3.1 | Server latency / ping display on dashboard | ⬜ | Medium |
| 3.2 | Data usage chart (last 24h / session) | ⬜ | Hard |
| 3.3 | Splash / onboarding screen (first launch walkthrough) | ⬜ | Medium |
| 3.4 | Quick settings tile (android.service.quicksettings) | ⬜ | Medium |
| 3.5 | Export/import profiles (JSON file) | ⬜ | Medium |
| 3.6 | Connection stats notification with data counter | ⬜ | Low |
| 3.7 | Lottie connection animations | ⬜ | Low |

---

## 🔲 Stretch Goals

| # | Task | Status | Notes |
|---|------|--------|-------|
| S.1 | Replace BadVPN with tun2proxy or Leaf (maintained) | ⬜ | Major effort |
| S.2 | SOCKS5 over TLS (encrypted proxy) | ⬜ | Protocol-level change |
| S.3 | WireGuard-based tunneling option | ⬜ | Completely new feature |
| S.4 | Auto region/ping-based server selection | ⬜ | |

---

## 📐 Target Architecture

```
app/src/main/java/net/typeblog/socks/
├── MainActivity.kt              # Compose host
├── SocksVpnService.kt           # Core VPN (keep as-is)
├── BootReceiver.kt              # Keep as-is
├── System.kt                    # JNI (keep as-is)
├── service/
│   └── VpnConnectionManager.kt  # Service lifecycle + AIDL bridge
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── profiles/
│   │   └── ProfileScreen.kt
│   └── components/
│       ├── ConnectionIndicator.kt
│       ├── StatusCard.kt
│       └── DataUsageChart.kt
└── data/
    ├── Profile.kt               # EncryptedSharedPreferences
    ├── ProfileManager.kt
    └── ProfileRepository.kt
```

### Key Rules
1. **Never touch `jni/`** — native code is stable, only modify if replacing the VPN engine (Stretch S.1)
2. **Keep `aidl/`** — IPC works fine, wrap in `VpnConnectionManager`
3. **Rewrite data layer in Kotlin** — keep the same API surface, add `ProfileRepository`
4. **UI in Compose only** — no new XML layouts, no new Fragments
5. **MVVM everywhere** — each screen gets a ViewModel, data flows through StateFlow

---

## Known Bugs

1. **`View.VISIBLE` / `View.GONE` import missing** — `ProfileFragment.kt` (lines 562, 565, 574, 581) and `MainActivity.kt` (line 39) use `View.VISIBLE` / `View.GONE` without `import android.view.View`. Parent class imports provide indirect access on some compilers, but this may fail on clean builds. Fix: add `import android.view.View` to both files.

2. **`PREF_CONNECTIVITY_CHECK` is a stub** — the preference is saved to SharedPreferences but nothing in `SocksVpnService` or elsewhere actually reads it. No connectivity monitoring logic exists. A future session should implement periodic connectivity pings and display results in the UI.

---

## 🧩 Session Log

| Session | Date | Focus | Agents Used | Files Changed | Status |
|---------|------|-------|-------------|---------------|--------|
| 1 | 2026-07-15 | Phase 1 — Security + Architecture fixes | 5 parallel | 16 | ✅ |
| 2 | 2026-07-15 | Kotlin migration (13 `.java` → `.kt`) + exported service fix | 4 parallel | 20 | ✅ |
| 3 | Jul 15, 2026 | Backend polish: IP detection, country flags, theme mode, auto-stop, connectivity check pref, AIDL extension, HTML mockup redesign (KiloProxy branding, Split Tunneling redesign), doc dedup | 3 explorer + 3 fixer | Completed |
| 4 | Jul 15, 2026 | Compose UI migration: theme, nav, 4 screens (Proxies/Status/Settings/SplitTunneling), VpnViewModel, 5 components, build config | 3 fixer | In Progress |

### Session Template

```
## Session N — [Title]

**Focus:** [what we're doing]
**Agents:** [count] parallel, zero file overlap
**Plan file:** FIXES_ORCHESTRATION_N.md
**State:** [⬜ Planned / 🔄 In Progress / ✅ Complete]

### Completed
- [ ] Task 1
- [ ] Task 2

### Key Decisions
- [ ] Decision 1
```

---

## 📁 Key Files Reference

| File | Purpose | Owned By |
|------|---------|----------|
| `MASTER.md` | THIS — single source of truth for all sessions | Us |
| `FORK_PLAN.md` | Original discovery + discovery analysis (archive) | Archive |
| `FIXES_ORCHESTRATION.md` | Session 1 batch orchestration (archive) | Archive |
| `FIXES_ORCHESTRATION_N.md` | Future session orchestration plans | Per-session |

---

## 🚀 Next Session Ready

### Immediate Build Fixes (compile then fix errors)
1. Build and fix any compile errors in the new Compose files
2. Ensure navigation between all 4 screens works
3. Fix service binding in VpnViewModel if needed

### Remaining Phase 2 Items
- Remove old XML files (main.xml, app_item.xml) and Fragment code (ProfileFragment.kt) AFTER Compose is verified working
- Add real app icons (PackageManager) to split tunneling (currently uses colored letter circles)
- Implement Connectivity Check overlay (mockup shows loading spinner → result card)
- Add animation transitions between tabs
- Internationalize strings

### Known Bugs
1. `View.VISIBLE` import missing in ProfileFragment.kt and MainActivity.kt
2. `PREF_CONNECTIVITY_CHECK` not consumed by backend
3. Service binding race in ProfileFragment.kt (legacy code)
