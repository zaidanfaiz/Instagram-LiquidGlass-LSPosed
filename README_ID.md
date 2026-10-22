<div align="center">

# Instagram Liquid Glass

**Navigasi Instagram melayang dengan tampilan frosted glass di Android.**

Modul LSPosed independen — tidak perlu menginstal APK Instagram modifikasi.
<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Inter&amp;weight=600&amp;size=21&amp;duration=2800&amp;pause=1100&amp;color=91A0B3&amp;center=true&amp;vCenter=true&amp;width=780&amp;height=48&amp;lines=Floating+glass.+Instagram+asli.;Home+%E2%86%92+Reels+%E2%86%92+DM+%E2%86%92+Profil+%E2%86%92+Search;Dibuat+untuk+Android+%26+LSPosed" alt="Animasi judul proyek: navbar kaca melayang untuk Instagram" />
</p>


[![Build](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml/badge.svg)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/actions/workflows/build-apk.yml)
[![Release](https://img.shields.io/badge/release-v0.1.0--beta.1-F5A623)](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1)
![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Framework](https://img.shields.io/badge/framework-LSPosed-5865F2)

**[Unduh APK](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1) · [Preview](#preview--animasi) · [Cara instal](#instalasi) · [Kompatibilitas](#kompatibilitas) · [Lapor bug](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues/new/choose)**

[English](README.md) · **Bahasa Indonesia**

</div>

---

## Navbar Instagram, tampilan baru

**Home → Reels → DM → Profil → Search**

Empat tab utama dalam kapsul kaca melayang, dengan tombol Search terpisah di sisi kanan. Navigasinya memakai aplikasi Instagram asli.

<div align="center">
<img src="assets/readme/hero.svg" alt="ZF branded Instagram Liquid Glass concept artwork" width="100%">
<sub>Mockup desain konseptual, bukan screenshot aplikasi asli.</sub>
</div>

### Fitur utama

| Fitur | Penjelasan |
| :--- | :--- |
| **Floating glass navbar** | Kapsul transparan dengan blur, tepi reflektif, dan animasi lensa aktif |
| **Blur pada Reels** | Sampling video terbatas memakai Android `PixelCopy` secara asinkron |
| **Lima tujuan** | Home, Reels, inbox DM, profil sendiri, dan Search di lingkaran terpisah |
| **Navigasi geser** | Geser lensa pada navbar; perpindahan lewat swipe isi halaman menggunakan remapping terbatas |
| **Tampil sesuai konteks** | Dirancang menghilang ketika membuka komentar Reels, chat DM, Story, atau profil orang lain |
| **Instagram asli** | Tidak mendistribusikan APK Instagram yang telah diubah |

> [!NOTE]
> Ini adalah **beta**. Efek blur terinspirasi Liquid Glass, tetapi belum memakai compositor iOS. Sebagian video atau konten tertentu dapat menampilkan kaca bening saat frame tidak bisa diambil. Swipe isi halaman belum mengganti pager internal Instagram secara langsung.

## Preview & animasi

<div align="center">
<img src="assets/readme/tested-application.svg" alt="Animasi HP POCO F7 Ultra dengan branding ZF dan detail lingkungan pengujian" width="95%">

**Animasi ilustratif** — tampilan HP dan perpindahan lensa adalah mockup, bukan rekaman hasil pengujian nyata.
</div>

### Galeri asli

Rekaman asli untuk Home, Reels, DM, Profil, dan Search dapat ditambahkan di `assets/readme/screenshots/`. Lihat [panduan aset](assets/readme/README.md). Screenshot belum dilampirkan sehingga tidak ada klaim visual yang tidak teruji.

## Kompatibilitas

**Modul ini tidak dikunci ke satu nomor versi Android atau Instagram.** Selama elemen navigasi Instagram kompatibel, modul **berpotensi berjalan di versi lain**. Namun, versi yang belum diuji belum bisa dijamin semuanya berfungsi.

| Persyaratan | Keterangan |
| :--- | :--- |
| **Android** | Android 10+ (minimum instalasi APK: API 29); fitur tertentu tergantung Android/ROM |
| **Instagram** | Aplikasi `com.instagram.android` terinstal |
| **Framework** | LSPosed aktif, dengan scope Instagram |
| **Perangkat** | Root atau lingkungan yang mendukung LSPosed |
| **Versi lain** | Boleh dicoba; kompatibilitas belum seluruhnya diverifikasi |

### Perangkat pengujian pengembang

<img src="assets/readme/tested-application.svg" alt="Animated branded test device illustration" width="100%">

**Catatan:** Informasi perangkat di bawah adalah catatan pengujian; ilustrasi di atas bukan bukti pengujian visual.


| Informasi | Konfigurasi |
| :--- | :--- |
| **Model HP** | **POCO F7 Ultra** |
| **Android** | **Android 16 (API 36)** |
| **Instagram** | **452.0.0.48.64** |
| **Resolusi window berdasarkan log** | **1440 × 3200 piksel** |
| **Paket aplikasi** | `com.instagram.android` |
| **Nama ROM / build** | Belum tercatat |

Data tersebut adalah **lingkungan pengujian**, bukan batasan pemasangan untuk pengguna lain. Laporan hasil uji di berbagai perangkat dan versi Instagram sangat membantu pengembangan.

## Instalasi

1. Buka **[GitHub Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases/tag/v0.1.0-beta.1)** dan unduh **APK modul**. Tersedia juga `SHA256SUMS.txt` untuk verifikasi checksum.
2. Instal APK modul pada HP yang sudah menggunakan LSPosed.
3. Buka **LSPosed → Modules**, aktifkan **Instagram Liquid Glass**, lalu pilih scope **Instagram (`com.instagram.android`)**.
4. **Paksa berhenti (force-stop) Instagram**, lalu buka kembali.
5. Uji semua tab, gesture geser, blur, dan buka-tutup komentar Reels.

> [!WARNING]
> Jika Instagram menjadi tidak stabil, video macet, atau tampilan bermasalah, nonaktifkan modul di LSPosed dan force-stop Instagram. **Jangan menghapus Instagram atau datanya** untuk mengatasi masalah modul.

### Cara update

**Update dilakukan manual dari GitHub**, tanpa updater otomatis di dalam modul.

1. Unduh APK terbaru melalui [Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases).
2. Buka file APK. Pilih **Update / Perbarui** apabila installer Android menampilkannya.
3. Force-stop Instagram dan buka kembali setelah selesai.

**Catatan:** update langsung hanya bisa jika nama paket, kode versi dan sertifikat signing kompatibel. Beta pertama memakai debug key sementara milik GitHub Actions. Jika versi baru berbeda sertifikat, **uninstal aplikasi modul saja**, pasang versi baru dan aktifkan kembali scope LSPosed. **Jangan uninstal Instagram.** Agar update mendatang selalu bisa dilakukan di tempat, pengembang perlu menggunakan signing key permanen.

## Cara kerja

```text
   Tombol dan navigasi Instagram asli
                  ↓
        Hook navigasi LSPosed
                  ↓
       Renderer kapsul kaca
         ├─ Foto/UI: blur latar
         └─ Reels: PixelCopy terbatas
```

- **Navigasi:** tombol asli Instagram tetap dipakai. Urutan yang diinginkan adalah Home → Reels → DM → Profil → Search. Swipe isi halaman masih berbasis penyesuaian setelah perpindahan native terdeteksi.
- **Efek kaca:** lapisan blur kecil, refleksi tepi, dan animasi lensa aktif. Blur Reels diambil sekitar **dua kali per detik**; jika tidak memungkinkan, kaca kembali bening.
- **Visibilitas:** navbar disembunyikan dalam konteks tertentu untuk tidak menutupi konten Instagram.

## Masalah umum dan privasi

| Masalah | Yang bisa dicoba |
| :--- | :--- |
| Navbar tidak muncul | Periksa modul dan scope LSPosed; force-stop Instagram |
| Blur kadang hilang | Beberapa video atau elemen layar tidak bisa disampling; ada fallback bening |
| Video Reels macet | Nonaktifkan modul, jalankan Instagram lagi, kemudian laporkan langkah reproduksi |
| Layar menyisakan ruang hitam | Sertakan versi perangkat/aplikasi dan log yang sudah disamarkan |
| APK gagal diperbarui | Periksa signing; instal ulang **modul saja**, bukan Instagram |

Log teknis dibuat secara lokal di:

```text
Download/IGGlass-0.1-dev-YYYYMMDD-HHmmss.txt
```

Potongan gambar untuk blur diproses di memori, **tidak dimaksudkan untuk disimpan sebagai gambar atau diunggah ke server**. Modul tidak punya formulir login Instagram maupun endpoint telemetri terpisah. Periksa dan samarkan informasi pribadi sebelum membagikan log atau screenshot.

## Build dari source

Perlu **JDK 17**, **Gradle 8.9**, **Android SDK Platform/Build Tools 35**, dan **Xposed API 82** (khusus kompilasi) di `app/libs/api-82.jar`.

```bash
gradle --no-daemon :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Workflow lengkap ada di [build-apk.yml](.github/workflows/build-apk.yml).

## Kontribusi dan lisensi

Lu boleh mengusulkan perubahan melalui **Pull Request**. Namun, **@zaidanfaiz** yang memutuskan perubahan mana yang diterima di branch `main`; proses review perlu didukung pengaturan perlindungan branch GitHub.

Kode asli proyek menggunakan **[lisensi khusus source-available](LICENSE)**, bukan lisensi open-source yang mengizinkan distribusi bebas. Distribusi ulang proyek, APK modifikasi, dan penerbitan ulang membutuhkan izin. Jika repo dibuat public, GitHub tetap memungkinkan source dilihat, di-clone dan di-fork sesuai ketentuan platform; lisensi tidak bisa memblokir itu secara teknis.

Baca juga **[CONTRIBUTING.md](CONTRIBUTING.md)** · **[SECURITY.md](SECURITY.md)** · **[CHANGELOG.md](CHANGELOG.md)** · **[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)**.

---

<div align="center">

**Navigasi lebih bersih, pengalaman Instagram tetap familiar.**

<sub>Proyek komunitas independen. Tidak berafiliasi dengan Instagram/Meta, Apple, atau LSPosed. Atribusi komponen pihak ketiga tersedia di THIRD_PARTY_NOTICES.md.</sub>

</div>
