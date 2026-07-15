# Compose UI Design — KiloProxy

## Brand
- **Name:** KiloProxy (internal package: `net.typeblog.socks`)
- **Style:** Clean, monochrome, Vercel-inspired
- **Font:** `fontFamily = FontFamily.Default` (system sans-serif, Inter-like on modern Android)

## Color Palette (Material3)

### Light Theme
| Token | Value | Usage |
|---|---|---|
| `primary` | `#2563EB` | Accent, FAB, active tab, toggles ON |
| `onPrimary` | `#FFFFFF` | Text on primary |
| `surface` | `#FAFAFA` | Page background |
| `surfaceVariant` | `#FFFFFF` | Cards, settings group |
| `surfaceTint` | `#F5F5F5` | Elevated surfaces |
| `outline` | `#E5E5E5` | Borders |
| `outlineVariant` | `#EEEEEE` | Light borders |
| `onSurface` | `#111111` | Primary text |
| `onSurfaceVariant` | `#666666` | Secondary text |
| `tertiary` | `#16A34A` | Green (connected, valid) |
| `error` | `#DC2626` | Red (disconnected, invalid) |
| `surfaceContainerLow` | `#FFFFFF` | Bottom nav, cards |
| `surfaceContainerHighest` | `#F5F5F5` | Info rows, icons bg |

### Dark Theme
| Token | Value | Usage |
|---|---|---|
| `primary` | `#3B82F6` | Accent |
| `surface` | `#000000` | Page bg |
| `surfaceVariant` | `#0A0A0A` | Cards |
| `surfaceTint` | `#111111` | Elevated |
| `outline` | `#222222` | Borders |
| `outlineVariant` | `#1A1A1A` | Light borders |
| `onSurface` | `#FFFFFF` | Primary text |
| `onSurfaceVariant` | `#888888` | Secondary text |
| `tertiary` | `#22C55E` | Green |
| `error` | `#EF4444` | Red |
| `surfaceContainerLow` | `#0A0A0A` | Bottom nav |
| `surfaceContainerHighest` | `#111111` | Info rows |

## Typography
- Font: System default (Inter or SF Pro on modern Android)
- Headings: 18px bold (`TitleMedium`)
- Body: 14px regular/normal (`BodyMedium`)
- Captions: 12px (`BodySmall`)
- Labels: 11px uppercase semibold (`LabelSmall`)

## Navigation Structure

```
NavHost(startDestination = "proxies")
├── composable("proxies")          → ProxiesScreen
│   ├── ProxyCard list
│   ├── FAB → AddEditProxySheet
│   └── ProxyDetailSheet (on card tap)
├── composable("status")           → StatusScreen
│   ├── ConnectionCard (shield icon + badge)
│   ├── Info grid (IP, Server, Connected since)
│   └── VpnButton (Start/Stop)
├── composable("settings")         → SettingsScreen
│   ├── Appearance section (theme mode, lang)
│   ├── Connection section (auto-connect, connectivity check, UDP, IPv6)
│   ├── Routing section (route pref, DNS)
│   ├── Advanced section (auto-stop)
│   ├── Split Tunneling item → SplitTunnelingScreen
│   └── About (version)
└── composable("split_tunneling")  → SplitTunnelingScreen
    ├── Allowed/Blocked segmented tabs
    └── Per-app toggle rows
```

## Bottom Navigation Bar
- 3 tabs: **Proxies** (globe icon), **Status** (shield icon), **Settings** (gear icon)
- Active tab: primary color accent, small top indicator line
- Inactive: tertiary text color
- Background: surfaceContainerLow

## Component Specifications

### ProxyCard
- Background: surfaceContainerLow
- Border: 1dp outline, rounded 10dp
- Padding: 14dp h, 16dp v
- Left: status dot (8dp circle, gray/tertiary)
- Middle: name (BodyMedium, semibold, onSurface), address (BodySmall, onSurfaceVariant)
- Right: "SOCKS5" chip (primary container, 10dp font, pill shape)
- Elevation: none (border-based), lifts on press

### Status Card
- Background: surfaceContainerLow
- Border: 1dp outline, rounded 12dp
- Center: shield icon in circle (56dp, green-bg when connected, gray-bg when not)
- Label: "Your connection state" (TitleSmall)
- Badge: "Connected" (tertiary-bg, tertiary) or "Disconnected" (surfaceVariant, onSurfaceVariant)
- Info grid: 3 rows (Your IP, Server, Connected since) on surfaceTint bg

### Settings Group
- Section title: 11dp uppercase, tertiary, 8dp bottom margin
- Group: surfaceContainerLow, 1dp outline, rounded 10dp
- Item: 14dp h padding, icon (30dp box, surfaceTint bg, 8dp rounded), label + optional desc, value/toggle on right
- Last item: no bottom border (handled by group rounding)

### Toggle Switch
- Material3 Switch component (default)
- Active: primary, Inactive: outline

### Button Styles
- **Primary**: primary bg, white text, rounded 8dp
- **Outline**: transparent, onSurface text, outline border
- **Danger/Stop**: error bg (light), error text + border (or full error bg on both themes)
- **FAB**: 52dp circle, primary bg, white + icon, elevated shadow

### Add/Edit Proxy Sheet (Modal)
- Bottom sheet (ModalBottomSheet from Material3)
- Form fields: Profile Name, Host (with validation), Port (with validation), Auth Type dropdown, conditional User/Pass, DNS
- Validation: host non-empty, port 1-65535, auth fields required when auth enabled
- Validation icons: ✓ (tertiary) / ✗ (error) per field
- Footer: Cancel + Save buttons

## Shared State (ViewModel)

### VpnViewModel
```kotlin
class VpnViewModel : ViewModel() {
    // VPN State
    val isRunning: StateFlow<Boolean>
    val currentIp: StateFlow<String?>
    val countryCode: StateFlow<String?>
    val connectedSince: StateFlow<Long>
    val realIp: StateFlow<String?>
    val realCountryCode: StateFlow<String?>

    // Actions
    fun startVpn(profileName: String)
    fun stopVpn()

    // Internal
    fun bindService()
    fun unbindService()
}
```

### ProfileRepository
```kotlin
class ProfileRepository(private val context: Context) {
    val profiles: StateFlow<List<String>>
    fun addProfile(name: String)
    fun removeProfile(name: String)
    fun switchProfile(name: String)
    fun getProfile(name: String): Profile
}
```

## File Organization
```
java/net/typeblog/socks/
├── MainActivity.kt             ← Rewritten: Compose entry, NavHost, bottom nav
├── SocksApplication.kt         ← Keep as-is (or minor update)
├── ui/
│   ├── theme/
│   │   ├── Theme.kt            ← Compose theme (light/dark)
│   │   ├── Color.kt            ← Color definitions
│   │   └── Type.kt             ← Typography
│   ├── navigation/
│   │   └── AppNavigation.kt    ← Routes + NavHost
│   ├── components/
│   │   ├── ProxyCard.kt
│   │   ├── ConnectionCard.kt
│   │   ├── SettingsItem.kt
│   │   ├── VpnButton.kt
│   │   └── AppToggleItem.kt
│   ├── screens/
│   │   ├── ProxiesScreen.kt
│   │   ├── StatusScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── SplitTunnelingScreen.kt
│   └── viewmodel/
│       └── VpnViewModel.kt
└── (all existing backend files kept as-is)
```

## Agent File Ownership (NO OVERLAP)

| Agent | Files to Create/Modify |
|---|---|
| **Agent 1 — Foundation** | `app/build.gradle` (add deps), `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt`, `ui/navigation/AppNavigation.kt`, `MainActivity.kt` (rewrite), `SocksApplication.kt` (minor update) |
| **Agent 2 — Proxies & Status** | `ui/components/ProxyCard.kt`, `ui/components/ConnectionCard.kt`, `ui/components/VpnButton.kt`, `ui/screens/ProxiesScreen.kt`, `ui/screens/StatusScreen.kt`, `ui/viewmodel/VpnViewModel.kt` |
| **Agent 3 — Settings & Split** | `ui/components/SettingsItem.kt`, `ui/components/AppToggleItem.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/SplitTunnelingScreen.kt` |

## Critical Rules
1. **NEVER modify** `jni/`, `aidl/`, `util/Countries.kt`, `SocksVpnService.kt`, `BootReceiver.kt`, `System.kt`, `AppSelector.kt`, `ProfileFragment.kt`, or any XML resource
2. Keep `IVpnService.aidl` as-is — wrap calls in VpnViewModel
3. Use Material3 components only (Material2 is deprecated)
4. Import theme colors from `net.typeblog.socks.ui.theme.*`
5. All screen Composables accept `modifier: Modifier = Modifier` parameter
6. Use `remember`/`mutableStateOf` for local UI state, `viewModel()` for shared state
7. KEEP existing files functional — don't break the old PreferenceFragment UI
8. Bottom nav and NavHost live in MainActivity
9. Each screen is a standalone @Composable — no cross-screen dependencies
10. Light theme is default

## Implementation Status

✅ **Implemented (Jul 15, 2026):**
- build.gradle: Compose deps added
- Color.kt, Type.kt, Theme.kt: Full theme system
- AppNavigation.kt: NavHost + bottom nav
- MainActivity.kt: Rewritten for Compose
- VpnViewModel.kt: Service binding + state polling
- ProxiesScreen.kt, StatusScreen.kt, SettingsScreen.kt, SplitTunnelingScreen.kt: All 4 screens
- ProxyCard.kt, ConnectionCard.kt, VpnButton.kt, SettingsItem.kt, AppToggleItem.kt: All components

🔄 **Next:** Compile, fix errors, verify navigation, remove old XML/Fragment code
