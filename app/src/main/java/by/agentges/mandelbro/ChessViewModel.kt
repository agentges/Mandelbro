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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
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
        calcRects()
    }

    private fun calcRects() {
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
        calcRects()

        if (bitmap != null) return
        viewModelScope.launch(Dispatchers.Default) {
            bitmap?.recycle()
            bitmap = null
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).run {
                bitmap = this
                val c = Canvas(this)
                c.drawColor(Color.Black.toArgb())
                job = launch {
                    drawChessPicture(c)
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

    /**
     * Create a new bitmap using w and h. Paint current bitmap on it using src and dest.
     * Recycle the old bitmap.
     * assign new bitmap to current bitmap.
     */
    private fun remakeBitmap() {
        viewModelScope.launch(Dispatchers.Default) {
            val currentJob = job
            currentJob?.cancel()
            currentJob?.join()

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
                calcRects()
            }
            job = launch { drawChessPicture(newCanvas) }
        }
    }
}


suspend fun drawChessPicture(canvas: Canvas) {
    withContext(Dispatchers.Default) {

        val paint = Paint()
        paint.color = Color.Red.toArgb()
        //paint.strokeWidth = 128f
        val cellSize = 128f

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val isWhite = (row + col) % 2 == 0
                paint.color = if (isWhite) Color.White.toArgb() else Color.Black.toArgb()
                canvas.drawRect(
                    col * cellSize, row * cellSize,
                    (col + 1) * cellSize, (row + 1) * cellSize, paint
                )
                delay(100)
                if (!isActive) break
            }
            if (!isActive) break
        }
    }
}