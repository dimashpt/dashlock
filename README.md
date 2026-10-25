# DashLock

Kunci layar untuk **head unit Android** (Android 7+): tombol floating di atas semua aplikasi, blok sentuhan saat terkunci, dan blok tombol **HOME / BACK / volume** lewat layanan **Aksesibilitas** saat terkunci.

Dikembangkan dan diuji di head unit **8227L** dan **NWD K2401** (landscape 1280×720). Java murni, tanpa Gradle.

<p align="center">
  <a href="https://github.com/dimashpt/dashlock/releases/latest"><img src="https://img.shields.io/github/v/release/dimashpt/dashlock?label=release" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-7%2B-3DDC84?logo=android&logoColor=white" alt="Android 7+">
  <img src="https://img.shields.io/badge/build-aapt2%20%2B%20javac-lightgrey" alt="No Gradle">
</p>

<p align="center"><sub>Proyek mandiri; tidak berafiliasi dengan produsen head unit maupun aplikasi lain.</sub></p>

## Fitur

- **Overlay floating** — tombol kuning semi-transparan, bisa digeser; menempel kiri/kanan saat dilepas.
- **Kunci layar** — sentuhan ke aplikasi di bawah diblok; hanya tombol DashLock yang aktif.
- **Aksesibilitas** — saat terkunci, tombol sistem (HOME, BACK, recent, volume, media) ditahan.
- **Konfirmasi kunci** — opsional (default **aktif**).
- **Buka kunci** — tahan 1–3 detik **atau** ketuk 3×; PIN opsional (default **tanpa PIN**, cukup gestur).
- **PIN** — hash SHA-256 + salt; keyboard otomatis saat PIN wajib; tombol centang keyboard = submit.
- **Toast** — 「Layar dikunci」 / 「Layar dibuka」.
- **Setelah reboot** — jika DashLock terakhir aktif, layanan overlay menyala lagi (boot + user unlock).

## Unduh

APK rilis: **[Releases](https://github.com/dimashpt/dashlock/releases/latest)** (`dashlock-1.0.0.apk`).

## Persyaratan

| Izin | Untuk apa |
|------|-----------|
| Tampil di atas aplikasi lain | Overlay perisai + tombol floating |
| Layanan aksesibilitas DashLock | Blok tombol fisik/virtual saat terkunci |
| Notifikasi (foreground service) | DashLock tetap jalan di latar |

## Setup pertama

1. Pasang APK → buka **DashLock**.
2. Di section **Izin** → izinkan **overlay** dan aktifkan **DashLock** di pengaturan aksesibilitas.  
   Jika toggle overlay abu-abu, pasang ulang APK terbaru atau:
   ```bash
   adb shell appops set id.sooon.dashlock SYSTEM_ALERT_WINDOW allow
   ```
3. Di **Status DashLock** → nyalakan **Aktifkan DashLock**.
4. (Opsional) Di **Buka kunci** → nyalakan **Wajib PIN** lalu **Atur / ganti PIN** (4–6 digit).

### Default pengaturan (instal baru)

| Pengaturan | Default |
|------------|---------|
| Konfirmasi sebelum mengunci | ON |
| Wajib PIN saat membuka | OFF |
| Gestur buka kunci | Tahan 2 detik |
| Aktifkan DashLock | OFF (user menyalakan sendiri) |

Semua disimpan di `SharedPreferences` (persisten setelah reboot).

## Cara pakai

| Aksi | Gestur |
|------|--------|
| Pindah tombol | Tahan & geser; lepas → snap kiri/kanan |
| Kunci | Tahan tombol ~0,2 detik (tanpa geser) → konfirmasi jika diaktifkan |
| Buka (tahan) | Saat terkunci: tahan 1–3 detik (sesuai pengaturan) → PIN jika wajib |
| Buka (ketuk) | Saat terkunci: ketuk 3× cepat → PIN jika wajib |

Matikan **Aktifkan DashLock** untuk menghilangkan tombol (hanya saat layar **tidak** terkunci).

## UI pengaturan

1. **Status DashLock** — toggle aktif, pesan status, ringkasan gestur  
2. **Kunci** — konfirmasi sebelum mengunci  
3. **Buka kunci** — PIN, gestur, durasi tahan  
4. **Izin** — overlay & aksesibilitas  

## Build (pengembang)

```bash
chmod +x build.sh
./build.sh              # build/dashlock.apk (debug keystore)
./build.sh release      # build/dashlock-1.0.0.apk
./build.sh install      # DEVICE=ip:5555
```

- `minSdk 24`, `targetSdk 29`, package `id.sooon.dashlock`
- Rilis produksi: siapkan `dashlock.keystore` atau set `DASHLOCK_KS_PASS`

## Batasan

- Tombol **POWER** tidak bisa diblok sepenuhnya.
- Beberapa firmware masih mengizinkan status bar / notifikasi.
- Layanan aksesibilitas harus tetap di-enable di Settings; saat DashLock nonaktif, tidak ada blok tombol.

## Lisensi

MIT — lihat [LICENSE](LICENSE).
