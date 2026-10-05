# TataskanPOS - Version Changelog & Release History 📜

All notable changes, features, security upgrades, database schema migrations, and UI refinements for **TataskanPOS** are documented in this file.

---

## 🚀 Version 2.4.1 (Version Code 10) - *Current Version*
*Release Date: October 2026*

### 📊 Analytics & UI Polish
- **Sales Bar Chart Baseline Fix**: Constrained `SalesBarChart` container height in `ReportingScreen.kt` so peak 100% bars scale neatly within the chart area without extending below the 0-baseline or overlapping X-axis day labels.
- **Google Account Selection Fallback**: Updated `googleSignInLauncher` in `SettingsScreen.kt` to extract selected Google email addresses from system intents on standalone APKs without SHA-1 developer errors.
- **Single-Line Input Label Formatting**: Fixed vertical line wrapping on floating text input labels ("4-Digit Backup PIN" and "Admin Verification Code") in `RegisterScreen.kt` and `LoginScreen.kt`.

---

## 📦 Version 2.4.0 (Version Code 9)
*Release Date: October 2026*

### 🏷️ Rebranding & Identity
- **Official Branding Update**: Renamed application to **TataskanPOS** (derived from **Tatak** - *mark/brand/seal* + **Scan** - *barcode/QR scanning*).
- **Gradle & IDE Project Label**: Updated `settings.gradle.kts` (`rootProject.name = "Tataskan"`), `.idea/.name`, and `strings.xml`.

### 🛡️ Safe Database Migrations (Anti-Data Loss)
- **Room Database Version 19**: Incremented database version to 19.
- **Zero Destructive Fallback**: Removed `.fallbackToDestructiveMigration(true)` to protect merchant databases against automatic Google Play updates.
- **Explicit Migration Paths**: Added `MIGRATION_17_18`, `MIGRATION_18_19`, and `MIGRATION_17_19` (`ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'`).

### 💳 GCash & Maya Digital Payment Engine
- **Transaction Payment Method**: Added `paymentMethod` field (`"CASH"` vs `"DIGITAL"`) recorded on transactions, receipts, and sales reports.
- **Merchant QR Code Uploads**: Added photo uploaders in Settings for GCash and Maya QR code images, saved persistently to local files (`gcash_qr.jpg`, `maya_qr.jpg`).
- **Tabbed Merchant QR Modal**: Checkout screen payment selector with tabbed modal (**GCash QR** vs **Maya QR**) displaying high-resolution QR codes (`ContentScale.Fit`) for customer checkout.

### ☁️ Google Drive Cloud Auto-Backup ("Anti-Disaster")
- **Real Google Sign-In**: Integrated `play-services-auth` for Google account linking with Drive AppData scope (`https://www.googleapis.com/auth/drive.appdata`).
- **Background WorkManager Sync**: Periodic 24-hour worker (`DriveBackupWorker`) flushing database WAL checkpoints (`PRAGMA wal_checkpoint(FULL)`) and uploading `.sukipos` ZIP backups to Google Drive.
- **Settings UI**: Link/Unlink Google Account status, last cloud sync timestamp, and 1-tap "Sync Now" button with polished button layout.

### 🧹 Maintenance, Demo Data & Store Reset
- **Reset Store Data Action**: Added `resetBusinessData` clearing products, transactions, categories, promos, QR URIs, and resetting tutorial tour while preserving user account credentials.
- **Dynamic Sales Peaks in Demo Data**: Refactored `seedDemoData` to generate 20+ transactions across the past 7 days with realistic daily sales peaks (weekend peaks ~₱6,000, midweek peaks ~₱3,400, quiet Mondays ~₱680) and mixed payment methods.

### 🏷️ Bulk Label Generator Overhaul
- **Segmented Code Type**: Barcode (`BarChart`) vs QR Code (`QrCode`) chip selector.
- **Interactive Mini Label Preview**: Real-time card showing simulated product title, scannable vector barcode/QR icon, and price tag scaling with label size (`SMALL`, `MEDIUM`, `LARGE`).
- **Select All / Deselect All**: 1-tap shortcut to check or clear all filtered products.
- **Rich Product Cards**: Category badges, retail price tags, and scannable barcode badges.

### 🔑 Security & Recovery Enhancements
- **Master Admin Code (`102819`)**: Updated master verification passcode to `102819`.
- **Restored Secret Promo (`NKMLVS`)**: Restored +10 days trial extension promo code.
- **1-Tap Ticket Copy**: Added "Copy Code" button copying reset ticket string (`TR-XXXXXX`) directly to system clipboard.
- **Success Confirmation Dialogs**: Added green CheckCircle success alerts for password resets and trial extensions.

### 🎨 UI & Layout Fixes
- **Eliminated Spacing Gaps**: Fixed double-padding in nested Scaffolds (`SettingsScreen`, `ReportingScreen`, `ProductAddEditPane`) and unified Scaffold background container colors.
- **Single-Line Input Labels**: Fixed vertical line wrapping on floating text labels ("4-Digit Backup PIN" and "Admin Verification Code").
- **Cleaned Dependencies**: Removed unused `play-services-location` dependency to eliminate reflection warning logs during app startup.

---

## 📦 Version 2.3.0 (Version Code 8)
*Release Date: October 2026*

### Summary
- Incremental feature release incorporating Google Drive Cloud Sync, GCash/Maya Digital Payments, and Label Generator overhaul.

---

## 📦 Version 2.2.0 (Version Code 7)
*Release Date: October 2026*

### Summary
- Prepared safe database migrations, digital payments, Google Drive auto-backups, and rebranding infrastructure.

---

## 📦 Version 2.1.1 (Version Code 6)
*Release Date: September 2026*

### Features & Fixes
- Added 24-step guided onboarding tutorial overlay (`GuidedTutorialOverlay`).
- Added Trial Extension Ticket system (`AX-XXXXXX`) and Facebook Support ticket verification (`TR-XXXXXX`).
- Added secret promo code redemption for trial days (+5d, +10d, +15d).
- Added global crash handler (`GlobalCrashHandler`) and error activity (`ErrorActivity`).

---

## 📦 Version 2.0.0 (Version Code 5)
*Release Date: August 2026*

### Features & Fixes
- KMP `:shared` module architecture with Room KMP database (Version 17/18).
- Navigation 3 routing with `SukiPosNavKey` and `ListDetailSceneStrategy` for adaptive tablet dual-panes (`PosTabletScreen`).
- CameraX ML Kit Barcode Scanning with continuous Bulk Scan Mode.
- Full database ZIP backup and restore engine (`BackupRepository`).

---

## 📦 Version 1.1.0 (Version Code 2)
*Release Date: July 2026*

### Features & Fixes
- Weekly Sales Trend bar charts and Top Products leaderboard.
- Thermal receipt PDF generation (`PdfUtils`) and share intents.
- Tagalog (`tl`) localization in `Strings.kt`.

---

## 📦 Version 1.0.0 (Version Code 1)
*Release Date: June 2026*

### Features & Fixes
- Initial offline POS release with Product Catalog, Cart, Checkout, and Room persistence.
