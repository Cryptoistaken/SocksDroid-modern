# Fresh Session Starter Prompt

Copy and paste this entire block into a new chat session:

---

I'm working on the **KiloProxy** project — a modern Android SOCKS5 VPN client (forked from SocksDroid) that we're redesigning and polishing.

**Repo:** https://github.com/AlexeyRF/SocksDroid-modern

## Current State

All backend changes are **complete and wired**. The Android app compiles and runs. Here's what's been done:

### ✅ Phase 1 — Foundation (Complete)
- **Security fixes:** EncryptedSharedPreferences (AES-256-GCM), killed CLI credential leak, removed JNI `system()` backdoor, fixed `strcpy` buffer overflow in tun2socks.c, added NDK hardening flags (`-fstack-protector-strong`, `_FORTIFY_SOURCE=2`), enabled R8/proguard minification
- **Architecture fixes:** `Activity` → `AppCompatActivity`, `PreferenceFragment` → `PreferenceFragmentCompat`, `startActivityForResult` → `ActivityResultLauncher`, fixed polling loop (onResume/onPause), fixed 8 empty catch blocks, fixed `26.26.26.x` → `10.10.10.x` (RFC 1918), fixed Dynamic Colors race, simplified password masking, removed dead code
- **Kotlin migration:** All 13 `.java` files → `.kt`
- **Export fix:** Added UID check in `onBind()` for VPN service
- **Remaining (3 items):** See "Remaining Fixes / Stubs" section below

### ✅ Backend Features (All Wired)
- Profile management (add/edit/delete)
- Server host + port with validation
- Auth: None / Username+Password
- Route preference: All / CHN / RU / RU+CHN
- DNS server + port with presets
- IPv6 proxy toggle
- UDP proxy toggle
- Auto-connect on boot toggle
- Per-app proxy / Split tunneling (`adv_per_app`, `adv_app_bypass`, `adv_app_list`)
- App selector with inline toggle list
- Auto-stop on screen off
- Connectivity check toggle
- Dark mode preference toggle
- IP detection with country flags
- Country database (253 entries)

### ✅ Phase 2 — Compose UI (In Progress)

The UI has been migrated to Jetpack Compose with:
- 3-tab bottom navigation (Proxies / Status / Settings)
- Theme with light/dark/system modes, Dynamic Colors support
- ProxiesScreen: card list, FAB, bottom sheets for detail + add/edit
- StatusScreen: IP display with country flags, VPN start/stop, connection info
- SettingsScreen: all preferences with Switch toggles + dialog pickers
- SplitTunnelingScreen: Allowed/Blocked tabs, per-app toggle rows
- VpnViewModel: AIDL binding, 1s state polling, profile management

### Compose UI File Map
```
ui/
├── theme/      Color.kt, Type.kt, Theme.kt
├── navigation/ AppNavigation.kt (Screen sealed class, NavHost, BottomNav)
├── screens/    ProxiesScreen.kt, StatusScreen.kt, SettingsScreen.kt, SplitTunnelingScreen.kt
├── components/ ProxyCard.kt, ConnectionCard.kt, VpnButton.kt, SettingsItem.kt, AppToggleItem.kt
└── viewmodel/  VpnViewModel.kt
```

### 🔧 Remaining Fixes / Stubs

1. **`View.VISIBLE` import bug** — `ProfileFragment.kt` and `MainActivity.kt` reference `View.VISIBLE`/`View.GONE` without `import android.view.View`. Fix by adding the import.
2. **`PREF_CONNECTIVITY_CHECK` not consumed** — Preference is saved but no backend code reads it. Either implement connectivity monitoring or remove.
3. **Service binding race** — `bindService`/`unbindService` timing in `ProfileFragment.kt` can throw `IllegalArgumentException: Service not registered`. Track bind state with a boolean flag.
4. **Build the Compose UI** — compile and fix any errors in the new Compose files. Run `./gradlew assembleDebug` to verify.

### ✅ Design Mockup Ready
A fully functional HTML/CSS prototype exists at:

**`B:\Studio\Tools\SocksDroid-modern\redesign-mockup.html`**

It includes:
- Phone-frame mockup (390×844) with light/dark theme
- 3-tab bottom navigation: **Proxies** / **Status** / **Settings**
- Proxy card list with status dots, detail overlay, edit/add modals
- Status tab: connection card, IP display (with country flags), VPN start/stop, session timer
- Settings: all toggles (dark mode, auto-connect, connectivity check, UDP, IPv6, auto-stop)
- Split tunneling: Allowed Apps / Blocked Apps tabs with per-app colored circle icons, package names, and toggle switches
- Form validation with icons (real-time host/port/auth validation)
- Connectivity check overlay with latency + exit IP simulation

## Your Mission

Review the design mockup at `B:\Studio\Tools\SocksDroid-modern\redesign-mockup.html` (open it in a browser) and implement the Android UI using the current backend code. **All backend features are already wired** — this is a pure UI implementation task. The mockup shows the exact visual target.

### Key Implementation Steps

1. **Read the master plan** at `B:\Studio\Tools\SocksDroid-modern\MASTER.md` for full context on the architecture, file layout, and what's been done
2. **Review the feature audit** at `B:\Studio\Tools\SocksDroid-modern\feature-audit.md` for the mapping between mockup features and backend preference keys
3. **Open the mockup** in a browser to see the visual target
4. **Implement Phase 2** from the master plan — the Compose UI migration:
   - Scaffolding: Compose BOM + M3 deps, theme (Material3 + Dynamic Colors), navigation (NavHost + bottom nav)
   - Dashboard screen: connection card, status, toggle, session timer, data usage
   - Settings screen: card-based settings (not PreferenceScreen), all toggles, profile management
   - Split tunneling: per-app list with colored icons and toggles
   - Remove old XML layouts after Compose UI is live
5. **Fix the remaining Phase 1 item:** service binding race in `ProfileFragment.kt`

### Code Structure
```
app/src/main/java/net/typeblog/socks/
├── MainActivity.kt              # Compose host
├── SocksVpnService.kt           # Core VPN (keep as-is)
├── BootReceiver.kt              # Keep as-is
├── System.kt                    # JNI (keep as-is)
├── ui/
│   ├── theme/Theme.kt, Color.kt, Type.kt
│   ├── navigation/AppNavigation.kt
│   ├── dashboard/DashboardScreen.kt + ViewModel
│   ├── settings/SettingsScreen.kt + ViewModel
│   └── components/
└── data/Profile.kt, ProfileManager.kt, ProfileRepository.kt
```

### Key Rules
- **Never touch `jni/`** — native code is stable
- **Keep `aidl/`** — IPC works fine, wrap in VpnConnectionManager
- **Rewrite data layer in Kotlin** — keep same API surface, add ProfileRepository
- **UI in Compose only** — no new XML layouts, no new Fragments
- **MVVM everywhere** — each screen gets a ViewModel, data through StateFlow

Start by reading `MASTER.md` for the full plan, then open the mockup in your browser, then examine the current Android source files. Let me know when you're ready to begin.
