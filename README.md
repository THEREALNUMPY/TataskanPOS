# TataskanPOS 🏪

Welcome to **TataskanPOS**! I built this application as a lightweight, offline-first Point of Sale (POS) and inventory management solution tailored for small retail shops, sari-sari stores, kiosks, and local businesses.

---

## 📱 Features

### 🛒 Point of Sale & Checkout
* **Fast Cart Billing:** Add items quickly, apply percentage or fixed discounts, and compute tax automatically.
* **Camera Barcode Scanner:** Continuous ML Kit barcode and QR code scanner powered by CameraX.
* **Digital Payment Support:** Accept GCash or Maya payments with merchant QR code modal displays during checkout.
* **Thermal Receipts:** Generate digital receipts and export printable thermal PDFs.

### 📦 Inventory & Barcode Labels
* **Product Catalog:** Manage products by category, unit cost, selling price, and stock levels.
* **Bulk Label Sheet Generator:** Print customized barcode or QR code label sheets in Small, Medium, or Large sizes as PDF files.
* **Stock Tracking:** Visual alerts for low stock items (< 5) and out-of-stock products.

### 📊 Reports & Sales Analytics
* **Sales Overview:** Track daily revenue, profit margins, and total transactions.
* **Charts & Analytics:** View 7-day sales trend graphs and best-selling product leaderboards.
* **Multi-Page PDF Export:** Download itemized sales reports.

### ☁️ Automatic Cloud Backups
* **Google Drive Sync:** Link your Google account for automatic background database backups to your personal Google Drive.
* **Offline-First:** Works completely offline without needing an active internet connection for daily sales.

---

## 🛠️ Built With

* **Language:** Kotlin
* **UI:** Jetpack Compose & Material 3
* **Database:** Room (SQLite)
* **Scanner:** CameraX & Google ML Kit Barcode API
* **Background Tasks:** WorkManager
* **Cloud Sync:** Google Drive AppData API

---

## 📄 License

This project is licensed under the [Apache 2.0 License](LICENSE).
