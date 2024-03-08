package by.agentges.mandelbro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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

class RectViewModel : ViewModel() {

    private val tileRectSize = 256

    private var job: Job? = null
    val offLock = Object()
    var offsx = 0
    var offsy = 0
    var scale = 256
    var w: Int = 0
    var h: Int = 0


    var fullRect: Rect = Rect()

    val src = Rect()
    val dst = Rect()

    var bitmap: Bitmap? = null

    val tileRects = mutableListOf<TileRect>()

    fun onDragStart(offset: Offset) {
        Log.d("tttt", "Drag start: $offset")
    }

    fun onDragEnd() {
        Log.d("tttt", "Drag end on $offsx, $offsy")
        //recreate bitmap
        //apply tile rects to bimap???




        //TEST
        val centerX = fullRect.centerX()
        val centerY = fullRect.centerY()
        tileRects[0].toCartesian(
            centerX,
            centerY,
            offsx,
            offsy,
            scale
        ).let {
            Log.d("tttt", "tileRects[0].cartesian = $it")
        }



       /* Log.d("tttt", "to cartesian 0 0 1 -1")
        test5(0, 0)
        Log.d("tttt", "to cartesian 0 1 1 0")
        test5(0, 1)
        Log.d("tttt", "to cartesian 1 0 2 -1")
        test5(1, 0)
        Log.d("tttt", "to cartesian 1 1 2 0")
        test5(1, 1)
        Log.d("tttt", "to cartesian -1 1 0 0")
        test5(-1, 1)
        Log.d("tttt", "to cartesian -1 0 0 -1")
        test5(-1, 0)
*/
    }

    private fun test5(x: Int, y: Int) {
        val centerX = fullRect.centerX()
        val centerY = fullRect.centerY()
        val sc = scale.toInt()

        val l = centerX.toInt() + sc * x
        val t = centerY.toInt() - sc * y

        val tileRect = TileRect(
            Rect(l, t, l + sc, t + sc),
            Color.Black
        )
        val cartesianRect = tileRect.toCartesian(
            centerX,
            centerY,
            0,
            0,
            scale
        )
        Log.d("tttt", "cartesianRect = $cartesianRect")
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        synchronized(offLock) {
            offsx += dragAmount.x.roundToInt()
            offsy += dragAmount.y.roundToInt()
            calcBitmapDrawingRects()
            calcTileRects(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
        }
    }

    private fun calcTileRects(dx: Int, dy: Int) {
        tileRects.offset(dx, dy)
        if (tileRects.union().contains(fullRect)) {
            return
        }

        var count = tileRects.size

        tileRects.removeAll { fullRect.containsOrIntersects(it.rect).not() }
        val removed = count - tileRects.size
        count = tileRects.size
        tileRects.forEach { it.color = Color.Yellow }
        tileRects.firstOrNull()?.let { it.color = Color.Blue }
        createTileRects(
            initialRect = tileRects.union().let {
                if (it.isEmpty) Rect(
                    fullRect.centerX(),
                    fullRect.centerY(),
                    fullRect.centerX(),
                    fullRect.centerY()
                ) else it
            },
            targetRect = fullRect,
            tileSize = tileRectSize
        ).let {
            tileRects.addAll(it)
        }
        val added = tileRects.size - count
        Log.d("tttt", "Removed $removed, added $added")
    }

    private fun calcBitmapDrawingRects() {
        src.set(
            max(0, -offsx),
            max(0, -offsy),
            (max(0, -offsx) + (w - offsx.absoluteValue)),
            (max(0, -offsy) + (h - offsy.absoluteValue)),
        )

        dst.set(
            max(0, offsx),
            max(0, offsy),
            (max(0, offsx) + (w - offsx.absoluteValue)),
            (max(0, offsy) + (h - offsy.absoluteValue)),
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
        calcTileRects(0, 0)

        if (bitmap != null) return
        viewModelScope.launch(Dispatchers.Default) {
            bitmap?.recycle()
            bitmap = null
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).run {
                bitmap = this
                val canvas = Canvas(this)
                canvas.drawColor(Color.Black.toArgb())
                job = launch {
                    rectDraw(canvas, fullRect, tileRectSize)
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

private suspend fun rectDraw(canvas: Canvas, rect: Rect, tileSize: Int) {
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


    val rects: MutableList<Rect> = mutableListOf()

    val waveRect = Rect(cx, cy, cx, cy)
    rects.addAll(
        createTileRects(waveRect, rect, tileSize).map { it.rect }
    )
    rects.forEach {
        delay(100)
        canvas.drawRect(it, rectPaint)
    }


}

private fun createTileRects(initialRect: Rect, targetRect: Rect, tileSize: Int): List<TileRect> {
    val result = mutableListOf<TileRect>()
    val waveRect = initialRect
    var rectsCount: Int
    do {
        rectsCount = result.count()
        waveRect.inset(-tileSize, -tileSize)
        var x = waveRect.left
        val predicate: (TileRect) -> Boolean = { !it.rect.isEmpty && targetRect.containsOrIntersects(it.rect) }

        while (x < waveRect.right) {
            result.addIf(
                TileRect(Rect(x, waveRect.top, x + tileSize, waveRect.top + tileSize), color = Color.Red),
                predicate
            )
            result.addIf(
                TileRect(Rect(x, waveRect.bottom - tileSize, x + tileSize, waveRect.bottom), color = Color.Red),
                predicate
            )
            x += tileSize
        }

        var y = waveRect.top + tileSize
        while (y < waveRect.bottom - tileSize) {
            result.addIf(
                TileRect(Rect(waveRect.left, y, waveRect.left + tileSize, y + tileSize), color = Color.Red),
                predicate
            )
            result.addIf(
                TileRect(Rect(waveRect.right - tileSize, y, waveRect.right, y + tileSize), color = Color.Red),
                predicate
            )
            y += tileSize
        }
    } while (result.count() > rectsCount)

    return result
}


fun Rect.containsOrIntersects(r: Rect): Boolean {
    if (isEmpty) return false
    if (contains(r)) return true
    if (Rect.intersects(this, r)) return true
    return false
}

fun List<TileRect>.offset(x: Int, y: Int) {
    this.forEach {
        it.rect.offset(x, y)
    }
}

fun List<TileRect>.union(): Rect {
    val unionRect = Rect()
    this.forEach {
        unionRect.union(it.rect)
    }
    return unionRect
}

fun <T> MutableList<T>.addIf(rect: T, predicate: (T) -> Boolean): Boolean {
    return predicate(rect).also {
        if (it) {
            add(rect)
        }
    }
}

data class TileRect(val rect: Rect, var color: Color) {
    fun toCartesian(centerX: Int, centerY: Int, offsx: Int, offsy: Int, scale: Int): RectF {
        val x = (rect.left - centerX + offsx) / scale.toFloat()
        val y = (rect.top - centerY + offsy) / -scale.toFloat()
        val w = rect.width() / scale.toFloat()
        val h = rect.height() / scale.toFloat()
        return RectF(x, y, x + w, y - h)
    }
}