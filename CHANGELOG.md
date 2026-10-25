# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-25

First stable release of DashLock — screen lock for Android head units with a floating overlay and system key blocking.

### Added

- Foreground service and full-screen shield while locked; draggable floating button (snap to left/right).
- Accessibility service: block HOME, BACK, recents, volume, and media keys while locked.
- Lock gesture (~200 ms hold) with optional confirmation dialog.
- Unlock gestures: hold 1–3 seconds or triple-tap; optional PIN with SHA-256 hash + salt.
- Settings: enable toggle, lock confirmation, unlock PIN, gesture mode and hold duration.
- Toasts when the screen is locked or unlocked.
- PIN dialog on the overlay with automatic keyboard and IME Done to submit.
- Auto-start after boot, user unlock, or APK update when DashLock remains enabled.
- Settings UI grouped into sections: Status, Lock, Unlock, Permissions.
- Build without Gradle (`aapt2`, `javac`, `d8`); Android 7+ (API 24).
- Package name `id.sooon.dashlock`.

[1.0.0]: https://github.com/dimashpt/dashlock/releases/tag/v1.0.0
