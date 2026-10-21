# Instagram Liquid Glass — LSPosed

Modul **LSPosed tidak resmi** yang mengubah navbar bawah Instagram menjadi kapsul kaca melayang, dengan efek blur dan tombol navigasi asli Instagram tetap digunakan.

[README English](README.md) · [Download rilis](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases) · [Changelog](CHANGELOG.md) · [Laporkan bug](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/issues/new/choose)

> [!IMPORTANT]
> **Versi beta.** Target utama: **Instagram 452.0.0.48.64 di Android 16**. Belum diverifikasi pada versi Instagram/Android lain. Proyek ini tidak berafiliasi dengan Instagram, Meta, Apple, atau pengembang LSPosed.

## Fitur

- Navbar melayang berisi **Home, Reels, DM dan Profil**, dengan **Search terpisah** di lingkaran kaca sebelah kanan.
- Urutan navigasi: **Home → Reels → DM → Profil → Search**. Mendukung tap dan geser lensa kaca. Swipe pada isi halaman masih merupakan pengalihan berbasis navigasi asli Instagram, sehingga belum tentu selalu mulus.
- Tampilan frosted glass dengan blur, tepi kaca reflektif dan animasi lensa aktif.
- Blur Reels beta melalui pengambilan potongan gambar kecil menggunakan `PixelCopy`; jika tidak didukung, tampilannya kembali menjadi kaca bening.
- Navbar otomatis disembunyikan pada konteks tertentu, termasuk komentar Reels, chat DM terbuka, Story dan profil pengguna lain.
- Tidak perlu memasang Instagram modifikasi. Modul bekerja pada Instagram yang sudah terpasang melalui LSPosed.

## Persyaratan

| Komponen | Status |
| --- | --- |
| Instagram 452.0.0.48.64 | Target utama |
| Android 16 | Lingkungan yang diuji selama pengembangan |
| LSPosed | Wajib |
| Root atau lingkungan yang mendukung LSPosed | Wajib |
| Android lain atau Instagram versi baru | Belum diverifikasi |

## Cara memasang

1. Unduh **APK modul** dari halaman [Releases](https://github.com/zaidanfaiz/Instagram-LiquidGlass-LSPosed/releases), beserta `SHA256SUMS.txt` jika ingin memeriksa checksum.
2. Instal APK modul pada perangkat yang sudah menjalankan LSPosed.
3. Buka **LSPosed → Modules → Instagram Liquid Glass**, aktifkan modul, dan pilih scope **Instagram (`com.instagram.android`)** saja.
4. **Paksa berhenti (force-stop) Instagram**, lalu buka kembali.
5. Uji Home, Reels, DM, Profil, Search, scrolling, dan buka/tutup komentar Reels.

**Perhatian:** beta pertama ditandatangani dengan debug key dari GitHub Actions. Jika pemasangan versi baru gagal karena tanda tangan berbeda, nonaktifkan dan **hapus aplikasi modulnya saja** sebelum instal ulang. Jangan menghapus Instagram atau datanya hanya karena masalah tanda tangan modul.

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

## Kredit dan lisensi

Proyek komunitas independen, bukan produk resmi Instagram. Penggunaan nama merek dan istilah iOS hanya untuk menjelaskan kompatibilitas/inspirasi desain. Atribusi komponen shader pihak ketiga tersedia di [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

**Kode asli proyek ini belum memiliki lisensi distribusi ulang yang dipilih.** Lisensi proyek menyeluruh akan ditetapkan dengan persetujuan pemilik repo. Lihat [README utama berbahasa Inggris](README.md) untuk informasi selengkapnya.
