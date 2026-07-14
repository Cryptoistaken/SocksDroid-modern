# Fixes Orchestration Plan

> Each agent handles **completely separate files** — no overlaps.
> After all agents complete, review changes then commit when ready.

---

## Agent 1 — Data Layer (Profile / ProfileManager)
**Files:**
- `app/src/main/java/net/typeblog/socks/util/Profile.java`
- `app/src/main/java/net/typeblog/socks/util/ProfileManager.java`
- `app/src/main/java/net/typeblog/socks/util/ProfileFactory.java`
- `app/src/main/java/net/typeblog/socks/util/Constants.java`

**Changes:**
1. Migrate `Profile.java` to `EncryptedSharedPreferences` (AndroidX Security)
2. Replace all `commit()` with `apply()` in Profile setters
3. Fix `ProfileManager.java:84` — `==` → `.equals()`
4. Update `ProfileManager.java` to use `EncryptedSharedPreferences`
5. Add `MASTER_KEY_ALIAS` constant to `Constants.java`
6. Adjust `ProfileFactory.java` if constructor signature changes

---

## Agent 2 — VPN Service & Bridge
**Files:**
- `app/src/main/java/net/typeblog/socks/SocksVpnService.java`
- `app/src/main/java/net/typeblog/socks/util/Utility.java`
- `app/src/main/java/net/typeblog/socks/System.java`
- `app/src/main/java/net/typeblog/socks/BootReceiver.java`

**Changes:**
1. `SocksVpnService.java` — Remove credential CLI args (`--username`/`--password`), pass via environment or drop (tun2socks doesn't require them at startup)
2. `SocksVpnService.java` — Fix empty catch blocks (log exceptions)
3. `SocksVpnService.java` — Change hardcoded IP `26.26.26.0/24` → proper private range `10.0.0.0/24` or `172.16.0.0/24`
4. `Utility.java` — Remove `INTENT_USERNAME`/`INTENT_PASSWORD` from startVpn intent
5. `System.java` — Remove `exec()` native method declaration
6. `BootReceiver.java` — Minor: fix empty catch if any

---

## Agent 3 — Native C Code
**Files:**
- `app/src/main/jni/system.cpp`
- `app/src/main/jni/badvpn/tun2socks/tun2socks.c`
- `app/src/main/jni/Android.mk`

**Changes:**
1. `system.cpp` — Remove `Java_net_typeblog_socks_system_exec()` function and `JNI_OnLoad` registration for it
2. `tun2socks.c` — Fix `strcpy(argv[0], "net.typeblog.socks")` → use `snprintf` or check bounds
3. `Android.mk` — Add hardening flags: `-fstack-protector-strong -D_FORTIFY_SOURCE=2` to CFLAGS

---

## Agent 4 — UI Layer
**Files:**
- `app/src/main/java/net/typeblog/socks/MainActivity.java`
- `app/src/main/java/net/typeblog/socks/ProfileFragment.java`
- `app/src/main/java/net/typeblog/socks/SocksApplication.java`
- `app/src/main/java/net/typeblog/socks/AppSelector.java`

**Changes:**
1. `MainActivity.java` — `extends Activity` → `extends AppCompatActivity`
2. `MainActivity.java` — Remove dead commented-out code (lines 22-45)
3. `MainActivity.java` — Use `getSupportFragmentManager()`
4. `ProfileFragment.java` — `PreferenceFragment` → `PreferenceFragmentCompat` (from AndroidX)
5. `ProfileFragment.java` — `addPreferencesFromResource()` → `setPreferencesFromResource()`
6. `ProfileFragment.java` — `startActivityForResult()` → `ActivityResultLauncher`
7. `ProfileFragment.java` — Replace polling loop with `ServiceConnection` callback pattern or `LiveData`
8. `ProfileFragment.java` — Fix all empty catch blocks (log exceptions)
9. `ProfileFragment.java` — Simplify password masking: `String.format(...)` → `p.getText().replaceAll(".", "*")`
10. `SocksApplication.java` — Fix Dynamic Colors race on first launch (initialize default prefs + read after init)
11. `AppSelector.java` — Fix double inflation (remove dead `simple_list_item_multiple_choice` inflation)

---

## Agent 5 — Build & Config
**Files:**
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/proguard-rules.pro`

**Changes:**
1. `app/build.gradle` — Enable R8: `minifyEnabled true`, add `release { ... }` config
2. `app/build.gradle` — Add dependency: `androidx.security:security-crypto:1.1.0-alpha06`
3. `app/build.gradle` — Add dependency: `androidx.appcompat:appcompat:1.6.1`
4. `app/build.gradle` — Add dependency: `androidx.preference:preference-ktx:1.2.1`
5. `AndroidManifest.xml` — Set `android:allowBackup="false"`
6. `AndroidManifest.xml` — Set `android:exported="false"` on BootReceiver (BOOT_COMPLETED is system-only)
7. `proguard-rules.pro` — Add keep rules for JNI classes

---

## File Ownership Matrix

| File | Agent |
|------|-------|
| `Profile.java` | 1 |
| `ProfileManager.java` | 1 |
| `ProfileFactory.java` | 1 |
| `Constants.java` | 1 |
| `SocksVpnService.java` | 2 |
| `Utility.java` | 2 |
| `System.java` | 2 |
| `BootReceiver.java` | 2 |
| `system.cpp` | 3 |
| `tun2socks.c` | 3 |
| `Android.mk` | 3 |
| `MainActivity.java` | 4 |
| `ProfileFragment.java` | 4 |
| `SocksApplication.java` | 4 |
| `AppSelector.java` | 4 |
| `app/build.gradle` | 5 |
| `AndroidManifest.xml` | 5 |
| `proguard-rules.pro` | 5 |

No file is touched by more than one agent.
