package com.example.piratescrabble

import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream

@ExperimentalGetImage class ScrabbleAnalyzer(private val listener : textListener) : ImageAnalysis.Analyzer {

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = when (imageProxy.format) {
            // Depending on image format, we use different methods to convert the imageproxy to bitmap
            35 -> imageProxy.image?.toBitmap()
            256 ->  imageProxy.convertImageProxyToBitmap()
            else -> { null }
        }
        if (bitmap != null) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    listener(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    listener(e.toString())
                }
            // TODO: Process result text (currently just a string of letters/words seperated by new lines)
        }
        imageProxy.close()
    }
}