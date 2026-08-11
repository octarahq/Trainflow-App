package com.octarahq.trainflow.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AztecScanner {

    suspend fun decodeAztecFromUri(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return if (mimeType == "application/pdf") {
            decodeFromPdf(context, uri)
        } else {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmapWithExifCorrection(context, uri)
            }
            if (bitmap != null) {
                val qrResult = decodeFromBitmap(bitmap)
                
                if (qrResult != null && qrResult.startsWith("i0C")) {
                    return qrResult
                }
                
                val ocrResult = runOcr(bitmap)
                if (ocrResult != null) {
                    return ocrResult
                }
                
                return qrResult
            }
            null
        }
    }

    private fun loadBitmapWithExifCorrection(context: Context, uri: Uri): Bitmap? {
        return try {
            val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { exifStream ->
                ExifInterface(exifStream).rotationDegrees
            } ?: 0

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    private suspend fun decodeFromPdf(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext null
                pfd.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        for (pageIndex in 0 until renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                val scale = 4f
                                val width = (page.width * scale).toInt()
                                val height = (page.height * scale).toInt()
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                val result = decodeFromBitmap(bitmap)
                                if (result != null) return@withContext result
                            }
                        }
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun decodeFromBitmap(bitmap: Bitmap): String? {
        runMlKit(bitmap, Barcode.FORMAT_AZTEC)?.let { return it }
        runMlKit(bitmap, Barcode.FORMAT_ALL_FORMATS)?.let { return it }
        return null
    }

    private suspend fun runMlKit(bitmap: Bitmap, @Barcode.BarcodeFormat format: Int): String? {
        return try {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(format)
                .build()
            val scanner: BarcodeScanner = BarcodeScanning.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            val results = scanner.process(image).await()
            scanner.close()

            val barcode = results.firstOrNull() ?: return null
            barcode.rawValue ?: barcode.rawBytes?.let { bytes -> String(bytes, Charsets.ISO_8859_1) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun runOcr(bitmap: Bitmap): String? {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            recognizer.close()

            val lines = result.textBlocks.flatMap { it.lines }.map { it.text.trim() }
            if (lines.isEmpty()) return null

            var departure = ""
            var arrival = ""
            var trainNumber = ""
            var passengerName = ""
            var dateStr = ""
            var pnr = ""

            val routeRegex = Regex("(.+?)\\s*[—→➔\\-]{1,2}\\s*(.+)")
            for (line in lines) {
                val match = routeRegex.find(line)
                if (match != null && !line.contains("Voiture", ignoreCase = true) && !line.contains("Place", ignoreCase = true)) {
                    departure = match.groupValues[1].trim()
                    arrival = match.groupValues[2].trim()
                    break
                }
            }

            if (departure.isEmpty()) {
                val stations = lines.filter { it.length > 3 && it.all { c -> c.isUpperCase() || c.isWhitespace() || c == '-' } && !it.contains("SNCF") && !it.contains("CONNECT") && !it.contains("PASSAGER") }
                if (stations.size >= 2) {
                    departure = stations[0]
                    arrival = stations[1]
                }
            }

            for (i in lines.indices) {
                val line = lines[i]

                if (line.contains("Train", ignoreCase = true)) {
                    val numRegex = Regex("\\d+")
                    val matchThis = numRegex.find(line)
                    if (matchThis != null) {
                        trainNumber = matchThis.value
                    } else if (i + 1 < lines.size) {
                        val matchNext = numRegex.find(lines[i + 1])
                        if (matchNext != null) {
                            trainNumber = matchNext.value
                        }
                    }
                }

                if (line.contains("Passager", ignoreCase = true) || line.contains("Voyageur", ignoreCase = true)) {
                    val nameParts = mutableListOf<String>()
                    var j = i + 1
                    while (j < lines.size && nameParts.size < 2) {
                        val nextLine = lines[j]
                        if (nextLine.isNotEmpty() && nextLine.replace(" ", "").all { it.isLetter() || it == '-' }) {
                            nameParts.add(nextLine)
                        }
                        j++
                    }
                    if (nameParts.isNotEmpty()) {
                        passengerName = nameParts.joinToString(" ")
                    }
                }

                if (line.contains("Dossier", ignoreCase = true) || line.contains("voyage", ignoreCase = true)) {
                    val pnrRegex = Regex("[A-Z0-9]{6}")
                    val matchThis = pnrRegex.find(line)
                    if (matchThis != null && matchThis.value != "CONNEC" && matchThis.value != "INOUIP") {
                        pnr = matchThis.value
                    } else {
                        for (k in 1..3) {
                            if (i + k < lines.size) {
                                val textToCheck = lines[i + k]
                                val matchNext = pnrRegex.find(textToCheck)
                                if (matchNext != null && !textToCheck.contains("SNCF") && !textToCheck.contains("TGV") && !textToCheck.contains("VOYAGE")) {
                                    pnr = matchNext.value
                                    break
                                }
                            }
                        }
                    }
                }

                val dateRegex = Regex("(\\d{1,2}\\s+(?:janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc|janvier|février|avril|juillet|septembre|octobre|novembre|décembre)[a-z.]*)", RegexOption.IGNORE_CASE)
                val matchDate = dateRegex.find(line)
                if (matchDate != null && (line.contains("départ", ignoreCase = true) || dateStr.isEmpty())) {
                    dateStr = matchDate.value
                }
            }

            if (departure.isNotEmpty() || arrival.isNotEmpty() || passengerName.isNotEmpty() || pnr.isNotEmpty()) {
                return "ocr_parsed:${departure}|${arrival}|${dateStr}|${trainNumber}|${passengerName}|${pnr}"
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
