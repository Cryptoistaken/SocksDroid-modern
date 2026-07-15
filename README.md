# KiloProxy

KiloProxy — a modern Android SOCKS5 VPN client (forked from [SocksDroid](https://github.com/AlexeyRF/SocksDroid)) with per-app proxy, DNS customization, and multi-profile management.

**Status:** Backend features complete. UI redesign in progress (Compose migration Phase 2).

## Features

- **SOCKS5 Proxy** — Route device traffic through any SOCKS5 server
- **Profile Management** — Multiple profiles with encrypted storage (AES-256-GCM)
- **Authentication** — None or Username+Password per profile
- **Route Modes** — All traffic / CHN bypass / RU bypass / RU+CHN bypass
- **DNS Customization** — Presets (Google, Cloudflare, AdGuard) or custom DNS:port
- **Per-App Proxy (Split Tunneling)** — Route or bypass individual apps through VPN
- **IPv6 Proxy** — Forward IPv6 traffic through the proxy
- **UDP Forwarding** — UDP over SOCKS5 via badvpn-udpgw
- **IP Detection** — Shows real & proxy IP with country flags (Unicode emoji)
- **Theme** — Light / Dark / System theme modes
- **Auto-Connect** — Start VPN automatically on device boot
- **Auto-Stop** — Stop VPN when screen turns off
- **Connectivity Check** — Test connection on save (UI pending)
- **Dynamic Colors** — Material You theming (Android 12+)
- **Foreground Service** — Persistent notification with VPN status

## Architecture

```
app/src/main/java/net/typeblog/socks/
├── MainActivity.kt          — Entry point, applies theme, hosts fragment
├── ProfileFragment.kt       — Main settings UI (PreferenceFragmentCompat)
├── SocksVpnService.kt       — VPN service with IP tracking, auto-stop, AIDL binder
├── SocksApplication.kt      — App init, Dynamic Colors, default prefs
├── BootReceiver.kt          — Auto-connect on boot
├── AppSelector.kt           — Per-app package picker dialog
├── System.kt                — JNI bridge (native libs)
└── util/
    ├── Constants.kt         — All preference keys
    ├── Countries.kt         — 253-country database with flags
    ├── Profile.kt           — Encrypted per-profile storage
    ├── ProfileFactory.kt    — Profile cache (WeakReference)
    ├── ProfileManager.kt    — Profile CRUD (EncryptedSharedPreferences)
    ├── Routes.kt            — VPN route definitions
    └── Utility.kt           — IP check, flag conversion, shell exec
```

### Native Code (`jni/`)
- `pdnsd/` — DNS proxy daemon (TCP to UDP)
- `badvpn/` — tun2socks SOCKS5-to-TUN tunnel + lwip TCP/IP stack
- `system.cpp` — JNI helpers (file descriptor passing, ABI detection)
- `libancillary/` — Unix fd passing

## Building

```bash
# Clone with submodules (for jni/ dependencies)
git clone --recurse-submodules https://github.com/AlexeyRF/SocksDroid-modern
cd SocksDroid-modern

# Build native libraries
ndk-build -C app/src/main/jni

# Build APK
./gradlew assembleDebug
```

Requires: Android SDK 34+, NDK 27+, JDK 17.

## Design Mockup

A working HTML/CSS prototype (`redesign-mockup.html`) shows the target UI — KiloProxy branding with 3-tab bottom navigation (Proxies / Status / Settings), IP display with country flags, split tunneling with per-app toggles, and light/dark theme.

## Roadmap

- **Phase 1 (Complete):** Security fixes (EncryptedSharedPreferences, NDK hardening, R8), architecture fixes, Java→Kotlin migration, backend features (IP, theme, auto-stop, connectivity)
- **Phase 2 (Next):** Compose UI migration — scaffold, theme, navigation, Dashboard, Settings, Split Tunneling, remove XML/Fragments
- **Phase 3 (Stretch):** SuperProxy-inspired polish — animations, bottom sheets, connectivity overlay, per-profile status dots

## License

GPL v3 (inherited from upstream SocksDroid). Native components (pdnsd, badvpn) under their respective licenses.
