# Changelog

Format berdasarkan [Keep a Changelog](https://keepachangelog.com/).

## [1.0.0] - 2026-10-25

Rilis pertama DashLock — kunci layar untuk head unit Android dengan overlay floating dan blok tombol sistem.

### Added

- Foreground service + overlay layar penuh saat terkunci, tombol floating draggable (snap kiri/kanan).
- Layanan aksesibilitas: blok HOME, BACK, recent, volume, dan media saat terkunci.
- Gestur kunci (tahan ~200 ms) dengan konfirmasi opsional.
- Gestur buka: tahan 1–3 detik atau ketuk 3×; PIN opsional dengan hash SHA-256.
- Pengaturan: toggle aktif, konfirmasi kunci, PIN buka kunci, mode & durasi gestur.
- Toast 「Layar dikunci」 / 「Layar dibuka」.
- Dialog PIN di overlay dengan keyboard otomatis dan submit lewat IME Done.
- Start otomatis setelah boot / user unlock / update APK bila DashLock masih aktif.
- UI pengaturan per section: Status, Kunci, Buka kunci, Izin.
- Build tanpa Gradle (`aapt2`, `javac`, `d8`); Android 7+ (API 24).

### Fixed

- Izin overlay: deklarasi `SYSTEM_ALERT_WINDOW` agar toggle tidak abu-abu di head unit.
- Layer modal layar penuh yang menahan semua sentuhan termasuk tombol floating.
- Deteksi aksesibilitas via `Settings.Secure` pada firmware head unit.
- Urutan window overlay agar tombol floating selalu di atas perisai.

[1.0.0]: https://github.com/dimashpt/dashlock/releases/tag/v1.0.0
