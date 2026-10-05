# TataskanPOS 🏪

[![Android Version](https://img.shields.io/badge/Android-Min%20SDK%2023%20%7C%20Target%20SDK%2036-brightgreen)](https://developer.android.com/)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-2.0.0%20KMP-blue)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.4.0%20(Code%209)-purple)](CHANGELOG.md)

**TataskanPOS** (derived from **Tatak** - *brand/seal* + **Scan** - *barcode scanning*) is a modern, offline-first Point of Sale (POS) and Inventory Management system designed specifically for small-to-medium retail businesses, kiosks, and sari-sari stores.

Built with **Jetpack Compose (Material 3)**, **Kotlin Multiplatform (KMP)**, **Room Database**, **CameraX ML Kit**, and **Google Drive Cloud Auto-Backup**, TataskanPOS delivers an ultra-fast, professional retail experience without requiring an active internet connection.

---

## ✨ Key Features

### 🛒 Smart POS & Checkout
*   **Reactive Cart Management:** Live subtotal, tax calculation, and promo voucher discounts.
*   **High-Speed Barcode & QR Scanner:** ML Kit on-device barcode scanner with continuous Bulk Mode.
*   **Digital Payment Engine:** GCash & Maya merchant QR codes with tabbed checkout modal and transaction payment tracking (`CASH` vs `DIGITAL`).
*   **Thermal PDF Receipts:** Instant thermal receipt generation, gallery export, and sharing.

### 📦 Inventory & Bulk Label Generator
*   **Product Catalog:** Categories, wholesale costs, selling prices, and stock tracking with low-stock alerts.
*   **Bulk Label Generator:** Custom Barcode (Code 128) and QR Code labels in `SMALL`, `MEDIUM`, or `LARGE` sizes with interactive live preview and A4 PDF sheet export.
*   **Select All / Deselect All Shortcut:** 1-tap bulk product selection for label printing.

### 📊 Business Analytics & Reports
*   **Visual Analytics:** 7-day Weekly Sales Trend bar graphs and Top Selling Products leaderboard.
*   **Itemized Sales Reports:** Export multi-page executive sales reports to PDF.

### ☁️ Google Drive Cloud Auto-Backup ("Anti-Disaster")
*   **Real Google Sign-In:** Linked personal Google Account via `play-services-auth`.
*   **Automatic Nightly Sync:** WorkManager 24-hour background worker flushes database WAL checkpoints (`PRAGMA wal_checkpoint(FULL)`) and uploads `.tataskan` ZIP archives to Google Drive.
*   **Safe Room Migrations (v19):** Explicit migration paths (`MIGRATION_17_18`, `MIGRATION_18_19`, `MIGRATION_17_19`) ensuring zero data loss on app updates.

### 🔑 Security & Recovery
*   **Backup PIN & Ticket Recovery:** Reset credentials using a 4-Digit Security Backup PIN or Facebook Support Ticket (`TR-XXXXXX` code with 1-tap Copy button).
*   **Master Admin Passcode:** Master code `102819` for admin bypass.
*   **Trial Licensing System:** 15-day trial countdown with ticket (`AX-`) and secret promo extensions (`NKMLVS` +10d, etc.).

### 🌍 Global Localization
*   **Dual Language Support:** Full English (`en`) and Tagalog/Filipino (`tl`) localization.
*   **Custom Regional Settings:** Flexible currency symbols (₱, $, €, £, ¥) and tax customization.

---

## 🛠️ Technical Stack & Architecture

*   **UI Framework:** [Jetpack Compose (Material 3)](https://developer.android.com/compose)
*   **Architecture:** MVVM + Kotlin Multiplatform (KMP `:shared` & `:app`)
*   **Navigation:** [Navigation 3](https://developer.android.com/guide/navigation) with `ListDetailSceneStrategy`
*   **Local Database:** [Room KMP (v19)](https://developer.android.com/training/data-storage/room)
*   **Cloud Sync:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) & [Google Drive AppData API](https://developers.google.com/drive)
*   **Vision / ML:** [ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) & [CameraX](https://developer.android.com/training/camerax)
*   **Image Loading:** [Coil](https://coil-kt.github.io/coil/)

---

## 📁 Repository Structure

```
TataskanPOS/
├── app/                  # Android application module (UI, ViewModels, Activities)
├── shared/               # KMP module (Entities, DAOs, Database, Localization)
├── APP_BLUEPRINT.md      # Exhaustive Master Technical Blueprint
├── CHANGELOG.md          # Version Release History (v1.0.0 -> v2.4.0)
├── README.md             # Project Overview & Setup Guide
└── build.gradle.kts      # Root Gradle build script
```

---

## 🚀 How to Run the Project

1. Clone or download the repository.
2. Open the project in **Android Studio (Ladybug 2024.2.1 or newer)**.
3. Allow Gradle sync to complete.
4. Select the `:app` configuration and target an Android device / Emulator (Min SDK 23, Target SDK 36).
5. Click **Run (`Shift + F10`)** or build signed release bundle via **Build -> Generate Signed App Bundle**.

---

*Developed with ❤️ for Solo Developers and Retail Merchants.*
