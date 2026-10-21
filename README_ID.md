# Instagram Liquid Glass — LSPosed

Modul **LSPosed tidak resmi** yang mengubah navbar bawah Instagram menjadi kapsul kaca melayang, dengan efek blur dan tombol navigasi asli Instagram tetap digunakan.

[README English](README.md) · [Download rilis](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases) · [Changelog](CHANGELOG.md) · [Laporkan bug](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues/new/choose)

> [!IMPORTANT]
> **Versi beta.** Modul ini **tidak sengaja dibatasi hanya untuk satu versi Instagram atau Android**. Ada kemungkinan berjalan di versi lain, tetapi belum diuji secara menyeluruh. Pengujian pengembang tercatat menggunakan **Android 16 (API 36) dan Instagram 452.0.0.48.64**. Proyek independen, bukan produk resmi Instagram, Meta, Apple, atau LSPosed.

## Fitur

- Navbar melayang berisi **Home, Reels, DM dan Profil**, dengan **Search terpisah** di lingkaran kaca sebelah kanan.
- Urutan navigasi: **Home → Reels → DM → Profil → Search**. Mendukung tap dan geser lensa kaca. Swipe pada isi halaman masih merupakan pengalihan berbasis navigasi asli Instagram, sehingga belum tentu selalu mulus.
- Tampilan frosted glass dengan blur, tepi kaca reflektif dan animasi lensa aktif.
- Blur Reels beta melalui pengambilan potongan gambar kecil menggunakan `PixelCopy`; jika tidak didukung, tampilannya kembali menjadi kaca bening.
- Navbar otomatis disembunyikan pada konteks tertentu, termasuk komentar Reels, chat DM terbuka, Story dan profil pengguna lain.
- Tidak perlu memasang Instagram modifikasi. Modul bekerja pada Instagram yang sudah terpasang melalui LSPosed.

## Kompatibilitas dan data perangkat pengujian

Modul bekerja dengan mengenali elemen navigasi pada aplikasi `com.instagram.android`, **tanpa daftar putih yang mengunci nomor versi Instagram**. Artinya, modul **mungkin berjalan di Android dan versi Instagram lain** selama struktur navigasinya kompatibel. Itu bukan jaminan semua versi akan bekerja sempurna.

| Komponen | Keterangan |
| --- | --- |
| Minimum pemasangan APK | **Android 10 (API 29)** sesuai deklarasi `minSdk`; bukan jaminan semua fitur berfungsi pada versi tersebut |
| Instagram | Paket aplikasi `com.instagram.android`, versi dapat berbeda |
| LSPosed | Wajib, dengan scope khusus Instagram |
| Root atau lingkungan yang mendukung LSPosed | Wajib |
| Android/Instagram lainnya | **Berpotensi kompatibel, belum diverifikasi** |
| iOS | Tidak didukung |

### Data perangkat yang digunakan untuk pengujian

Informasi ini berasal dari log diagnostik perangkat saat pengembangan, **bukan batasan instalasi**.

| Informasi | Data terverifikasi |
| --- | --- |
| Sistem operasi | **Android 16 / API 36** |
| Versi Instagram | **452.0.0.48.64** |
| Resolusi window/layar yang tercatat | **1440 × 3200 piksel** |
| Paket Instagram | `com.instagram.android` |
| Paket modul | `dev.zaidan.igglass` |
| Merek dan model HP | **Belum terekam di log** |
| Nama ROM / build | **Belum terekam di log** |

Pengujian tersebut tidak menutup kemungkinan modul berjalan di Android 10–15, Android yang lebih baru, atau versi Instagram lainnya. Kompatibilitas harus diuji pada masing-masing perangkat. Laporan dari perangkat lain sangat membantu.

## Instalasi dan update manual

1. Unduh **APK modul** dari halaman [GitHub Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases). Boleh periksa SHA-256 dengan `SHA256SUMS.txt`. Ini **bukan APK Instagram modifikasi**.
2. Instal APK pada perangkat Android dengan LSPosed.
3. Buka **LSPosed → Modules → Instagram Liquid Glass**, aktifkan dan pilih scope **Instagram (`com.instagram.android`)** saja.
4. **Force-stop Instagram**, kemudian buka kembali.
5. Coba Home, Reels, DM, Profil, Search, blur, dan komentar Reels.

### Cara update ketika ada rilis baru

**Update dilakukan manual melalui GitHub**, tanpa pembaruan otomatis di dalam modul.

1. Buka [GitHub Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases).
2. Unduh APK versi terbaru, lalu buka APK tersebut.
3. Jika pemasang Android menampilkan **Update / Perbarui**, pilih itu.
4. Force-stop dan buka kembali Instagram setelah update.

**Catatan tanda tangan APK:** Android hanya mengizinkan update di tempat jika nama paket, versi, dan sertifikat penandatanganan cocok. Beta `v0.1.0-beta.1` memakai debug key sementara dari GitHub Actions. Kalau build berikutnya menggunakan kunci berbeda, tombol **Update** akan gagal; nonaktifkan dan **uninstal aplikasi modulnya saja**, pasang APK baru, lalu aktifkan kembali di LSPosed. **Jangan hapus Instagram atau datanya.** Agar ke depannya update langsung bekerja konsisten, diperlukan kunci signing tetap milik pengelola.

## Batasan penting

Efeknya belum sama dengan *Liquid Glass* bawaan iOS. Blur pada video Reels menggunakan pendekatan sampling berkala (sekitar dua frame per detik), bukan pembiasan compositor real-time. Beberapa video, konten terlindungi, atau konfigurasi perangkat bisa menyebabkan efek blur tidak tersedia. Sistem akan memilih kaca bening daripada tampilan putih atau hitam saat pengambilan gambar gagal.

Swipe pada isi halaman juga belum mengubah adapter pager internal Instagram. Di beberapa transisi, halaman lain mungkin terlihat sesaat atau urutan swipe tidak berjalan persis seperti visual navbar.

Jika pemutaran Reels macet, gerakan terganggu, atau navbar hilang, **nonaktifkan modul di LSPosed dan force-stop Instagram**.

## Log, privasi, dan pelaporan

Log teknis dibuat secara lokal di:

```text
Download/IGGlass-0.1-dev-YYYYMMDD-HHmmss.txt
```

Log berisi informasi teknis seperti kondisi navigasi, ukuran View, dan kegagalan rendering. Potongan gambar yang diperlukan untuk efek blur diproses **dalam memori** dan tidak dimaksudkan untuk diunggah atau ditulis ke log. Modul ini tidak memiliki fitur login atau analitik jaringan sendiri.

Saat membuat GitHub Issue, cantumkan versi Instagram, Android, LSPosed, modul, langkah reproduksi, dan log yang **sudah diperiksa agar tidak mengandung data pribadi**.

## Build dari source

Diperlukan **JDK 17**, **Android SDK Platform/Build Tools 35**, **Gradle 8.9**, serta `app/libs/api-82.jar` (Xposed API 82, hanya untuk kompilasi).

```bash
gradle --no-daemon :app:assembleDebug
```

APK hasil build berada di `app/build/outputs/apk/debug/app-debug.apk`. Lihat [workflow build](.github/workflows/build-apk.yml) untuk proses lengkap.

## Kontribusi, pengelolaan kode, dan lisensi

Orang lain boleh **mengajukan perubahan** melalui GitHub Pull Request. Namun, **hanya pengelola yang berwenang menyetujui dan melakukan merge ke branch `main`**. Mengajukan PR tidak otomatis memberikan hak untuk menerbitkan APK resmi.

Aturan [LICENSE](LICENSE) menggunakan ketentuan **source-available untuk kontribusi**, bukan lisensi open-source MIT untuk seluruh proyek. Penggunaan ulang secara bebas, distribusi ulang APK modifikasi, pemiroran, dan penerbitan ulang kode asli membutuhkan izin tertulis pemilik hak cipta. Komponen shader pihak ketiga tetap memiliki lisensi MIT sendiri seperti dijelaskan di [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

**Catatan GitHub:** jika repo dibuat public, orang secara teknis tetap dapat mengunduh, meng-clone, dan melakukan fork; hal itu tidak dapat dinonaktifkan hanya dengan menulis larangan dalam lisensi. Hak yang diberikan GitHub melalui ketentuan platform tetap berlaku. Proteksi branch serta review pemilik dapat mengendalikan **merge ke `main`**, bukan mencegah seluruh penyalinan kode.

Proyek independen; tidak berafiliasi dengan Instagram, Meta, Apple, atau LSPosed. Lihat [README utama (English)](README.md), [CONTRIBUTING](CONTRIBUTING.md), dan [SECURITY](SECURITY.md).
