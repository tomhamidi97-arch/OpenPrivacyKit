# OpenPrivacyKit

**Open-source per-app Android identifier spoofing for LSPosed** — an auditable,
community-driven upgrade of the closed-source "Privacy Kit" concept.

> 開源、可审计的 LSPosed 按应用标识符伪造模块。

## Why

Closed-source privacy modules ask for total trust: their code runs inside every
app you scope them to, with effectively full access to that app's data. This
project exists so you don't have to trust anyone — you can read every line,
build it yourself, and get a byte-identical APK.

## What it does

| Hook | Field | Notes |
|---|---|---|
| Settings.Secure | `android_id` | per-app stable random or custom value |
| Build fields | `MODEL`, `MANUFACTURER`, `BRAND`, `DEVICE`, `FINGERPRINT`, `PRODUCT` | reflection set at process start |
| Advertising ID | GMS `AdvertisingIdClient$Info.getId` | zeroed UUID |
| Network | `NetworkInterface.getHardwareAddress` | locally-administered random MAC |

Every hook:
- is a small standalone class you can audit in one sitting,
- is off unless you explicitly enable it for an app,
- logs its activation to the Xposed log (`[OpenPrivacyKit]` tag).

## Requirements

- Rooted device with Magisk + LSPosed
- Android 8.1+ (API 27)

## Build

```bash
git clone https://github.com/<you>/OpenPrivacyKit
cd OpenPrivacyKit
./gradlew assembleRelease
```

Or use the GitHub Actions workflow in `.github/workflows/build.yml` — every
push produces a debug APK artifact, and tagged pushes produce release APKs.
Signing: CI builds are unsigned (debug-signed); for your own releases, add a
signing config locally. **Do not trust APKs not built from a tagged commit.**

## Usage

1. Install the APK, enable the module in LSPosed.
2. Scope it to the apps you want to affect (keep scope narrow).
3. Open OpenPrivacyKit, pick an app, set values (`!` = auto-random), save.
4. Force-stop and reopen the target app.

## Roadmap

- [ ] MediaDrm / Widevine ID hook
- [ ] Sensor fingerprint (accelerometer bias)
- [ ] Identifier rotation scheduling (time-based)
- [ ] Read-out detection report ("what did this app read?")
- [ ] Per-app randomization consistency check (timezone/language coherence)

## Limitations

This module is one privacy layer, not anonymity. Apps can still correlate via
account login, IP, cookies, sensors, and server-side records. See the
disclaimer: spoofing may break apps, trigger integrity checks, or violate
terms of service. Use responsibly.

## License

GPL-3.0-or-later. Fork it, audit it, improve it.
