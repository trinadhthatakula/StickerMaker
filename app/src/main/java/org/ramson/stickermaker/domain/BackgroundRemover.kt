package org.ramson.stickermaker.domain


import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis


object BackgroundRemover {

    private var buffer = ByteBuffer.allocate(0)
    private var width = 0
    private var height = 0
    private val segmentOptions by lazy {
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
    }
    private val segment by lazy {
        Segmentation.getClient(segmentOptions)
    }

    /**
     * Process the image to get buffer and image height and width
     * @param bitmap Bitmap which you want to remove background.
     * @param trimEmptyPart After removing the background if its true it will remove the empty part of bitmap. by default its false.
     * @param listener listener for success and failure callback.
     **/
    fun bitmapForProcessing(
        bitmap: Bitmap,
        trimEmptyPart: Boolean? = false,
        listener: OnBackgroundChangeListener
    ) {
        //Generate a copy of bitmap just in case the if the bitmap is immutable.
        val copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val input = InputImage.fromBitmap(copyBitmap, 0)
        segment.process(input)
            .addOnSuccessListener { segmentationMask ->
                buffer = segmentationMask.buffer
                width = segmentationMask.width
                height = segmentationMask.height

                CoroutineScope(Dispatchers.IO).launch {
                    val time = measureTimeMillis {
                        val resultBitmap = if (trimEmptyPart == true) {
                            val bgRemovedBitmap = removeBackgroundFromImage(copyBitmap)
                            trim(bgRemovedBitmap)
                        } else {
                            removeBackgroundFromImage(copyBitmap)
                        }
                        val mask = generateMask(copyBitmap)
                        withContext(Dispatchers.Main) {
                            listener.onSuccessWithMask(resultBitmap, mask)
                            listener.onSuccess(resultBitmap)
                        }
                    }
                    Log.e("TAG", "bitmapForProcessingTime: $time")
                }

            }
            .addOnFailureListener { e ->
                println("Image processing failed: $e")
                listener.onFailed(e)

            }
    }


    /**
     * Change the background pixels color to transparent.
     * */
    private suspend fun removeBackgroundFromImage(
        image: Bitmap
    ): Bitmap {
        val bitmap = CoroutineScope(Dispatchers.IO).async {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val bgConfidence = ((1.0 - buffer.float) * 255).toInt()
                    if (bgConfidence >= 100) {
                        image.setPixel(x, y, Color.TRANSPARENT)
                    }
                }
            }
            buffer.rewind()
            return@async image
        }
        return bitmap.await()
    }

    private suspend fun generateMask(
        image: Bitmap
    ): Bitmap {
        val bitmap = CoroutineScope(Dispatchers.IO).async {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val bgConfidence = ((1.0 - buffer.float) * 255).toInt()
                    if (bgConfidence >= 100) {
                        image.setPixel(x, y, Color.TRANSPARENT)
                    }else {
                        image.setPixel(x,y,Color.BLACK)
                    }
                }
            }
            buffer.rewind()
            return@async image
        }
        return bitmap.await()
    }


    /**
     * trim the empty part of a bitmap.
     **/
    private suspend fun trim(
        bitmap: Bitmap
    ): Bitmap {
        val result = CoroutineScope(Dispatchers.Default).async {
            var firstX = 0
            var firstY = 0
            var lastX = bitmap.width
            var lastY = bitmap.height
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            loop@ for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    if (pixels[x + y * bitmap.width] != Color.TRANSPARENT) {
                        firstX = x
                        break@loop
                    }
                }
            }
            loop@ for (y in 0 until bitmap.height) {
                for (x in firstX until bitmap.width) {
                    if (pixels[x + y * bitmap.width] != Color.TRANSPARENT) {
                        firstY = y
                        break@loop
                    }
                }
            }
            loop@ for (x in bitmap.width - 1 downTo firstX) {
                for (y in bitmap.height - 1 downTo firstY) {
                    if (pixels[x + y * bitmap.width] != Color.TRANSPARENT) {
                        lastX = x
                        break@loop
                    }
                }
            }
            loop@ for (y in bitmap.height - 1 downTo firstY) {
                for (x in bitmap.width - 1 downTo firstX) {
                    if (pixels[x + y * bitmap.width] != Color.TRANSPARENT) {
                        lastY = y
                        break@loop
                    }
                }
            }
            return@async Bitmap.createBitmap(bitmap, firstX, firstY, lastX - firstX, lastY - firstY)
        }
        return result.await()
    }

}