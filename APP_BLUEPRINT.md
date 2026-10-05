# TataskanPOS - Master Application Blueprint 🏪

This document is the official, comprehensive Master Blueprint for **TataskanPOS** (formerly SukiPOS). It documents every architectural decision, database entity, ViewModel responsibility, UI screen, utility function, permission, PDF rendering specification, security algorithm, and resilience mechanism across the entire codebase (`:app` and `:shared`).

---

## 🏷️ Brand Identity & Meaning

**Tataskan** is derived from a meaningful fusion of two core concepts:
- **Tatak** (Tagalog for *brand*, *mark*, *seal*, or *stamp* — representing business identity, official receipts, and established brand presence).
- **Scan** (Referring to high-speed barcode and QR code scanning for effortless POS transactions and inventory tracking).

Together, **Tataskan** means *stamping your mark on your business while seamlessly scanning every sale*.

---

## 🛠️ 1. Technical Stack & Architecture Overview

| Component / Layer | Technology | Specification & Detail |
| :--- | :--- | :--- |
| **UI Framework** | Jetpack Compose (Material 3) | Declarative UI, edge-to-edge layout, responsive typography & dynamic color schemes |
| **Architecture** | MVVM + Kotlin Multiplatform (KMP) | Clean separation: `:shared` (domain/data/KMP) & `:app` (Android UI/ViewModels) |
| **Navigation** | Navigation 3 | Type-safe route navigation (`NavDisplay`), backstack management & `ListDetailSceneStrategy` |
| **Database Engine** | Room KMP (Version 19) | Offline-first SQLite database with explicit migration paths (`MIGRATION_17_18`, `MIGRATION_18_19`, `MIGRATION_17_19`) |
| **Cloud Backup** | WorkManager & Google Drive | Anti-Disaster auto-backup uploading `.tataskan` ZIPs to Google Drive |
| **Preferences** | DataStore Preferences | Reactive async key-value settings, QR codes, and login session tracking |
| **Vision / AI** | ML Kit Barcode Scanning | High-speed on-device scanning for `FORMAT_ALL_FORMATS` (EAN-13, UPC-A, QR Code, Code 128) |
| **Camera Engine** | CameraX (`ImageAnalysis`) | Lifecycle-aware camera pipeline with orientation-aware image analysis |
| **PDF Engine** | Native Android `PdfDocument` | Thermal receipts, A4 label sheets, promo voucher sheets & multi-page sales reports |
| **Image Loading** | Coil (`AsyncImage`) | Crossfaded async image loading for store logo, GCash/Maya QRs, and product photo catalog |
| **Crash Resilience** | `GlobalCrashHandler` | Intercepts uncaught thread crashes, writes JSON reports, and launches recovery `ErrorActivity` |

---

## 📐 2. Database Schema Blueprint (`AppDatabase.kt`)

TataskanPOS operates an offline-first Room database (`tataskan_database`) version 19 with explicit migration paths (`MIGRATION_17_18`, `MIGRATION_18_19`, `MIGRATION_17_19`) managing 7 entities:

```
+-----------------------------------------------------------------------------------+
|                                  DATABASE SCHEMA                                  |
+-------------------+      +-------------------+      +-------------------------+   |
|       User        |      |      Product      |      |       Transaction       |   |
+-------------------+      +-------------------+      +-------------------------+   |
| id: Int (PK)      |      | id: Long (PK)     |      | id: Long (PK)           |   |
| username: String  |      | name: String      |      | total: Double           |   |
| password: String  |      | price: Double     |      | amountReceived: Double  |   |
| shopName: String  |      | cost: Double      |      | taxAmount: Double       |   |
| backupPin: String |      | category: String  |      | discountAmount: Double  |   |
+-------------------+      | stock: Int        |      | promoName: String?      |   |
                           | barcode: String?  |      | paymentMethod: String   |   |
                           | imageUri: String? |      | timestamp: Long         |   |
                           +-------------------+      +-------------------------+   |
                           +-------------------+                   |                |
                                                                   | 1:N            |
                                                                   v                |
+-------------------+      +-------------------+      +-------------------------+   |
|     Category      |      |       Promo       |      |     TransactionItem     |   |
+-------------------+      +-------------------+      +-------------------------+   |
| id: Int (PK)      |      | id: Int (PK)      |      | id: Long (PK)           |   |
| name: String      |      | name: String      |      | transactionId: Long     |   |
+-------------------+      | code: String      |      | productId: Long         |   |
                           | type: PromoType   |      | productName: String     |   |
                           | value: Double     |      | quantity: Int           |   |
                           | productId: Long?  |      | priceAtSale: Double     |   |
                           | usageLimit: Int?  |      | costAtSale: Double      |   |
                           | currentUsage: Int |      +-------------------------+   |
                           | isActive: Boolean |                                    |
                           +-------------------+                                    |
                                                                                    |
                           +---------------------------+                            |
                           |      StockAdjustment      |                            |
                           +---------------------------+                            |
                           | id: Long (PK)             |                            |
                           | productId: Long           |                            |
                           | productName: String       |                            |
                           | quantityChange: Int       |                            |
                           | reason: AdjustmentReason  |                            |
                           | notes: String             |                            |
                           | timestamp: Long           |                            |
                           +---------------------------+                            |
+-----------------------------------------------------------------------------------+
```

### Entity Fields & Data Types

1. **`User`** (`tableName = "users"`):
   - `id: Int` (AutoGenerate Primary Key)
   - `username: String` (Unique username)
   - `password: String` (Plaintext local protection)
   - `shopName: String` (Store title displayed on dashboard and receipts)
   - `backupPin: String` (Exactly 4 numeric digits for local password recovery)

2. **`Product`** (`tableName = "products"`):
   - `id: Long` (AutoGenerate Primary Key)
   - `name: String` (Product title)
   - `price: Double` (Retail selling price)
   - `cost: Double` (Unit wholesale cost for profit calculation)
   - `category: String` (Category grouping name)
   - `stock: Int` (Available quantity in inventory)
   - `barcode: String?` (Optional Barcode/QR code string)
   - `imageUri: String?` (URI to local photo or camera image)

3. **`Transaction`** (`tableName = "transactions"`):
   - `id: Long` (AutoGenerate Primary Key)
   - `total: Double` (Final grand total including tax minus discounts)
   - `amountReceived: Double` (Cash tendered by customer)
   - `taxAmount: Double` (Calculated tax component)
   - `discountAmount: Double` (Total promo discount applied)
   - `promoName: String?` (Name of applied promo voucher)
   - `timestamp: Long` (System epoch millis at sale completion)

4. **`TransactionItem`** (`tableName = "transaction_items"`):
   - `id: Long` (AutoGenerate Primary Key)
   - `transactionId: Long` (Foreign Key link to `Transaction`)
   - `productId: Long` (Source Product ID)
   - `productName: String` (Snapshot product title at time of sale)
   - `quantity: Int` (Units purchased)
   - `priceAtSale: Double` (Snapshot price at time of sale)
   - `costAtSale: Double` (Snapshot unit cost at time of sale)

5. **`Category`** (`tableName = "categories"`):
   - `id: Int` (AutoGenerate Primary Key)
   - `name: String` (Unique category name)

6. **`Promo`** (`tableName = "promos"`):
   - `id: Int` (AutoGenerate Primary Key)
   - `name: String` (Voucher title)
   - `code: String` (Scannable/Enterable promo code string)
   - `type: PromoType` (`PERCENTAGE_TOTAL`, `FIXED_TOTAL`, `PERCENTAGE_PRODUCT`, `FIXED_PRODUCT`)
   - `value: Double` (% discount rate or fixed currency amount)
   - `productId: Long?` (Target product ID if product-specific)
   - `usageLimit: Int?` (Max allowed redemptions)
   - `currentUsage: Int` (Current redemption count)
   - `isActive: Boolean` (Active toggle status)

7. **`StockAdjustment`** (`tableName = "stock_adjustments"`):
   - `id: Long` (AutoGenerate Primary Key)
   - `productId: Long` (Product ID)
   - `productName: String` (Product name)
   - `quantityChange: Int` (Positive for restock, negative for deduction)
   - `reason: AdjustmentReason` (`SOLD`, `RESTOCKED`, `DAMAGED`, `EXPIRED`, `LOST`, `MANUAL`)
   - `notes: String` (Adjustment note)
   - `timestamp: Long` (System epoch millis)

---

## 🚀 3. Comprehensive Module Breakdown

### Module 1: Authentication, Security & Licensing
- **Registration**: Register screen with Shop Name, Username, Password, Confirm Password, and 4-Digit Security PIN (`RegisterScreen.kt`).
- **Single Account Enforcer**: Prevents registering secondary accounts on the same device (`repository.getUserCount() > 0`).
- **Session Manager**: Persists active username and last activity timestamp. Automatically logs user out after 1 hour of inactivity checked on `ON_RESUME`.
- **Password Recovery**:
  - **PIN Reset**: Resets password instantly if 4-digit backup PIN matches (`resetPasswordWithBackupPin`). Includes direct support link (*"Forgot Backup PIN? Request Support via Facebook Ticket"*). Triggers green success confirmation dialog upon completion.
  - **Facebook Ticket Reset**: Generates 6-digit `TR-XXXXXX` code valid for 5 minutes with 1-tap Copy Code button. Verified using `(ticketCode + 101928) % 1000000` or Master Passcode **`102819`**. Triggers green success confirmation dialog upon completion.
- **15-Day Trial System**:
  - Trial countdown timer checking remaining days against `trialStartTimestamp` and `extraTrialDays`.
  - **Extension Ticket**: Generates `AX-XXXXXX` code valid for 5 minutes. Verified using `(ticketCode + 281910) % 1000000` (+15 days). Triggers green success confirmation dialog upon completion.
  - **Extension Promo Codes**: Secret redemption codes (`NKMLVS` +10d, `HCSKANPOS` +15d, `HPYNWTASKANPOS` +10d, `PROMO5SK`/`SUKI5DAYS`/`TRIAL5EXT`/`BONUS5DAYS`/`FREE5SKPOS` +5d) recorded via `DeviceRedemptionManager`. Triggers green success confirmation dialog upon completion.
  - **Trial Lock Screen (`TrialExpiredScreen`)**: Full-screen lockout when trial expires, offering ticket request, promo code input, or Facebook/Email support links.

### Module 2: POS Billing Engine & Cart Management
- **Cart State (`PosViewModel.kt`)**: Reactive `_cartItemsMap` holding `CartItem(product, quantity)`.
- **Stock Validation**: Prevents adding out-of-stock items (`stock <= 0`) or exceeding available quantity (`quantity > product.stock`).
- **Quick Adjusters & Undo**: Quantity increment/decrement buttons, item swipe/delete with a 1-tap Undo option (`undoRemoveFromCart`).
- **Promo Discount Resolution**:
  - `PERCENTAGE_TOTAL`: `subtotal * (value / 100.0)`
  - `FIXED_TOTAL`: `value.coerceAtMost(subtotal)`
  - `PERCENTAGE_PRODUCT`: `targetItem.subtotal * (value / 100.0)`
  - `FIXED_PRODUCT`: `(value * targetItem.quantity).coerceAtMost(targetItem.subtotal)`
- **Checkout Screen (`CheckoutScreen.kt`)**:
  - Live cash tendered input with decimal keyboard support.
  - Suggestive bill chips (e.g. ₱20, ₱50, ₱100, ₱500, ₱1000 or exact total).
  - Live Change calculation: `(amountReceived - grandTotal)`.
  - Tax calculation: `taxableAmount * (taxPercentage / 100)`.
  - Confirmation dialog displaying full breakdown before completing sale.
- **Atomic Database Transaction (`insertFullTransaction`)**: Executed inside a Room `@Transaction` block that inserts transaction record, transaction items, and reduces product stock in SQLite.

### Module 3: Digital Receipts & Thermal PDF Export
- **Receipt Screen (`ReceiptScreen.kt`)**:
  - Displays store logo (Coil `AsyncImage`), store title, address, telephone, receipt number, date/time, itemized table, subtotal, discount, tax, grand total, cash tendered, change, and footer message.
- **PDF Generator (`PdfUtils.generateReceiptPdf`)**:
  - Creates 300x600 pt thermal receipt PDF document.
  - Draws store headers, dividers, receipt info, item table, totals, and footer text using Android `Canvas` and `Paint`.
  - Exports directly to `MediaStore.Downloads` (`Environment.DIRECTORY_DOWNLOADS + "/SukiPos"`).
- **Share Intent**: Android `Intent.ACTION_SEND` with `application/pdf` MIME type and `FLAG_GRANT_READ_URI_PERMISSION`.

### Module 4: Inventory & Catalog Management
- **Catalog List (`ProductListPane.kt`)**:
  - Real-time search query filtering by product title or barcode.
  - Category filter chips dynamically loaded from `CategoryDao`.
  - Low Stock toggle filter (shows items with stock in `1..5`).
  - Quick stock adjustment buttons (+ / -) on product cards auto-recording a `StockAdjustment` with reasons (`RESTOCKED`, `MANUAL`).
- **Add / Edit Pane (`ProductAddEditPane.kt`)**:
  - Product photo picker supporting Camera capture (`FileProvider` temp file) or Gallery selection (`ActivityResultContracts.GetContent`).
  - Product Name, Selling Price, Unit Cost, Category (auto-complete dropdown with auto-creation of new categories in `CategoryDao`), Stock Level, and Barcode.
  - Barcode auto-generator button (`generateRandomBarcodeString`).
- **Product Details (`ProductDetailPane.kt`)**:
  - Displays high-res product photo, price, cost, profit margin (`price - cost`), stock level, and scannable barcode.
  - Downloads individual Barcode (Code 128) or QR Code image directly to device Gallery (`BarcodeUtils.saveBitmapToGallery`).
  - Stock adjustment history timeline.

### Module 5: Bulk Label Generator
- **Bulk Selection Screen (`LabelGeneratorScreen.kt`)**:
  - Select multiple inventory products and set label quantities for each.
  - Customization controls:
    - Label Size: `SMALL` (5/row), `MEDIUM` (3/row), `LARGE` (2/row).
    - Code Type: `AUTO`, `BARCODE` (Code 128), `QR`.
    - Toggles: `Show Product Name`, `Show Price`.
- **Label Sheet PDF (`PdfUtils.generateLabelSheetPdf`)**:
  - Creates A4 printable PDF document (595x842 pt).
  - Renders grid of label cards with dashed outlines, ZXing vector barcode/QR bitmaps, product titles, and prices.

### Module 6: Barcode & QR Code Scanner
- **CameraX ML Kit Scanner (`ScannerScreen.kt`, `BarcodeScanner.kt`)**:
  - Uses CameraX `ImageAnalysis` analyzer feeding camera frames to ML Kit `BarcodeScanning` (`FORMAT_ALL_FORMATS`).
  - Bulk / Continuous Scan Mode: Allows scanning multiple codes sequentially without closing camera (`defaultBulkScan` setting).
  - Auto-Detection Resolution:
    - Matches Barcode to Product -> adds product to POS cart.
    - Matches Barcode to Promo -> applies promo discount voucher to cart.

### Module 7: Promotions & Vouchers
- **Promo Manager (`PromoManagementScreen.kt`, `PromoViewModel.kt`)**:
  - Create and manage percentage or fixed discounts for cart totals or individual products.
  - Tracks redemption limits vs. current usage count.
  - Auto-generates scannable promo codes (`PROMO_XXXXXX`).
- **Export Options**:
  - Save voucher image card (400x400 JPG) to Gallery (`savePromoToGallery`).
  - Export printable A4 Promo Sheet PDF (`generatePromoLabelsPdf`).

### Module 8: Business Reports & Sales Analytics
- **Dashboard & Reporting Metrics (`ReportViewModel.kt`, `ReportingScreen.kt`)**:
  - Today's Revenue, Today's Profit, Today's Transaction Count.
  - Monthly Revenue, Monthly Profit, Monthly Transaction Count.
  - Low Stock Count (< 5) & Out-of-Stock Count (<= 0).
  - Total Product Count & Total Inventory Valuation (`cost * stock`).
  - Best-selling product title and units sold.
- **Analytics Charts**:
  - 7-day Weekly Sales Trend bar graph (Daily Revenue & Profit breakdown).
  - Top 5 Selling Products ranking leaderboard.
  - Recent Transaction History list with receipt inspector.
- **Sales Report PDF (`generateSalesReportPdf`)**:
  - Generates multi-page business sales report PDF containing executive summary, weekly trend table, top products leaderboard, and itemized transaction history.

### Module 9: App Settings & Local Database Backup Engine
- **Profile & Preferences (`SettingsScreen.kt`, `SettingsViewModel.kt`)**:
  - Business Profile: Store Name, Address, Phone Number, Store Logo.
  - Receipts & Tax: Tax Percentage, Enable/Disable Receipts, Receipt Footer.
  - Regional: Currency Symbol (₱, $, €, etc.), Language selection (`en` English, `tl` Tagalog).
  - Label Defaults: Show Name and Show Price default toggles.
- **Unified Backup Engine & Google Cloud Sync (`BackupRepository.kt`, `GoogleDriveBackupManager.kt`)**:
  - Executes `PRAGMA wal_checkpoint(FULL)` to commit WAL changes to disk.
  - Creates compressed ZIP archive (`.sukipos`) containing `databases`, `shared_prefs`, and `files` directories.
  - Integrated real Google Sign-In intent account picker (`play-services-auth`) allowing merchants to link their personal Google Account and upload backups to Google Drive `appDataFolder`.
  - Internal backup storage (`files/backups/`) and System File Picker export for external saving.
  - Import & Restore engine: Validates database, closes active connections, extracts ZIP entries, writes `restore_pending.flag`, and forces session re-login upon app restart.
- **Demo Data Seeder (`seedDemoData`)**:
  - Seeds store profile ("Small Mall Mart"), 12% tax rate, and 30+ realistic demo products across Grocery, Bakery, Dairy, Electronics, Beverages, and Snacks with dynamic 7-day daily sales peaks.
- **Reset All Store Data (`resetBusinessData`)**:
  - Clears all Room business tables (`products`, `transactions`, `transaction_items`, `categories`, `promos`, `stock_adjustments`), clears QR code URIs, resets tutorial state (`updateTutorialComplete(false)`), and retains user account credentials (`User` table).
- **Factory Reset / Wipe Data (`ClearDbActivity.kt`)**:
  - Clears all Room tables, shared preferences, backup files, and terminates process.

### Module 10: Interactive Guided Tutorial System
- **24-Step Onboarding Walkthrough (`TutorialViewModel.kt`, `GuidedTutorial.kt`)**:
  - Step-by-step tour covering POS, Camera Scanning, Checkout, Receipts, Inventory Creation, Auto-Barcode, Label Printing, Promos, Reports, Settings, and Feedback.
- **Target Spotlight Overlay (`GuidedTutorialOverlay`)**:
  - Dynamic element highlighting (`Modifier.tutorialTarget`) with dark backdrop, spotlight cutout, step tooltips, Next/Skip buttons, and back arrow instructions.

### Module 11: Crash Resilience & Error Recovery
- **Uncaught Exception Interceptor (`GlobalCrashHandler.kt`)**:
  - Intercepts all uncaught main or background thread exceptions.
- **Crash Logger (`ErrorReporter.kt`)**:
  - Persists `CrashReport` (exception type, message, stack trace, thread, timestamp) to internal JSON storage.
- **Error Recovery Activity (`ErrorActivity.kt`)**:
  - Displays user-friendly crash notice screen with expandable stack trace, copy to clipboard, and 1-tap app restart button (`MainActivity`).

---

## 🗺️ 4. Navigation Blueprint (`TataskanNavKey.kt`)

```
                          +-------------------+
                          |      Splash       |
                          +-------------------+
                                    |
                     +--------------+--------------+
                     |                             |
                     v                             v
           +-------------------+         +-------------------+
           |       Login       | <-----> |     Register      |
           +-------------------+         +-------------------+
                     |                             |
                     +--------------+--------------+
                                    |
                                    v
                          +-------------------+
                          |  Home (Dashboard) |
                          +-------------------+
                                    |
   +----------------+---------------+----------------+----------------+
   |                |                                |                |
   v                v                                v                v
+------+     +--------------+                   +----------+     +----------+
| POS  |     | Inventory    |                   | Reports  |     |   More   |
+------+     +--------------+                   +----------+     +----------+
   |                |                                                 |
   |-- Search       |-- Product Detail                                |-- Promos
   |-- Scanner      |-- Product Add/Edit                              |-- Labels
   |-- Checkout     |-- Label Generator                               |-- Settings
   |-- Receipt                                                        |-- Feedback
```

### Full Route Table

| Route / NavKey | Screen Composable | Key Capabilities |
| :--- | :--- | :--- |
| `TataskanNavKey.Splash` | Splash Screen | Loads auth & settings state |
| `TataskanNavKey.Login` | `LoginScreen` | Username/Password login, PIN/Ticket reset |
| `TataskanNavKey.Register` | `RegisterScreen` | Account registration & PIN setup |
| `TataskanNavKey.Home` | `HomeScreen` | Dashboard, sales overview, stock alerts |
| `TataskanNavKey.Pos` | `PosCartScreen` / `PosTabletScreen` | POS cart, search, scan, item quantifiers |
| `TataskanNavKey.ProductList` | `ProductListPane` | Catalog list, category chips, low stock filter |
| `TataskanNavKey.ProductSearch` | `ProductListPane` | Fast product search for cart addition |
| `TataskanNavKey.LabelGenerator` | `LabelGeneratorScreen` | Bulk label selection & PDF sheet creation |
| `TataskanNavKey.ProductDetail(id)` | `ProductDetailPane` | Product detail, barcode/QR gallery download |
| `TataskanNavKey.ProductAddEdit(id)`| `ProductAddEditPane` | Product form, photo capture, auto-barcode |
| `TataskanNavKey.Checkout` | `CheckoutScreen` | Cash tendered, suggestion chips, change, tax |
| `TataskanNavKey.Receipt(txId)` | `ReceiptScreen` | Digital receipt, thermal PDF export & share |
| `TataskanNavKey.Scanner` | `ScannerScreen` | CameraX ML Kit barcode scanner (Bulk Mode) |
| `TataskanNavKey.Reports` | `ReportingScreen` | Business metrics, sales charts & PDF reports |
| `TataskanNavKey.Feedback` | `FeedbackScreen` | Star rating & feedback submission |
| `TataskanNavKey.Settings` | `SettingsScreen` | Store profile, tax, currency, language, backups |
| `TataskanNavKey.PromoList` | `PromoManagementScreen` | Promo vouchers, image export & PDF promo sheet |
| `TataskanNavKey.More` | `MoreScreen` | Secondary navigation hub |

---

## 🌐 5. System Permissions & File Paths

### System Permissions (`AndroidManifest.xml`)
- `android.permission.INTERNET` - Optional network lookup for images / support links.
- `android.permission.CAMERA` - CameraX scanner & product photo capture.
- `android.permission.VIBRATE` - Haptic feedback on barcode scan.
- `android.permission.READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (API <= 28/32).
- `android.permission.READ_MEDIA_IMAGES` (API 33+).

### Provider & Directories
- **FileProvider Authority**: `${applicationId}.fileprovider` mapped to `@xml/file_paths`.
- **Public Exports**: `Environment.DIRECTORY_DOWNLOADS + "/Tataskan"` (PDF Receipts, Reports, Labels) and `Environment.DIRECTORY_PICTURES + "/Tataskan"` (Barcode/QR PNGs, Promo Cards).
- **Internal Backups**: `context.filesDir/backups/*.tataskan`.

---

## 🌐 6. Localization Map (`Strings.kt`)

TataskanPOS includes centralized strings in `Strings.kt` supporting:
- **`en` (English)** - Primary fallback language.
- **`tl` (Tagalog/Filipino)** - Full translation across all 24 tutorial steps, POS, Inventory, Settings, Reports, Dialogs, and Error notices.

---

*Master Blueprint Verified & Maintained for TataskanPOS Architecture.*
