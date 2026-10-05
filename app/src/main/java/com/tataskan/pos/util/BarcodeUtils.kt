package com.tataskan.pos.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.OutputStream

object BarcodeUtils {

    fun generateBarcode(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, width, height)
            bitMatrixToBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQRCode(text: String, size: Int): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            bitMatrixToBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateBarcodeWithText(
        barcodeText: String,
        productName: String,
        price: String,
        width: Int,
        height: Int,
        showName: Boolean = true,
        showPrice: Boolean = true
    ): Bitmap? {
        val lineCount = (if (showName) 1 else 0) + (if (showPrice) 1 else 0)
        val barcodeHeight = if (lineCount > 0) (height * (1.0 - 0.15 * lineCount)).toInt() else height
        val barcodeBitmap = generateBarcode(barcodeText, width, barcodeHeight) ?: return null
        
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)
        
        canvas.drawBitmap(barcodeBitmap, 0f, 10f, null)
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = width * 0.05f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        var currentY = (barcodeHeight + 40).toFloat()
        
        if (showName) {
            canvas.drawText(productName, (width / 2).toFloat(), currentY, paint)
            currentY += 40f
        }
        
        if (showPrice) {
            paint.textSize = width * 0.04f
            canvas.drawText(price, (width / 2).toFloat(), currentY, paint)
        }
        
        return resultBitmap
    }

    fun generateQRCodeWithText(
        qrText: String,
        productName: String,
        price: String,
        size: Int,
        showName: Boolean = true,
        showPrice: Boolean = true
    ): Bitmap? {
        val lineCount = (if (showName) 1 else 0) + (if (showPrice) 1 else 0)
        val qrSize = if (lineCount > 0) (size * (1.0 - 0.1 * lineCount)).toInt() else size
        val qrBitmap = generateQRCode(qrText, qrSize) ?: return null
        
        val resultBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)
        
        val left = (size - qrSize) / 2f
        canvas.drawBitmap(qrBitmap, left, 10f, null)
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = size * 0.06f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        var currentY = (qrSize + 45).toFloat()
        
        if (showName) {
            canvas.drawText(productName, (size / 2).toFloat(), currentY, paint)
            currentY += size * 0.08f
        }
        
        if (showPrice) {
            paint.textSize = size * 0.05f
            paint.isFakeBoldText = true
            canvas.drawText(price, (size / 2).toFloat(), currentY, paint)
        }
        
        return resultBitmap
    }

    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun generateRandomBarcodeString(length: Int = 12): String {
        val allowedChars = "0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SukiPos")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return uri
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(uri, null, null)
            }
        }
        return null
    }
}
