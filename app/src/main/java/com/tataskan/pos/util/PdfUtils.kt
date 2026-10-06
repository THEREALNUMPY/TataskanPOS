package com.tataskan.pos.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tataskan.pos.data.relation.TransactionWithItems
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.ui.report.DailySales
import com.tataskan.pos.ui.report.TopProduct
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfUtils {

    private fun saveToPublicDownloads(context: Context, fileName: String, block: (OutputStream) -> Unit): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, if (fileName.endsWith(".pdf")) "application/pdf" else "text/comma-separated-values")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Tataskan")
            }
        }

        val resolver = context.contentResolver
        // Use MediaStore.Downloads for API 29+, fallback to Files for older
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    block(stream)
                }
                return it
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri?.let { resolver.delete(it, null, null) }
        }
        return null
    }

    fun generateReceiptPdf(
        context: Context,
        transaction: TransactionWithItems,
        storeName: String,
        storeAddress: String,
        phoneNumber: String,
        currencySymbol: String,
        footer: String
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        
        var y = 40f
        
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(storeName, 150f, y, paint)
        y += 20f
        
        paint.isFakeBoldText = false
        paint.textSize = 10f
        if (storeAddress.isNotBlank()) {
            canvas.drawText(storeAddress, 150f, y, paint)
            y += 15f
        }
        if (phoneNumber.isNotBlank()) {
            canvas.drawText("Tel: $phoneNumber", 150f, y, paint)
            y += 15f
        }
        
        y += 10f
        paint.strokeWidth = 1f
        canvas.drawLine(20f, y, 280f, y, paint)
        y += 20f
        
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Receipt #: ${transaction.transaction.id}", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(transaction.transaction.timestamp))
        canvas.drawText(date, 280f, y, paint)
        y += 25f
        
        paint.textAlign = Paint.Align.LEFT
        transaction.items.forEach { item ->
            canvas.drawText("${item.quantity}x ${item.productName}", 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyUtils.formatCurrency(item.priceAtSale * item.quantity, currencySymbol), 280f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 15f
        }
        
        y += 10f
        canvas.drawLine(20f, y, 280f, y, paint)
        y += 20f
        
        paint.textAlign = Paint.Align.LEFT
        val subtotalBeforeTaxAndDisc = transaction.items.sumOf { it.priceAtSale * it.quantity }
        
        if (transaction.transaction.discountAmount > 0) {
            canvas.drawText("Subtotal:", 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyUtils.formatCurrency(subtotalBeforeTaxAndDisc, currencySymbol), 280f, y, paint)
            y += 15f
            
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Discount (${transaction.transaction.promoName ?: "Promo"}):", 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-${CurrencyUtils.formatCurrency(transaction.transaction.discountAmount, currencySymbol)}", 280f, y, paint)
            y += 15f
        } else {
            canvas.drawText("Subtotal:", 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyUtils.formatCurrency(transaction.transaction.total - transaction.transaction.taxAmount, currencySymbol), 280f, y, paint)
            y += 15f
        }
        
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Tax:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatCurrency(transaction.transaction.taxAmount, currencySymbol), 280f, y, paint)
        y += 15f
        
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatCurrency(transaction.transaction.total, currencySymbol), 280f, y, paint)
        y += 15f

        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        val payMethodLabel = if (transaction.transaction.paymentMethod == "DIGITAL") "Digital (QR)" else "Cash"
        canvas.drawText("Payment:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(payMethodLabel, 280f, y, paint)
        y += 15f

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Received:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatCurrency(transaction.transaction.amountReceived, currencySymbol), 280f, y, paint)
        y += 15f

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Change:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        val changeAmount = (transaction.transaction.amountReceived - transaction.transaction.total).coerceAtLeast(0.0)
        canvas.drawText(CurrencyUtils.formatCurrency(changeAmount, currencySymbol), 280f, y, paint)
        y += 25f
        
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(footer, 150f, y, paint)
        
        pdfDocument.finishPage(page)
        
        val fileName = "Receipt_${transaction.transaction.id}.pdf"
        return saveToPublicDownloads(context, fileName) { stream ->
            pdfDocument.writeTo(stream)
            pdfDocument.close()
        }
    }

    fun generateLabelSheetPdf(
        context: Context,
        products: List<Pair<Product, Int>>,
        showName: Boolean,
        showPrice: Boolean,
        currencySymbol: String,
        labelSizeStr: String = "MEDIUM",
        forceType: String = "AUTO"
    ): Uri? {
        val pdfDocument = PdfDocument()
        val a4Width = 595
        val a4Height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        
        val margin = 20f
        
        val (labelWidth, labelHeight, labelsPerRow) = when (labelSizeStr.uppercase()) {
            "SMALL" -> Triple(110f, 60f, 5)
            "LARGE" -> Triple(250f, 150f, 2)
            else -> Triple(180f, 100f, 3) // MEDIUM
        }
        
        val spacing = (a4Width - 2 * margin - (labelsPerRow * labelWidth)) / (labelsPerRow - 1).coerceAtLeast(1)
        
        val scale = when(labelSizeStr.uppercase()) {
            "SMALL" -> 0.6f
            "LARGE" -> 1.4f
            else -> 1.0f
        }

        var x = margin
        var y = margin
        
        products.forEach { (product, qty) ->
            repeat(qty) {
                if (y + labelHeight > a4Height - margin) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin
                    x = margin
                }
                
                paint.style = Paint.Style.STROKE
                paint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                paint.color = Color.LTGRAY
                paint.strokeWidth = 0.5f
                canvas.drawRect(x, y, x + labelWidth, y + labelHeight, paint)
                
                paint.pathEffect = null
                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK
                paint.textAlign = Paint.Align.CENTER
                
                val centerX = x + labelWidth / 2
                val code = if (product.barcode.isNullOrBlank()) product.id.toString() else product.barcode ?: product.id.toString()
                
                val nameHeight = if (showName) 12f * scale else 0f
                val priceHeight = if (showPrice) 14f * scale else 0f
                val availableImageHeight = labelHeight - nameHeight - priceHeight - (20f * scale)
                
                val bitmapWidth = (labelWidth * 0.85).toInt()
                val bitmapHeight = availableImageHeight.toInt().coerceAtLeast(10)
                
                val barcodeBitmap = when (forceType.uppercase()) {
                    "QR" -> BarcodeUtils.generateQRCode(code, bitmapHeight)
                    "BARCODE" -> BarcodeUtils.generateBarcode(code, bitmapWidth, bitmapHeight)
                    else -> {
                        if (code.length > 20) BarcodeUtils.generateQRCode(code, bitmapHeight)
                        else BarcodeUtils.generateBarcode(code, bitmapWidth, bitmapHeight)
                    }
                }

                var currentY = y + (10f * scale)
                
                if (showName) {
                    paint.textSize = 8f * scale
                    val displayName = if (product.name.length > 25) product.name.take(22) + "..." else product.name
                    canvas.drawText(displayName, centerX, currentY + paint.textSize, paint)
                    currentY += paint.textSize + (4f * scale)
                }
                
                barcodeBitmap?.let {
                    val imgWidth = if (forceType == "QR" || (forceType == "AUTO" && code.length > 20)) 
                        it.height.toFloat() else it.width.toFloat()
                        
                    val rect = RectF(
                        centerX - imgWidth / 2f,
                        currentY,
                        centerX + imgWidth / 2f,
                        currentY + it.height
                    )
                    canvas.drawBitmap(it, null, rect, null)
                    currentY += it.height + (4f * scale)
                }
                
                if (showPrice) {
                    paint.isFakeBoldText = true
                    paint.textSize = 10f * scale
                    canvas.drawText(CurrencyUtils.formatCurrency(product.price, currencySymbol), centerX, currentY + paint.textSize, paint)
                    paint.isFakeBoldText = false
                }
                
                x += labelWidth + spacing
                if (x + labelWidth > a4Width - margin + 5f) {
                    x = margin
                    y += labelHeight + 10f
                }
            }
        }
        
        pdfDocument.finishPage(page)
        
        val fileName = "Labels_${System.currentTimeMillis()}.pdf"
        return saveToPublicDownloads(context, fileName) { stream ->
            pdfDocument.writeTo(stream)
            pdfDocument.close()
        }
    }

    fun generateSalesReportPdf(
        context: Context,
        transactions: List<TransactionWithItems>,
        currencySymbol: String,
        todayRevenue: Double,
        todayCount: Int,
        monthlyRevenue: Double,
        monthlyCount: Int,
        weeklyTrend: List<DailySales>,
        topProductsList: List<TopProduct>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        
        var y = 50f
        
        // 1. Header
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Business Sales Report", 297f, y, paint)
        y += 35f
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        val genDate = "Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}"
        canvas.drawText(genDate, 297f, y, paint)
        y += 45f
        
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        
        // 2. High Level Summary (Today / Month)
        paint.isFakeBoldText = true
        paint.textSize = 14f
        canvas.drawText("Performance Summary", 50f, y, paint)
        y += 25f
        
        paint.isFakeBoldText = false
        paint.textSize = 11f
        canvas.drawText("Today's Revenue: ${CurrencyUtils.formatCurrency(todayRevenue, currencySymbol)} ($todayCount txns)", 60f, y, paint)
        y += 18f
        canvas.drawText("Monthly Revenue: ${CurrencyUtils.formatCurrency(monthlyRevenue, currencySymbol)} ($monthlyCount txns)", 60f, y, paint)
        y += 35f

        // 3. Weekly Sales Trend
        if (weeklyTrend.isNotEmpty()) {
            paint.isFakeBoldText = true
            paint.textSize = 14f
            canvas.drawText("Weekly Sales Trend", 50f, y, paint)
            y += 25f
            
            paint.isFakeBoldText = false
            paint.textSize = 10f
            weeklyTrend.forEach { day ->
                canvas.drawText("${day.date}: ${CurrencyUtils.formatCurrency(day.revenue, currencySymbol)}", 70f, y, paint)
                y += 15f
            }
            y += 25f
        }

        // 4. Top Products
        if (topProductsList.isNotEmpty()) {
            paint.isFakeBoldText = true
            paint.textSize = 14f
            canvas.drawText("Top Selling Products", 50f, y, paint)
            y += 25f
            
            paint.isFakeBoldText = false
            paint.textSize = 10f
            topProductsList.forEach { product ->
                canvas.drawText("- ${product.name}: ${product.quantity} units (${CurrencyUtils.formatCurrency(product.revenue, currencySymbol)})", 70f, y, paint)
                y += 15f
            }
            y += 30f
        }

        // 5. Transaction History Table
        paint.isFakeBoldText = true
        paint.textSize = 14f
        canvas.drawText("Recent Transaction History", 50f, y, paint)
        y += 20f
        
        paint.strokeWidth = 1.5f
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 18f
        
        paint.textSize = 10f
        canvas.drawText("ID", 55f, y, paint)
        canvas.drawText("Date", 100f, y, paint)
        canvas.drawText("Items", 300f, y, paint)
        canvas.drawText("Tax", 400f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total", 540f, y, paint)
        y += 10f
        
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 20f
        
        paint.isFakeBoldText = false
        transactions.forEach { tx ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                
                // Re-draw table header on new page
                paint.isFakeBoldText = true
                canvas.drawText("ID", 55f, y, paint)
                canvas.drawText("Date", 100f, y, paint)
                canvas.drawText("Items", 300f, y, paint)
                canvas.drawText("Tax", 400f, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Total", 540f, y, paint)
                y += 10f
                canvas.drawLine(50f, y, 545f, y, paint)
                y += 20f
                paint.isFakeBoldText = false
            }
            
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("#${tx.transaction.id}", 55f, y, paint)
            val txDate = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(tx.transaction.timestamp))
            canvas.drawText(txDate, 100f, y, paint)
            canvas.drawText("${tx.items.size}", 300f, y, paint)
            canvas.drawText(CurrencyUtils.formatCurrency(tx.transaction.taxAmount, currencySymbol), 400f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyUtils.formatCurrency(tx.transaction.total, currencySymbol), 540f, y, paint)
            y += 15f
        }
        
        pdfDocument.finishPage(page)
        
        val fileName = "Full_Sales_Report_${System.currentTimeMillis()}.pdf"
        return saveToPublicDownloads(context, fileName) { stream ->
            pdfDocument.writeTo(stream)
            pdfDocument.close()
        }
    }

    fun generatePromoLabelsPdf(
        context: Context,
        promos: List<com.tataskan.pos.data.entity.Promo>,
        currencySymbol: String
    ): Uri? {
        val pdfDocument = PdfDocument()
        val a4Width = 595
        val a4Height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        
        val margin = 40f
        val labelWidth = 240f
        val labelHeight = 140f
        val spacing = 20f

        var x = margin
        var y = margin
        
        promos.forEach { promo ->
            if (y + labelHeight > a4Height - margin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = margin
                x = margin
            }
            
            // Draw Border
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawRect(x, y, x + labelWidth, y + labelHeight, paint)
            
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            val centerX = x + labelWidth / 2
            
            // Title
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText(promo.name, centerX, y + 25f, paint)
            
            // Discount Value
            paint.textSize = 18f
            paint.color = Color.parseColor("#E91E63") // Pink/Red
            val valueText = if (promo.type == com.tataskan.pos.data.entity.PromoType.PERCENTAGE_TOTAL || promo.type == com.tataskan.pos.data.entity.PromoType.PERCENTAGE_PRODUCT)
                "${promo.value}% OFF" else "${CurrencyUtils.formatCurrency(promo.value, currencySymbol)} OFF"
            canvas.drawText(valueText, centerX, y + 50f, paint)
            
            // QR Code
            paint.color = Color.BLACK
            val qrBitmap = BarcodeUtils.generateQRCode(promo.code, 60)
            qrBitmap?.let {
                val rect = RectF(centerX - 30f, y + 60f, centerX + 30f, y + 120f)
                canvas.drawBitmap(it, null, rect, null)
            }
            
            // Code text
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText(promo.code, centerX, y + 132f, paint)
            
            // Movement logic
            x += labelWidth + spacing
            if (x + labelWidth > a4Width - margin) {
                x = margin
                y += labelHeight + spacing
            }
        }
        
        pdfDocument.finishPage(page)
        
        val fileName = "Promos_${System.currentTimeMillis()}.pdf"
        return saveToPublicDownloads(context, fileName) { stream ->
            pdfDocument.writeTo(stream)
            pdfDocument.close()
        }
    }

    fun savePromoToGallery(
        context: Context,
        promo: com.tataskan.pos.data.entity.Promo,
        currencySymbol: String
    ): Boolean {
        val width = 400
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        canvas.drawColor(Color.WHITE)

        // Border
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 4f
        canvas.drawRect(10f, 10f, width - 10f, height - 10f, paint)

        // Content
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER

        // Title
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText(promo.name, width / 2f, 60f, paint)

        // Discount
        paint.textSize = 36f
        paint.color = Color.parseColor("#E91E63")
        val valueText = if (promo.type == com.tataskan.pos.data.entity.PromoType.PERCENTAGE_TOTAL || promo.type == com.tataskan.pos.data.entity.PromoType.PERCENTAGE_PRODUCT)
            "${promo.value}% OFF" else "${CurrencyUtils.formatCurrency(promo.value, currencySymbol)} OFF"
        canvas.drawText(valueText, width / 2f, 110f, paint)

        // QR Code
        paint.color = Color.BLACK
        val qrBitmap = BarcodeUtils.generateQRCode(promo.code, 180)
        qrBitmap?.let {
            val rect = RectF(width / 2f - 90f, 140f, width / 2f + 90f, 330f)
            canvas.drawBitmap(it, null, rect, null)
        }

        // Code text
        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText(promo.code, width / 2f, 370f, paint)

        // Save
        val fileName = "Promo_${promo.name.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Tataskan")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        return try {
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
