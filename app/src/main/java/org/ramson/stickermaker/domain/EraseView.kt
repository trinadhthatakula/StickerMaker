package org.ramson.stickermaker.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

suspend fun combineBitmapsWithPorterDuff(originalBitmap: Bitmap, maskBitmap: Bitmap): Bitmap {
    return CoroutineScope(Dispatchers.IO).async{
        val combinedBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw the original image
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

        // Apply the mask using PorterDuff mode DST_IN
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, paint)

        // Reset the PorterDuff mode to default
        paint.xfermode = null
        combinedBitmap
    }.await()
}

class EraseView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val path: Path = Path()
    private val paint: Paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = 50f
    }
    private val paint2: Paint = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = 50f
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        this.canvas = Canvas(this.bitmap!!)
    }

    fun saveErasedPortionToFile(filePath: String) {
        val erasedBitmap =
            Bitmap.createBitmap(bitmap!!.width, bitmap!!.height, Bitmap.Config.ARGB_8888)
        val erasedCanvas = Canvas(erasedBitmap)

        // Draw the original bitmap on the erased canvas
        erasedCanvas.drawBitmap(bitmap!!, 0f, 0f, null)

        // Draw the path with the clear paint to erase the portion
        erasedCanvas.drawPath(path, paint)

        // Save the erased portion as a PNG image
        try {
            val file = File(filePath)
            val fileOutputStream = FileOutputStream(file)
            erasedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveErasedPortionToBitmap(): Bitmap {
        val erasedBitmap =
            Bitmap.createBitmap(bitmap!!.width, bitmap!!.height, Bitmap.Config.ARGB_8888)
        val erasedCanvas = Canvas(erasedBitmap)
        erasedCanvas.drawBitmap(bitmap!!, 0f, 0f, null)
        erasedCanvas.drawPath(path, paint2)
        return erasedBitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
            canvas.drawPath(path, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
            }

            MotionEvent.ACTION_UP -> {
                path.lineTo(x, y)
                canvas?.drawPath(path, paint)
                path.reset()
            }
        }

        invalidate()
        return true
    }
}
