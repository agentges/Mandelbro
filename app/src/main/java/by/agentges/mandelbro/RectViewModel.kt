package by.agentges.mandelbro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.annotation.ColorInt
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

    private val tileRectSize = 128

    private var job: Job? = null
    val offLock = Object()
    var offsx = 0
    var offsy = 0
    var scale = 128

    val fullRect = Rect()

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

        logTileRects()
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        synchronized(offLock) {
            offsx += dragAmount.x.roundToInt()
            offsy += dragAmount.y.roundToInt()
            calcBitmapDrawingRects()
            offsetTileRects(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
            recalcTileRects()
        }
    }

    private fun offsetTileRects(dx: Int, dy: Int) {
        tileRects.forEach {
            it.rect.offset(dx, dy)
        }
    }

    private fun logTileRects() {
        tileRects.forEachIndexed { index, tileRect -> Log.d("tttt", "Tile[$index]: ${tileRect.rect}") }
    }

    /**
     * Remove tiles that are not visible
     * Add new tiles that are visible
     */
    private fun recalcTileRects() {
        val unionRect = tileRects.union()

        if (unionRect.contains(fullRect)) {
            return
        }

        var count = tileRects.size
        removeInvisibleTileRects()
        val removed = count - tileRects.size
        count = tileRects.size

        //color markers for tests
        tileRects.forEach { it.color = Color.Yellow }
        tileRects.firstOrNull()?.let { it.color = Color.Blue }

        createTileRects(
            initialRect = if (unionRect.isEmpty)
                Rect(fullRect.centerX(), fullRect.centerY(), fullRect.centerX(), fullRect.centerY())
            else
                unionRect,
            targetRect = fullRect,
            tileSize = tileRectSize
        ).let {
            tileRects.addAll(it)
        }

        val added = tileRects.size - count
        Log.d("tttt", "Removed $removed, added $added")
    }

    private fun removeInvisibleTileRects() {
        tileRects.removeAll { fullRect.containsOrIntersects(it.rect).not() }
    }

    private fun calcBitmapDrawingRects() {
        src.set(
            max(0, -offsx),
            max(0, -offsy),
            (max(0, -offsx) + (fullRect.width() - offsx.absoluteValue)),
            (max(0, -offsy) + (fullRect.height() - offsy.absoluteValue)),
        )

        dst.set(
            max(0, offsx),
            max(0, offsy),
            (max(0, offsx) + (fullRect.width() - offsx.absoluteValue)),
            (max(0, offsy) + (fullRect.height() - offsy.absoluteValue)),
        )
    }

    fun onSurfaceCreated(width: Int, height: Int) {
        onSurfaceSizeChanged(width, height)
    }

    fun onSurfaceSizeChanged(newWidth: Int, newHeight: Int) {

        val oldRect = Rect(fullRect)

        fullRect.set(0, 0,   newWidth,  newHeight)
        // reduce fullRect a bit just for test purposes
        fullRect.inset(fullRect.width() / 8, fullRect.height() / 8)

        val centerOffsetX = fullRect.centerX() - oldRect.centerX()
        val centerOffsetY = fullRect.centerY() - oldRect.centerY()

        offsetTileRects(centerOffsetX, centerOffsetY)
        recalcTileRects()

        calcBitmapDrawingRects()

        if (bitmap != null) return
        viewModelScope.launch(Dispatchers.Default) {
            bitmap?.recycle()
            bitmap = null
            Bitmap.createBitmap(fullRect.width(), fullRect.height(), Bitmap.Config.ARGB_8888).run {
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

    fun screenRectToCartesian(screenRect: Rect): RectF {
        val x = (screenRect.left - fullRect.centerX() - offsx) / scale.toFloat()
        val y = (screenRect.top - fullRect.centerY() - offsy) / -scale.toFloat()
        val w = screenRect.width() / scale.toFloat()
        val h = screenRect.height() / scale.toFloat()
        return RectF(x, y, x + w, y - h)
    }

    fun cartesianRectToScreen(cartesianRect: RectF): Rect {
        val x = (cartesianRect.left * scale) + fullRect.centerX() + offsx
        val y = (cartesianRect.top * -scale) + fullRect.centerY() + offsy
        val w = (cartesianRect.width() * scale).toInt()
        val h = (cartesianRect.height() * scale).toInt()
        return Rect(x.roundToInt(), y.roundToInt(), x.roundToInt() + w, y.roundToInt() + h)
    }

    fun screenPointToCartesian(screenPoint: Point): PointF {
        val x = (screenPoint.x - fullRect.centerX() - offsx) / scale.toFloat()
        val y = (screenPoint.y - fullRect.centerY() - offsy) / -scale.toFloat()
        return PointF(x, y)
    }

    fun cartesianPointToScreen(cartesianPoint: PointF): Point {
        val x = (cartesianPoint.x * scale) + fullRect.centerX() + offsx
        val y = (cartesianPoint.y * -scale) + fullRect.centerY() + offsy
        return Point(x.roundToInt(), y.roundToInt())
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
        delay(200)
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
    fun draw(canvas: Canvas, restoreablePaint: RestoreablePaint) {
        restoreablePaint.usePaint(
            color = color.toArgb(),
        ) {
            canvas.drawRect(rect, it)
            canvas.drawCircle(rect.centerX().toFloat(), rect.centerY().toFloat(), rect.width() / 2f, it)
        }
    }
}


/**
 * A class to save and restore paint parameters
 */
class RestoreablePaint(private val paint: Paint) {

    @ColorInt
    private var oldColor: Int = paint.color
    private var oldStrokeWidth: Float = paint.strokeWidth
    private var oldStyle: Paint.Style = paint.style

    fun usePaint(
        @ColorInt color: Int = paint.color,
        strokeWidth: Float = paint.strokeWidth,
        style: Paint.Style = paint.style,
        block: (Paint) -> Unit
    ) {
        savePaintParams()
        try {
            setPaintParams(color, strokeWidth, style)
            block(paint)
        } finally {
            restorePaintParams()
        }
    }

    private fun setPaintParams(@ColorInt color: Int, strokeWidth: Float, style: Paint.Style) {
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.style = style
    }

    private fun restorePaintParams() {
        paint.color = oldColor
        paint.strokeWidth = oldStrokeWidth
        paint.style = oldStyle
    }

    private fun savePaintParams() {
        oldColor = paint.color
        oldStrokeWidth = paint.strokeWidth
        oldStyle = paint.style
    }

}

fun Paint.use(
    @ColorInt color: Int = this.color,
    strokeWidth: Float = this.strokeWidth,
    style: Paint.Style = this.style,
    block: (Paint) -> Unit
) {
    this.color = color
    this.strokeWidth = strokeWidth
    this.style = style
    block(this)
}
