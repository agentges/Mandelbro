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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var oldBitmapRect = Rect()
    var bitmapRect = Rect()

    val tileRects = mutableListOf<TileRect>()

    fun onDragStart(offset: Offset) {
        Log.d("tttt", "Drag start: $offset")
    }

    fun onDragEnd() {
        Log.d("tttt", "Drag end on $offsx, $offsy")

        recalculateBitmapRect()

        //recreate bitmap
        remakeBitmap()

        //apply tile rects to bimap???
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        synchronized(offLock) {
            val dx = dragAmount.x.roundToInt()
            val dy = dragAmount.y.roundToInt()
            offsx += dx
            offsy += dy
            bitmapRect.offset(dx, dy)
            calcBitmapDrawingRects()
            offsetTileRects(dx, dy)
            recalcTileRects()
        }
    }

    private fun offsetTileRects(dx: Int, dy: Int) {
        tileRects.forEach {
            it.rect.offset(dx, dy)
        }
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
        val srcX = max(0, fullRect.left - bitmapRect.left)
        val srcY = max(0, fullRect.top - bitmapRect.top)

        val intersectionRect = Rect().apply {
            setIntersect(bitmapRect, fullRect)
        }
        val srcW = intersectionRect.width()
        val srcH = intersectionRect.height()

        src.set(
            srcX,
            srcY,
            srcX + srcW,
            srcY + srcH,
        )

        val dstX = max(0, bitmapRect.left - fullRect.left) + fullRect.left
        val dstY = max(0, bitmapRect.top - fullRect.top) + fullRect.top

        dst.set(
            dstX,
            dstY,
            dstX + srcW,
            dstY + srcH,
        )
    }

    /**
     * Create a new bitmap using bitmapRect. Paint current bitmap on it using src and dest.
     * Recycle the old bitmap.
     * assign new bitmap to current bitmap.
     */
    private fun remakeBitmap() {
        viewModelScope.launch(Dispatchers.Default) {
            job?.cancelAndJoin()

            ////

            val srcX = max(0, bitmapRect.left - oldBitmapRect.left)
            val srcY = max(0, bitmapRect.top - oldBitmapRect.top)

            val intersectionRect = Rect().apply {
                setIntersect(oldBitmapRect, bitmapRect)
            }
            val srcW = intersectionRect.width()
            val srcH = intersectionRect.height()

            val dstX = max(0, oldBitmapRect.left - bitmapRect.left)
            val dstY = max(0, oldBitmapRect.top - bitmapRect.top)

            val bSrc = Rect(srcX, srcY, srcX + srcW, srcY + srcH)
            val bDst = Rect(dstX, dstY, dstX + srcW, dstY + srcH)


            // log oldbitmaprect, bitmap rect, bsrc, bdst
            Log.d("tttt", "oldBitmapRect: $oldBitmapRect bitmapRect: $bitmapRect bSrc: $bSrc bDst: $bDst")


            ////


            val newBitmap = Bitmap.createBitmap(bitmapRect.width(), bitmapRect.height(), Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBitmap)
            val bitmapPaint = Paint()
            synchronized(this@RectViewModel) {
                bitmap?.let {
                    //  newCanvas.drawColor(Color.Yellow.toArgb())
                    newCanvas.drawBitmap(it, bSrc, bDst, bitmapPaint)
                    it.recycle()
                }
                bitmap = newBitmap

                calcBitmapDrawingRects()
            }

            val tileRectsToDraw = createTileRectsToDraw()
            job = launch {
                rectDraw(newCanvas, bitmapRect, tileRectsToDraw)
            }
        }
    }

    fun onSurfaceCreated(width: Int, height: Int) {
        onSurfaceSizeChanged(width, height)
    }

    fun onSurfaceSizeChanged(newWidth: Int, newHeight: Int) {

        val oldRect = Rect(fullRect)

        fullRect.set(0, 0, newWidth, newHeight)
        // reduce fullRect a bit just for test purposes
        fullRect.inset(fullRect.width() / 8, fullRect.height() / 8)

        val centerOffsetX = fullRect.centerX() - oldRect.centerX()
        val centerOffsetY = fullRect.centerY() - oldRect.centerY()

        offsetTileRects(centerOffsetX, centerOffsetY)
        recalcTileRects()

        recalculateBitmapRect()
        calcBitmapDrawingRects()

        if (bitmap != null) return
        viewModelScope.launch(Dispatchers.Default) {
            bitmap?.recycle()
            bitmap = null
            Bitmap.createBitmap(bitmapRect.width(), bitmapRect.height(), Bitmap.Config.ARGB_8888).run {
                bitmap = this
                val canvas = Canvas(this)
                canvas.drawColor(Color.Black.toArgb())
                val tileRectsToDraw = createTileRectsToDraw()
                job = launch {
                    rectDraw(canvas, bitmapRect, tileRectsToDraw)
                }
            }
        }
    }

    private fun createTileRectsToDraw(): List<TileRect> {
        val tileRectsToDraw = tileRects.filter { bitmapRect.containsOrIntersects(it.rect) }
            .map { TileRect(Rect(it.rect), it.color) }
            .onEach { it.rect.offset(-bitmapRect.left, -bitmapRect.top) }
        return tileRectsToDraw
    }

    private fun recalculateBitmapRect() {
        oldBitmapRect = bitmapRect
        bitmapRect = tileRects.filter { fullRect.containsOrIntersects(it.rect) }.union()
    }

    fun onSurfaceDestroyed() {
        // nothing for now
    }

    override fun onCleared() {
        super.onCleared()
        bitmap?.recycle()
        bitmap = null
    }

    /**
     * Convert rect in BitmapRect coordinates to screen rect coordinates
     */
    fun bitmapRectToScreen(bRect: Rect): Rect {
        return Rect(bRect).apply {
            offset(bitmapRect.left, bitmapRect.top)
        }
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

private suspend fun rectDraw(canvas: Canvas, rect: Rect, tileRects: List<TileRect>) {
    val pointPaint = Paint().apply {
        color = Color.Red.copy(alpha=0.1f).toArgb()
        strokeWidth = 16f
    }

    val textPaint = Paint().apply {
        color = Color.White.toArgb()
        textSize = 32f
    }



    tileRects.forEach {
        delay(20)
        //canvas.drawPoint(it.rect.left.toFloat(), it.rect.top.toFloat(), pointPaint)
        canvas.drawCircle(it.rect.centerX().toFloat(), it.rect.centerY().toFloat(), it.rect.width() / 2f, pointPaint)

        //bitmapRectToScreen()
        Rect(it.rect).apply { offset(rect.left, rect.top) }
        
        val s = "\uD83D\uDE0E"
        canvas.drawText(s, it.rect.centerX().toFloat(), it.rect.centerY().toFloat(), textPaint)
    }
}

private fun createTileRects(initialRect: Rect, targetRect: Rect, tileSize: Int): List<TileRect> {
    val result = mutableListOf<TileRect>()
    val waveRect = Rect(initialRect)
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
    return when {
        isEmpty -> false
        contains(r) -> true
        Rect.intersects(this, r) -> true
        else -> false
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
@Deprecated("Use Paint.use instead")
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
