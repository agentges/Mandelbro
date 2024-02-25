package by.agentges.mandelbro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class ChessViewModel : ViewModel() {

    private var job: Job? = null
    var offsx = 0f
    var offsy = 0f
    var w: Int = 0
    var h: Int = 0

    var fullRect: Rect = Rect()

    val src = Rect()
    val dest = Rect()

    val splitRects = Array(4) { AnchorRect(Rect(), RectCorner.TOP_LEFT) }

    var bitmap: Bitmap? = null


    fun onDragStart(offset: Offset) {
        Log.d("tttt", "Drag start: $offset")
    }

    fun onDragEnd() {
        Log.d("tttt", "Drag end")
        remakeBitmap()
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        offsx += dragAmount.x
        offsy += dragAmount.y
        // Log.d("tttt", "Offsets $offsx, $offsy")
        calcDrawingRects()
    }

    private fun calcDrawingRects() {
        src.set(
            max(0f, -offsx).roundToInt(),
            max(0f, -offsy).roundToInt(),
            (max(0f, -offsx) + (w - offsx.absoluteValue)).roundToInt(),
            (max(0f, -offsy) + (h - offsy.absoluteValue)).roundToInt(),
        )

        dest.set(
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
        Log.d("tttt", "THE rect: $fullRect")
        w = newWidth
        h = newHeight
        calcDrawingRects()

        if (bitmap != null) return
        viewModelScope.launch(Dispatchers.Default) {
            bitmap?.recycle()
            bitmap = null
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).run {
                bitmap = this
                val c = Canvas(this)
                c.drawColor(Color.Black.toArgb())
                job = launch {
                    drawChessPicture(c, AnchorRect(fullRect, RectCorner.TOP_LEFT))
                }
            }
        }
    }


    fun onSurfaceDestroyed() {
        // nothing for now
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("tttt", "Clear bitmap")
        bitmap?.recycle()
        bitmap = null
    }

    fun splitRect(rect: Rect, splitPoint: Point): List<AnchorRect> {
        require(rect.contains(splitPoint.x, splitPoint.y)) { "Split point must be inside the given rect" }
        return listOf(
            AnchorRect(Rect(rect.left, rect.top, splitPoint.x, splitPoint.y), RectCorner.BOTTOM_RIGHT),
            AnchorRect(Rect(splitPoint.x, rect.top, rect.right, splitPoint.y), RectCorner.BOTTOM_LEFT),
            AnchorRect(Rect(rect.left, splitPoint.y, splitPoint.x, rect.bottom), RectCorner.TOP_RIGHT),
            AnchorRect(Rect(splitPoint.x, splitPoint.y, rect.right, rect.bottom), RectCorner.TOP_LEFT)
        )
    }

    /**
     * Find the corner of rect2 that is inside rect1
     */
    private fun findContainingCorner(rect1: Rect, rect2: Rect): Point? {
        getRectCornersAsList(rect2).forEach {
            if (rect1.contains(it.x, it.y)) {
                return it
            }
        }
        return null
    }

    private fun getRectCornersAsList(rect: Rect): List<Point> {
        return listOf(
            Point(rect.left, rect.top),        // Top-left corner
            Point(rect.right, rect.top),       // Top-right corner
            Point(rect.left, rect.bottom),     // Bottom-left corner
            Point(rect.right, rect.bottom)     // Bottom-right corner
        )
    }


    /**
     * Create a new bitmap using w and h. Paint current bitmap on it using src and dest.
     * Recycle the old bitmap.
     * assign new bitmap to current bitmap.
     */
    private fun remakeBitmap() {
        viewModelScope.launch(Dispatchers.Default) {
            job?.cancel()
            job?.join()

            calcSplitRects()

            val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBitmap)
            val bitmapPaint = Paint()
            synchronized(this@ChessViewModel) {
                bitmap?.let {
                    newCanvas.drawColor(Color.Yellow.toArgb())
                    newCanvas.drawBitmap(it, src, dest, bitmapPaint)
                    it.recycle()
                }
                bitmap = newBitmap

                offsx = 0f
                offsy = 0f
                calcDrawingRects()
            }
            job = launch {
                splitRects.forEach {
                    launch { drawChessPicture(newCanvas, it) }
                }
            }
        }
    }

    private fun calcSplitRects() {
        val fullRectWithOffset = Rect(
            fullRect.left + offsx.roundToInt(),
            fullRect.top + offsy.roundToInt(),
            fullRect.right + offsx.roundToInt(),
            fullRect.bottom + offsy.roundToInt(),
        )
        findContainingCorner(fullRect, fullRectWithOffset)?.also { splitPoint ->
            //split rect and remove the one that contains the center of the dest rect
            splitRect(fullRect, splitPoint).filterNot {
                false//it.rect.contains(dest.centerX(), dest.centerY())
            }.forEachIndexed { index, anchorRect ->
                if (index in splitRects.indices) {
                    splitRects[index].set(anchorRect)
                }
            }
        } ?: run {
            //set the first splitRect equal to fullRect, and the rest to Rect(0,0,0,0)
            splitRects[0].rect.set(fullRect)
            splitRects[0].anchorCorner = RectCorner.TOP_LEFT
            for (i in 1 until splitRects.size) {
                splitRects[i].rect.set(0, 0, 0, 0)
            }
        }
    }
}

suspend fun drawChessPicture(canvas: Canvas, anchorRect: AnchorRect) {
    withContext(Dispatchers.Default) {
        try {
            Log.d("tttt", "Start drawing chess board on rect: $anchorRect")
            if (anchorRect.rect.width() == 0 || anchorRect.rect.height() == 0) {
                Log.d("tttt", "Drawing chess board cancelled due to zero width or height")
                return@withContext
            }

            val paint = Paint()
            paint.color = Color.Red.toArgb()

            val pointSize = 128f
            val stepSize = 128f
            paint.strokeWidth = pointSize

            val xPoints = ceil(anchorRect.rect.width().toFloat() / stepSize - 0.5f).toInt() + 1
            val yPoints = ceil(anchorRect.rect.height().toFloat() / stepSize - 0.5f).toInt() + 1

            for (y in 0 until yPoints) {
                for (x in 0 until xPoints) {
                    val isWhite = (y + x) % 2 == 0
                    paint.color = if (isWhite) Color.White.toArgb() else Color.Black.toArgb()

                    val drawX = when (anchorRect.anchorCorner) {
                        RectCorner.TOP_RIGHT, RectCorner.BOTTOM_RIGHT -> anchorRect.rect.right - x * stepSize
                        else -> anchorRect.rect.left + x * stepSize
                    }

                    val drawY = when (anchorRect.anchorCorner) {
                        RectCorner.BOTTOM_LEFT, RectCorner.BOTTOM_RIGHT -> anchorRect.rect.bottom - y * stepSize
                        else -> anchorRect.rect.top + y * stepSize
                    }
                    canvas.drawPoint(drawX, drawY, paint)
                    delay(5)
                    yield()   //???
                    if (!isActive) {
                        Log.d("tttt", "Drawing chess board cancelled x")
                        break
                    }
                }
                if (!isActive) {
                    Log.d("tttt", "Drawing chess board cancelled y")
                    break
                }
            }
            Log.d("tttt", "Done drawing chess board")
        } catch (e: Exception) {
            Log.d("tttt", "Error drawing chess board: ${e.message}")
        }
    }
}

enum class RectCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

data class AnchorRect(
    val rect: Rect,
    var anchorCorner: RectCorner,
) {
    fun set(anchorRect: AnchorRect) {
        rect.set(anchorRect.rect)
        anchorCorner = anchorRect.anchorCorner
    }
}

fun Rect.contains(point: Point): Boolean {
    return contains(point.x, point.y)
}