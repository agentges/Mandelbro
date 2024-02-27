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
        Log.d("tttt", "Drag end on $offsx, $offsy")
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        offsx += dragAmount.x
        offsy += dragAmount.y
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
        w = newWidth
        h = newHeight
        calcDrawingRects()

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


private suspend fun rectDraw(canvas: Canvas, rect: Rect) {
    val paint = Paint().apply {
        color = Color.Red.toArgb()
        strokeWidth = 50f
    }
    canvas.drawColor(Color.Blue.toArgb())
    repeat(1000) {
        delay(500)
        val x= Random.nextFloat()*rect.width()
        val y= Random.nextFloat()*rect.height()
        canvas.drawPoint(x, y, paint)
    }
}
