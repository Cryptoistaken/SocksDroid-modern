# Session 2 — Kotlin Migration + Remaining Phase 1 Fixes

> No UI work — that's last.
> All agents run in parallel. Zero file overlap.

---

## Agent 0 — Build Config (Kotlin Setup)
**Files:**
- `build.gradle` (root)
- `app/build.gradle`

**Changes:**
1. Root `build.gradle` — add `kotlin-gradle-plugin` to `buildscript.dependencies`
2. `app/build.gradle` — apply `kotlin-android` plugin, add `kotlin-stdlib` dependency

---

## Agent A — Data Layer (6 files)
**Files:**
- `util/Constants.java` → `Constants.kt`
- `util/Profile.java` → `Profile.kt`
- `util/ProfileManager.java` → `ProfileManager.kt`
- `util/ProfileFactory.java` → `ProfileFactory.kt`
- `util/Routes.java` → `Routes.kt`
- `util/Utility.java` → `Utility.kt`

**Changes:**
1. Convert each `.java` to idiomatic Kotlin `.kt` (same package, same class names, same API surface)
2. Delete the old `.java` files after writing `.kt` versions
3. Keep all existing method signatures so callers don't break
4. Keep EncryptedSharedPreferences, `.apply()`, `.equals()` fixes from Session 1

---

## Agent B — Service Layer (3 files + fixes)
**Files:**
- `System.java` → `System.kt`
- `SocksVpnService.java` → `SocksVpnService.kt`
- `BootReceiver.java` → `BootReceiver.kt`

**Changes:**
1. Convert each `.java` to idiomatic Kotlin `.kt`
2. Delete the old `.java` files after writing `.kt` versions
3. In `SocksVpnService.kt` — fix the exported service: add a permission check in `onBind()` to verify caller
4. Keep all Session 1 fixes (no CLI creds, 10.10.10.x IPs, empty catch → Log.e)

---

## Agent C — UI Layer (4 files)
**Files:**
- `MainActivity.java` → `MainActivity.kt`
- `ProfileFragment.java` → `ProfileFragment.kt`
- `SocksApplication.java` → `SocksApplication.kt`
- `AppSelector.java` → `AppSelector.kt`

**Changes:**
1. Convert each `.java` to idiomatic Kotlin `.kt`
2. Delete the old `.java` files after writing `.kt` versions
3. Keep all Session 1 fixes (AppCompatActivity, PreferenceFragmentCompat, ActivityResultLauncher, lifecycle-aware polling, etc.)

---

## File Ownership Matrix

| File | Agent |
|------|-------|
| `build.gradle` (root) | 0 |
| `app/build.gradle` | 0 |
| `util/Constants.java` → `.kt` | A |
| `util/Profile.java` → `.kt` | A |
| `util/ProfileManager.java` → `.kt` | A |
| `util/ProfileFactory.java` → `.kt` | A |
| `util/Routes.java` → `.kt` | A |
| `util/Utility.java` → `.kt` | A |
| `System.java` → `.kt` | B |
| `SocksVpnService.java` → `.kt` | B |
| `BootReceiver.java` → `.kt` | B |
| `MainActivity.java` → `.kt` | C |
| `ProfileFragment.java` → `.kt` | C |
| `SocksApplication.java` → `.kt` | C |
| `AppSelector.java` → `.kt` | C |

No file is touched by more than one agent.
