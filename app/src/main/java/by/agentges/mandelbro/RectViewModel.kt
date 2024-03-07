package by.agentges.mandelbro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class RectViewModel : ViewModel() {

    private var job: Job? = null
    val offLock = Object()
    var offsx = 0f
    var offsy = 0f
    var w: Int = 0
    var h: Int = 0


    var fullRect: Rect = Rect()

    val src = Rect()
    val dst = Rect()

    var bitmap: Bitmap? = null

    fun onDragStart(offset: Offset) {
        Log.d("tttt", "Drag start: $offset")
    }

    fun onDragEnd() {
        Log.d("tttt", "Drag end on $offsx, $offsy")
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        synchronized(offLock) {
            offsx += dragAmount.x
            offsy += dragAmount.y
            calcBitmapDrawingRects()
        }
    }

    private fun calcBitmapDrawingRects() {
        src.set(
            max(0f, -offsx).roundToInt(),
            max(0f, -offsy).roundToInt(),
            (max(0f, -offsx) + (w - offsx.absoluteValue)).roundToInt(),
            (max(0f, -offsy) + (h - offsy.absoluteValue)).roundToInt(),
        )

        dst.set(
            max(0f, offsx).roundToInt(),
            max(0f, offsy).roundToInt(),
            (max(0f, offsx) + (w - offsx.absoluteValue)).roundToInt(),
            (max(0f, offsy) + (h - offsy.absoluteValue)).roundToInt(),
        )
    }

    fun onSurfaceCreated(width: Int, height: Int) {
        onSurfaceSizeChanged(width, height)
    }

    fun onSurfaceSizeChanged(newWidth: Int, newHeight: Int) {
        fullRect = Rect(0, 0, newWidth, newHeight)
        w = newWidth
        h = newHeight
        calcBitmapDrawingRects()

        if (bitmap != null) return
        viewModelScope.launch(Dispatchers.Default) {
            bitmap?.recycle()
            bitmap = null
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).run {
                bitmap = this
                val canvas = Canvas(this)
                canvas.drawColor(Color.Black.toArgb())
                job = launch {
                    rectDraw(canvas, fullRect)
                }
            }
        }
    }

    fun onSurfaceDestroyed() {
        // nothing for now
    }

    override fun onCleared() {
        super.onCleared()
        bitmap?.recycle()
        bitmap = null
    }
}

fun MutableList<Rect>.addNotEmpty(rect: Rect) {
    if (rect.isEmpty.not()) {
        add(rect)
    }
}

fun MutableList<Rect>.addIf(rect: Rect, predicate: (Rect) -> Boolean): Boolean {
    return predicate(rect).also {
        if (it) {
            add(rect)
        }
    }
}

private suspend fun rectDraw(canvas: Canvas, rect: Rect) {
    val pointPaint = Paint().apply {
        color = Color.Red.toArgb()
        strokeWidth = 50f
    }
    canvas.drawColor(Color.Blue.toArgb())

    val cx = rect.centerX()
    val cy = rect.centerY()


    val rectPaint = Paint().apply {
        color = Color.Green.toArgb()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }


    val rectSize = 64
    val rects: MutableList<Rect> = mutableListOf()

    val waveRect = Rect(cx, cy, cx, cy)
    var rectsCount: Int
    do {
        rectsCount = rects.count()
        waveRect.inset(-rectSize, -rectSize)
        rectPaint.color = Color.Black.toArgb()
        canvas.drawRect(waveRect, rectPaint)
        rectPaint.color = Color.Green.toArgb()

        var x = waveRect.left
        val predicate: (Rect) -> Boolean = { !it.isEmpty && rect.containsOrIntersects(it) }

        while (x < waveRect.right) {
            rects.addIf(Rect(x, waveRect.top, x + rectSize, waveRect.top + rectSize), predicate)
            rects.addIf(Rect(x, waveRect.bottom - rectSize, x + rectSize, waveRect.bottom), predicate)
            x += rectSize
        }

        var y = waveRect.top + rectSize
        while (y < waveRect.bottom - rectSize) {
            rects.addIf(Rect(waveRect.left, y, waveRect.left + rectSize, y + rectSize), predicate)
            rects.addIf(Rect(waveRect.right - rectSize, y, waveRect.right, y + rectSize), predicate)
            y += rectSize
        }
    } while (rects.count() > rectsCount)

    rects.forEach {
        delay(100)
        canvas.drawRect(it, rectPaint)
    }


}

fun Rect.containsOrIntersects(r: Rect): Boolean {
    if (isEmpty) return false
    if (contains(r)) return true
    if (Rect.intersects(this, r)) return true
    return false
}
