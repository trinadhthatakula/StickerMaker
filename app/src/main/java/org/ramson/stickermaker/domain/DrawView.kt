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

class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var initBitmap: Bitmap? = null
    private var bitmap: Bitmap? = null
    private var path = Path()
    private var erasePath = Path()

    var brushRadius: Float = 50f
        set(value) {
            field = value
            path = Path()
            erasePath = Path()
            paint.strokeWidth = value
            paintMask.strokeWidth = value
        }
    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        //xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = brushRadius
    }
    private val paintMask: Paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = brushRadius
    }
    /*private val scaleGestureDetector: ScaleGestureDetector =
        ScaleGestureDetector(context, ScaleListener())

    private var scaleFactor = 1.0f*/

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    var maskAppliedListener: MaskAppliedListener? = null

    interface MaskAppliedListener {
        fun onMaskApplied(bitmap: Bitmap?)
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        initBitmap = bitmap
        path = Path()
        erasePath = Path()
        invalidate()
    }

    var isAdding = true
        set(value) {
            field = value
            path = Path()
            erasePath = Path()
            invalidate()
        }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
        bitmap?.let { bt -> canvas.drawBitmap(bt, 0f, 0f, null) }
        if (isAdding)
            canvas.drawPath(path, paint)
        else {
            canvas.drawPath(erasePath, paintMask)
        }
        val genBitmap = generateMask()
        bitmap = genBitmap
        maskAppliedListener?.onMaskApplied(
            genBitmap
        )
    }

    fun undo(){

    }

    fun reset(){
        initBitmap?.let {
            bitmap = it
            path = Path()
            erasePath = Path()
            invalidate()
            maskAppliedListener?.onMaskApplied(it)
        }
    }

    private fun generateMask(): Bitmap? {
        bitmap?.let { bt ->
            val result = Bitmap.createBitmap(bt.width, bt.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawBitmap(bt, 0f, 0f, null)
            if (isAdding)
                canvas.drawPath(path, paint)
            else {
                canvas.drawPath(erasePath, paintMask)
            }
            return result
        } ?: return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //scaleGestureDetector.onTouchEvent(event)
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                if (isAdding)
                    path.moveTo(x, y)
                else erasePath.moveTo(x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isAdding)
                    path.lineTo(x, y)
                else erasePath.lineTo(x, y)
            }

            MotionEvent.ACTION_UP -> {
                if (isAdding) {
                    extraCanvas.drawPath(path, paint)
                } else {
                    //path.lineTo(x, y)
                    extraCanvas.drawPath(erasePath, paintMask)
                    //path.reset()
                }
                //path.reset()
            }
        }

        invalidate()
        return true
    }

    /*private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = 1.0f.coerceAtLeast(
                scaleFactor.coerceAtMost(5.0f)
            ) // Limit scale factor between 1.0 and 5.0
            extraCanvas.save()

            // Adjust the canvas matrix to apply zoom and pivot at the pinch point
            val focusX = detector.focusX
            val focusY = detector.focusY
            extraCanvas.scale(scaleFactor, scaleFactor, focusX, focusY)

            invalidate()
            return true
        }
    }*/
}
