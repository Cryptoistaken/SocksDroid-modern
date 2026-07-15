# Feature Audit: HTML Mockup vs. Android Backend

**Generated:** 2026-07-15  
**Mockup:** `redesign-mockup.html`  
**Backend:** `app/src/main/java/net/typeblog/socks/`

---

## Summary

| Category | Mockup Features | Backend Match | Missing from Backend |
|----------|----------------|---------------|---------------------|
| Profile Management | 3 | 3 | 0 |
| Connection Settings | 5 | 5 | 0 |
| Authentication | 3 | 3 | 0 |
| Routing & DNS | 2 | 2 | 0 |
| Advanced | 1 | 1 | 0 |
| Split Tunneling | 2 | 2 | 0 |
| Appearance | 2 | 1 | 1 |
| Status/Info Display | 4 | 0 | 4 |
| UI/UX Elements | 8 | 0 | 8 |
| **TOTAL** | **30** | **17** | **13** |

---

## Detailed Feature Audit Table

| # | Feature in HTML Mockup | Status | Backend Change Needed? | Notes |
|---|------------------------|--------|----------------------|-------|
| **Profile Management** | | | | |
| 1 | Profile selector (ListPreference) | ✅ Have | No | Pref key: `profile` (PREF_PROFILE). Wired in ProfileFragment.kt:103,292 |
| 2 | Add new profile (FAB → modal) | ✅ Have | UI only | Pref key: `prof_add_btn`. ProfileFragment.kt:414-438. Current UI uses AlertDialog; mockup uses bottom sheet modal |
| 3 | Delete profile | ✅ Have | UI only | Pref key: `prof_del_btn`. ProfileFragment.kt:440-459. Current UI uses AlertDialog confirmation |
| **Connection Settings** | | | | |
| 4 | Server host (EditTextPreference) | ✅ Have | No | Pref key: `server_ip` (PREF_SERVER_IP). ProfileFragment.kt:185-188, Profile.kt:17-23 |
| 5 | Server port (EditTextPreference) | ✅ Have | No | Pref key: `server_port` (PREF_SERVER_PORT). ProfileFragment.kt:190-194, Profile.kt:25-31 |
| 6 | Auth type toggle (None / Username+Password) | ✅ Have | UI only | Pref key: `auth_userpw` (PREF_AUTH_USERPW). ProfileFragment.kt:196-198. Mockup uses dropdown select; backend uses CheckBoxPreference |
| 7 | Username field (EditTextPreference) | ✅ Have | No | Pref key: `auth_username` (PREF_AUTH_USERNAME). ProfileFragment.kt:200-202, Profile.kt:41-46 |
| 8 | Password field (EditTextPreference, password masked) | ✅ Have | No | Pref key: `auth_password` (PREF_AUTH_PASSWORD). ProfileFragment.kt:204-206, Profile.kt:49-55 |
| **Routing & DNS** | | | | |
| 9 | Route preference (All traffic, CHN, RU, RU+CHN) | ✅ Have | No | Pref key: `adv_route` (PREF_ADV_ROUTE). ListPreference with 4 values. ProfileFragment.kt:210-212, Routes.kt |
| 10 | DNS server (EditTextPreference) | ✅ Have | No | Pref key: `adv_dns` (PREF_ADV_DNS). ProfileFragment.kt:224-228, Profile.kt:65-71. Mockup shows "8.8.8.8:53" as default |
| **Connection Settings (cont.)** | | | | |
| 11 | IPv6 proxy toggle | ✅ Have | No | Pref key: `ipv6_proxy` (PREF_IPV6_PROXY). ProfileFragment.kt:247-249, Profile.kt:105-111 |
| 12 | UDP proxy toggle | ✅ Have | No | Pref key: `udp_proxy` (PREF_UDP_PROXY). ProfileFragment.kt:251-253, Profile.kt:113-119 |
| 13 | Auto-connect on boot toggle | ✅ Have | No | Pref key: `adv_auto_connect` (PREF_ADV_AUTO_CONNECT). ProfileFragment.kt:260-262, Profile.kt:129-135, BootReceiver.kt |
| **Advanced** | | | | |
| 14 | Per-app proxy / Split tunneling toggle | ✅ Have | UI only | Pref key: `adv_per_app` (PREF_ADV_PER_APP). ProfileFragment.kt:235-237. Mockup uses Allowed/Disallowed tabs; backend uses single checkbox + app selector |
| 15 | App bypass mode toggle | ✅ Have | No | Pref key: `adv_app_bypass` (PREF_ADV_APP_BYPASS). ProfileFragment.kt:239-241, Profile.kt:89-95 |
| 16 | App selector (per-app list) | ✅ Have | UI only | Pref key: `adv_app_selector`. ProfileFragment.kt:168-171, AppSelector.kt. Mockup shows inline toggle list; backend uses AlertDialog with ListView |
| 17 | App list (EditTextPreference) | ✅ Have | No | Pref key: `adv_app_list` (PREF_ADV_APP_LIST). ProfileFragment.kt:243-245, Profile.kt:97-103 |
| **Appearance** | | | | |
| 18 | Dark mode toggle | ⚠️ Partial | Yes | Mockup shows dedicated dark/light theme toggle. Backend only has `dynamic_colors` (PREF_DYNAMIC_COLORS) for Material You dynamic colors. No manual dark mode toggle exists. Need to add dark mode preference and theme-switching logic |
| 19 | Language selector | ❌ Missing | Yes | Not present in backend. No language/locale preference in Constants.kt, Profile.kt, or settings.xml. Would need: new pref key, locale helper, string resources in multiple languages |
| **Status/Info Display** | | | | |
| 20 | VPN status card (connected/disconnected indicator) | ⚠️ Partial | UI only | Backend tracks `mRunning` state via IVpnService Binder (ProfileFragment.kt:484-512). No dedicated "status card" UI - just a MaterialSwitch. New UI component needed, but backend data exists |
| 21 | Connection info grid (Server, IP Address, Connected since) | ❌ Missing | Yes | Backend does NOT track: (a) external IP address, (b) connection start time. Would need: new fields in Profile or VPN service, IP lookup logic, timestamp recording on VPN start |
| 22 | Start/Stop VPN button (Status tab) | ⚠️ Partial | UI only | Backend has VPN start/stop via MaterialSwitch (ProfileFragment.kt:272-278, 514-551). Mockup puts this in a separate Status tab; backend has it on the main screen |
| 23 | Connection status badge (Connected/Disconnected) | ⚠️ Partial | UI only | Backend tracks `mRunning` but no badge UI. New UI component needed, backend data source exists |
| **Split Tunneling (UI-specific)** | | | | |
| 24 | Allowed/Disallowed tab view | ⚠️ Partial | UI only | Backend has `adv_per_app` (boolean) + `adv_app_bypass` (boolean) + `adv_app_list` (string). Mockup presents as two tabs. Backend combines into single list + bypass toggle. UI restructuring needed |
| 25 | Per-app toggle switches (inline) | ✅ Have | UI only | Backend uses AppSelector.kt with CheckBox list in AlertDialog. Mockup shows inline toggles. Same data model, different UI |
| **UI/UX Elements** | | | | |
| 26 | Proxy card list (name, address, protocol badge, status dot) | ⚠️ Partial | UI only | Backend stores profile name, server, port. No explicit "protocol" field (always SOCKS5). No per-profile connection status stored in UI layer |
| 27 | Proxy detail modal (overlay) | ❌ Missing | UI only | No detail view in backend. Backend has single-screen layout with all fields visible. New UI component needed |
| 28 | Add/Edit proxy modal (form with validation) | ⚠️ Partial | UI only | Backend has add profile (AlertDialog) + edit (inline preferences). Mockup combines into single form modal with validation icons. New UI needed |
| 29 | Form validation (host required, port 1-65535, auth fields conditional) | ❌ Missing | UI only | Backend has minimal validation (port non-empty check in ProfileFragment.kt:191). Mockup shows real-time validation with icons/spinners. Pure UI feature |
| 30 | Bottom tab bar (Proxies, Status, Settings) | ❌ Missing | UI only | Backend uses single Activity with PreferenceFragment. Mockup uses 3-tab layout. Complete UI restructuring needed |
| 31 | Dark/Light theme switching | ⚠️ Partial | Yes | Mockup has full dark/light theme with CSS variables. Backend only supports `dynamic_colors` for Material You. Manual theme switching not implemented |
| 32 | VPN start/stop button in proxy detail modal | ✅ Have | UI only | Backend has VPN toggle via MaterialSwitch. Mockup puts VPN control inside proxy detail modal. Same backend logic, different UI placement |

---

## Preference Key Mapping

| Mockup Feature | Preference Key (Constants.kt) | Profile.kt Method | ProfileFragment Wired? |
|---|---|---|---|
| Profile selector | `profile` (PREF_PROFILE) | N/A (managed by ProfileManager) | Yes (line 292, 334) |
| Add profile button | `prof_add_btn` | N/A | Yes (line 327, 352) |
| Delete profile button | `prof_del_btn` | N/A | Yes (line 329, 353) |
| Server host | `server_ip` (PREF_SERVER_IP) | `getServer()` / `setServer()` | Yes (line 294, 335) |
| Server port | `server_port` (PREF_SERVER_PORT) | `getPort()` / `setPort()` | Yes (line 296, 336) |
| Auth toggle | `auth_userpw` (PREF_AUTH_USERPW) | `isUserPw()` / `setIsUserpw()` | Yes (line 298, 337) |
| Username | `auth_username` (PREF_AUTH_USERNAME) | `getUsername()` / `setUsername()` | Yes (line 300, 338) |
| Password | `auth_password` (PREF_AUTH_PASSWORD) | `getPassword()` / `setPassword()` | Yes (line 302, 339) |
| Route preference | `adv_route` (PREF_ADV_ROUTE) | `getRoute()` / `setRoute()` | Yes (line 304, 340) |
| DNS presets | `adv_dns_presets` | `setDns()` (via preset) | Yes (line 306, 341) |
| DNS server | `adv_dns` (PREF_ADV_DNS) | `getDns()` / `setDns()` | Yes (line 308, 342) |
| DNS port | `adv_dns_port` (PREF_ADV_DNS_PORT) | `getDnsPort()` / `setDnsPort()` | Yes (line 310, 343) |
| IPv6 proxy | `ipv6_proxy` (PREF_IPV6_PROXY) | `hasIPv6()` / `setHasIPv6()` | Yes (line 318, 344) |
| UDP proxy | `udp_proxy` (PREF_UDP_PROXY) | `hasUDP()` / `setHasUDP()` | Yes (line 320, 345) |
| UDP gateway | `udp_gw` (PREF_UDP_GW) | `getUDPGW()` / `setUDPGW()` | Yes (line 322, 349) |
| Auto-connect | `adv_auto_connect` (PREF_ADV_AUTO_CONNECT) | `autoConnect()` / `setAutoConnect()` | Yes (line 324, 350) |
| Per-app proxy | `adv_per_app` (PREF_ADV_PER_APP) | `isPerApp()` / `setIsPerApp()` | Yes (line 312, 346) |
| App bypass | `adv_app_bypass` (PREF_ADV_APP_BYPASS) | `isBypassApp()` / `setIsBypassApp()` | Yes (line 314, 347) |
| App selector | `adv_app_selector` | N/A (opens AppSelector) | Yes (line 331, 354) |
| App list | `adv_app_list` (PREF_ADV_APP_LIST) | `getAppList()` / `setAppList()` | Yes (line 316, 348) |
| Dynamic colors | `dynamic_colors` (PREF_DYNAMIC_COLORS) | N/A (SharedPreferences) | Yes (line 326, 351) |

---

## Features Requiring Backend Changes

### 1. Dark Mode Toggle (Priority: Medium)
**What exists:** `dynamic_colors` preference for Material You  
**What's needed:** Manual dark/light theme switching  
**Backend work:**
- Add new preference key: `PREF_DARK_MODE` to Constants.kt
- Add getter/setter in Profile.kt or use shared app-level pref
- Add theme-switching logic in MainActivity.kt (setTheme() before setContentView)
- Add CheckBoxPreference to settings.xml

### 2. Language Selector (Priority: Low)
**What exists:** Nothing  
**What's needed:** In-app language switching  
**Backend work:**
- Add new preference key: `PREF_LANGUAGE` to Constants.kt
- Create locale helper utility
- Add string resources for supported languages
- Add ListPreference to settings.xml
- Apply locale in MainActivity.kt using `Locale.setDefault()` + `resources.updateConfiguration()`

### 3. Connection Info Display (Priority: Medium)
**What exists:** VPN running state via Binder  
**What's needed:** External IP, connection timestamp  
**Backend work:**
- Add timestamp field to SocksVpnService: record `System.currentTimeMillis()` on VPN start
- Add IP lookup: either (a) HTTP call to ipinfo.io/ip on VPN connect, or (b) read from tun2socks output
- Expose these via IVpnService Binder interface
- Update ProfileFragment to read/display these values

### 4. Per-Profile Connection Status (Priority: Low)
**What exists:** Single global VPN running state  
**What's needed:** Which profile is currently connected  
**Backend work:**
- Store active profile name when VPN starts (in SharedPreferences or memory)
- Expose via Binder
- Update UI to show per-card status dots

---

## Features Requiring UI-Only Changes (No Backend Work)

| Feature | Current Backend | UI Work Needed |
|---|---|---|
| Bottom tab bar | Single PreferenceFragment | Replace with Fragment + ViewPager/BottomNavigation |
| Proxy card list | ListPreference dropdown | RecyclerView with custom card layout |
| Proxy detail modal | Inline preferences | Bottom sheet dialog fragment |
| Add/Edit proxy modal | AlertDialog + EditText | Bottom sheet with form validation |
| Form validation | Minimal port check | Real-time validation with icons |
| Status tab | MaterialSwitch only | New fragment with status card + info grid |
| Split tunneling tabs | Single checkbox + list | TabLayout with filtered app lists |
| Dark/light theme | dynamic_colors only | Theme switching with CSS-like variables |

---

## Recommendations

1. **Phase 1 (Backend):** Add dark mode preference, connection timestamp, and IP tracking to the backend. These are small, isolated changes.

2. **Phase 2 (UI Foundation):** Restructure MainActivity to use bottom navigation with 3 fragments (Proxies, Status, Settings). This is the biggest structural change.

3. **Phase 3 (UI Features):** Implement proxy cards, detail modals, and form validation. These are purely UI and can be built incrementally.

4. **Phase 4 (Polish):** Add language selector, form validation animations, and theme switching.

5. **Deferred:** Per-profile connection status is low priority since the app only supports one active VPN connection at a time.

---

## Risk Assessment

- **Low Risk:** All connection/profile features already work. UI restructuring won't break backend logic.
- **Medium Risk:** IP address lookup on VPN connect could add latency or fail on some networks. Consider making it async/non-blocking.
- **High Risk:** Language switching can break if string resources aren't complete. Start with 2-3 languages max.
