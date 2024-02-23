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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt

class ChessViewModel : ViewModel() {

    var offsx = 0f
    var offsy = 0f
    var w: Int = 0
    var h: Int = 0
    var rect: Rect = Rect()
    var fullRect: Rect = Rect()

    val src = Rect()
    val dest = Rect()

    private var canvas: Canvas? = null
    private val dotPaint = Paint()

    var bitmap: Bitmap? = null


    fun onDragStart(offset: Offset) {
        Log.d("tttt", "Drag start: $offset")
    }

    fun onDragEnd() {
        Log.d("tttt", "Drag end")
    }

    fun onDragCancel() {
        Log.d("tttt", "Drag cancel")
    }

    fun onDrag(dragAmount: Offset) {
        offsx += dragAmount.x
        offsy += dragAmount.y
       // Log.d("tttt", "Offsets $offsx, $offsy")
        //rect.offsetTo(offsx.roundToInt(), offsy.roundToInt())
        rect.left = -offsx.roundToInt()
        rect.top = -offsy.roundToInt()
        rect.right = (offsx + w!!).roundToInt()
        rect.bottom = (offsy + h!!).roundToInt()
//        Log.d("tttt", "New rect: $rect")

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
        rect = Rect(0, 0, newWidth, newHeight)
        fullRect = Rect(0, 0, newWidth, newHeight)
        Log.d("tttt", "THE rect: $rect")
        w = newWidth
        h = newHeight
        calcRects()

        if (bitmap != null) return
        viewModelScope.launch {
            bitmap?.recycle()
            bitmap = null
            canvas = (bitmap ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)).run {
                bitmap = this
                Canvas(this)

            }
            drawChessPicture(canvas!!, dotPaint)
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


}


suspend fun drawChessPicture(canvas: Canvas, paint: Paint) {
    withContext(Dispatchers.Default) {
        canvas.drawColor(Color.Black.toArgb())

        paint.color = Color.Red.toArgb()
        paint.strokeWidth = 128f

        repeat(10) {
            canvas.drawPoint(it * 100f, it * 200f, paint)
            delay(3000)
        }
        repeat(10) {
            canvas.drawPoint(1000 - it * 100f, it * 200f, paint)
            delay(3000)
        }

    }
}