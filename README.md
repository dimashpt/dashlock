# DashLock

Screen lock for **Android head units** (Android 7+): a floating button over any app, touch blocking while locked, and **HOME / BACK / volume** blocking via an **Accessibility** service when locked.

Tested on **8227L** and **NWD K2401** head units (landscape 1280×720). Plain Java, no Gradle.

<p align="center">
  <a href="https://github.com/dimashpt/dashlock/releases/latest"><img src="https://img.shields.io/github/v/release/dimashpt/dashlock?label=release" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-7%2B-3DDC84?logo=android&logoColor=white" alt="Android 7+">
  <img src="https://img.shields.io/badge/build-aapt2%20%2B%20javac-lightgrey" alt="No Gradle">
</p>

<p align="center">
  <b>Bahasa Indonesia:</b> <a href="docs/README.id.md">docs/README.id.md</a><br>
  <sub>Independent project; not affiliated with any head unit vendor or other app.</sub>
</p>

## Features

- **Floating overlay** — semi-transparent yellow button; drag to move; snaps to left or right edge.
- **Screen lock** — touches do not reach apps below; only the DashLock button stays active.
- **Accessibility** — while locked, blocks system keys (HOME, BACK, recents, volume, media).
- **Lock confirmation** — optional (default **on**).
- **Unlock** — hold 1–3 seconds **or** triple-tap; optional PIN (default **gesture only**).
- **PIN** — SHA-256 hash + salt; keyboard auto-shown when PIN is required; IME Done submits.
- **Toasts** — “Screen locked” / “Screen unlocked” (Indonesian strings in the app UI).
- **After reboot** — restarts automatically if DashLock was left enabled (boot + user unlock).

## Download

Release APK: **[Releases](https://github.com/dimashpt/dashlock/releases/latest)** (`dashlock-1.0.0.apk`).

## Requirements

| Permission | Purpose |
|------------|---------|
| Display over other apps | Shield overlay + floating button |
| DashLock accessibility service | Block hardware/on-screen keys while locked |
| Notification (foreground service) | Keep DashLock running in the background |

## First-time setup

1. Install the APK and open **DashLock**.
2. Under **Permissions**, allow **overlay** and enable **DashLock** in accessibility settings.  
   If the overlay toggle is greyed out, reinstall the latest APK or run:
   ```bash
   adb shell appops set id.sooon.dashlock SYSTEM_ALERT_WINDOW allow
   ```
3. Under **DashLock status**, turn on **Enable DashLock**.
4. (Optional) Under **Unlock**, enable **PIN required to unlock** and set a PIN (4–6 digits).

### Default settings (fresh install)

| Setting | Default |
|---------|---------|
| Confirm before locking | ON |
| PIN required to unlock | OFF |
| Unlock gesture | Hold 2 seconds |
| Enable DashLock | OFF (you turn it on) |

All settings persist in `SharedPreferences` across reboots.

## Usage

| Action | Gesture |
|--------|---------|
| Move button | Press and drag; release to snap left/right |
| Lock | Hold the button ~0.2 s without moving → confirm if enabled |
| Unlock (hold) | While locked: hold 1–3 s (per settings) → PIN if required |
| Unlock (tap) | While locked: triple-tap quickly → PIN if required |

Turn off **Enable DashLock** to hide the button (only when the screen is **not** locked).

## Settings UI

1. **DashLock status** — enable toggle, status text, gesture summary  
2. **Lock** — confirm before locking  
3. **Unlock** — PIN, gesture mode, hold duration  
4. **Permissions** — overlay and accessibility  

## Build (developers)

```bash
chmod +x build.sh
./build.sh              # build/dashlock.apk (debug keystore)
./build.sh release      # build/dashlock-1.0.0.apk
./build.sh install      # DEVICE=ip:5555
```

- `minSdk 24`, `targetSdk 29`, package `id.sooon.dashlock`
- Production signing: add `dashlock.keystore` or set `DASHLOCK_KS_PASS`

## Limitations

- **POWER** cannot be fully blocked.
- Some firmware still allows the status bar / notification shade.
- Accessibility must stay enabled in system settings; when DashLock is disabled, keys are not blocked.

## License

MIT — see [LICENSE](LICENSE).
