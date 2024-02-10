package by.agentges.mandelbro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import by.agentges.mandelbro.ui.theme.MandelbroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MandelbroTheme {
                Painting()
            }
        }
    }
}

fun Surface.useCanvas(inOutDirty: Rect, block: Canvas.() -> Unit) {
    val canvas = lockCanvas(inOutDirty)
    try {
        block(canvas)
    } finally {
        unlockCanvasAndPost(canvas)
    }
}

fun sinease(t: Float): Float {
    return CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t)

    //return t
    //return t * t * (3.0f - 2.0f * t);
    //val sqr = t * t


}

//val scale = 420_000
//val ofsx = 0.7467f
//val ofsy = -0.1025f
val scale = 2_000_000
val ofsx = 0.7470f
val ofsy = -0.1010f

@Composable
fun Painting(modifier: Modifier = Modifier) {
    AndroidExternalSurface(
        modifier = Modifier
            .fillMaxSize()

    ) {
        // Resources can be initialized/cached here

        val linePaint = Paint().apply {
            color = Color.Red.toArgb()
            strokeWidth = 10f
        }


        var pointsArray: FloatArray = FloatArray(2 * 100 * 100)
        var colorsArray: IntArray = IntArray(100 * 100)

        val paletteSize = 2048
        val palette = IntArray(paletteSize) {
            val t = it.toFloat() / paletteSize
            Color(
                CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
            ).toArgb()
        }


        var bitmap: Bitmap? = null

        // A surface is available, we can start rendering
        onSurface { surface, width, height ->
            var w = width
            var h = height
            var fullRect = Rect(0, 0, w, h)
            var step = 1
            var xPoints = w / step + 1
            var yPoints = h / step + 1
            var numPoints = xPoints * yPoints
            pointsArray = FloatArray(2 * numPoints) { 0f }
            colorsArray = IntArray(numPoints) { 0 }

            val centerX = w / 2
            val centerY = h / 2


            launch {
                withContext(Dispatchers.Default) {

                    if (bitmap == null)
                        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap!!)

                    linePaint.strokeWidth = 1f


                    Log.d("tttt", "calc ALL points")
                    repeat(yPoints) { pY ->
                        repeat(xPoints) { pX ->
                            val index = pX + pY * xPoints
                            val x = pX.toFloat() + 0.5f
                            val y = pY.toFloat() + 0.5f
                            pointsArray[2 * index] = (x - centerX) / scale - ofsx
                            pointsArray[2 * index + 1] = (y - centerY) / (-scale) - ofsy
                        }
                    }

                    val splitNum = 4
                    val tm = measureTimeMillis {
                        repeat(splitNum) { split ->
                            Log.d("tttt", "split ${split+1} of $splitNum")

                            Log.d("tttt", "calc colors")
                            repeat(yPoints) { pYsplitted ->
                                if (pYsplitted % splitNum == 0) {
                                    val pY = (pYsplitted + split).coerceAtMost(yPoints - 1)
                                    repeat(xPoints) { pX ->
                                        val index = pX + pY * xPoints

                                        val re = pointsArray[2 * index]
                                        val im = pointsArray[2 * index + 1]

                                        val maxIterations = palette.size

                                        val iterations = countIterations(re, im, maxIterations)

                                        colorsArray[index] = if (iterations == maxIterations) {
                                            Color.Black.toArgb()
                                        } else {
                                            palette[iterations % palette.size]
                                        }
                                    }


                                    //drawPalette(w, h, canvas, palette)

                                    repeat(xPoints) { x ->
                                        val index = x + pY * xPoints

                                        linePaint.color = colorsArray[index]


                                        canvas.drawPoint(
                                            x.toFloat() + 0.5f,
                                            pY.toFloat() + 0.5f,
                                            linePaint,
                                        )
                                    }


                                }
                            }
                        }
                    }
                    Log.d("tttt", "ALL DONE in $tm") //117 with Complex | 42 - no complex
                }
            }


            /*
                        launch {
                            withContext(Dispatchers.Default) {

                                var sc = 1
                                step = 128
                                while (step > 32) {
                                    val scDoneIn = measureTimeMillis {
                                        xPoints = ceil(w.toFloat() / step - 0.5f).toInt() + 1
                                        yPoints = ceil(h.toFloat() / step - 0.5f).toInt() + 1
                                        numPoints = xPoints * yPoints
                                        pointsArray = FloatArray(2 * numPoints) { 0f }
                                        colorsArray = IntArray(numPoints) { 0 }

                                        fillPoints(
                                            w,
                                            h,
                                            sc,
                                            step,
                                            yPoints,
                                            xPoints,
                                            pointsArray,
                                            colorsArray,
                                            palette
                                        )

                                        if (bitmap == null)
                                            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bitmap!!)

                                        repeat(yPoints) { y ->
                                            repeat(xPoints) { x ->
                                                val index = x + y * xPoints
                                                //correct
                                                linePaint.strokeWidth = when (sc) {
                                                    1 -> step / 1f
                                                    else -> step / 2f
                                                }

                                                //experimental
                                                //linePaint.strokeWidth = 2f


                                                linePaint.color = colorsArray[index]
                                                canvas.drawPoint(
                                                    pointsArray[2 * index],
                                                    pointsArray[2 * index + 1],
                                                    linePaint
                                                )
                                            }
                                        }
                                        //drawPalette(w, h, canvas, palette)

                                        //update iteration variables
                                        sc++
                                        if (sc > 4 && (sc - 2) % 3 == 0) {
                                            step /= 2
                                        }

                                        //  delay(1000)

                                    }

                                    Log.d("tttt", " DoneIn: $scDoneIn ms   NewValues: sc=$sc step=$step")
                                }
                                Log.d("tttt", "DONE iters")

                                //draw each pixel
                                step = 1
                                val tm = measureTimeMillis {
                                    xPoints = ceil(w.toFloat() / step - 0.5f).toInt() + 1
                                    yPoints = ceil(h.toFloat() / step - 0.5f).toInt() + 1
                                    numPoints = xPoints * yPoints
                                    pointsArray = FloatArray(2 * numPoints) { 0f }
                                    colorsArray = IntArray(numPoints) { 0 }

                                    Log.d("tttt", "xstart fillALLPoints")

                                    /////---------FILL and ddraw ALL POINTS----------------------

                                    if (bitmap == null)
                                        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap!!)

                                    linePaint.strokeWidth = 1f

                                    val splitNum = 4
                                    repeat(splitNum) { split ->
                                        Log.d("tttt", "split $split of $splitNum")
                                        repeat(yPoints) { pYsplitted ->
                                            if (pYsplitted % splitNum == 0) {
                                                val pY = (pYsplitted + split).coerceAtMost(yPoints - 1)
                                                repeat(xPoints) { pX ->
                                                    val index = pX + pY * xPoints
                                                    val x = pX.toFloat() + 0.5f
                                                    val y = pY.toFloat() + 0.5f

                                                    pointsArray[2 * index] = x
                                                    pointsArray[2 * index + 1] = y


                                                    val centerX = w / 2
                                                    val centerY = h / 2


                                                    val re = (x - centerX) / scale - ofsx
                                                    val im = (y - centerY) / (-scale) - ofsy
                                                    val c = re + im * i
                                                    var z = c

                                                    colorsArray[index] = Color.Black.toArgb()
                                                    repeat(palette.size) {
                                                        z = z * z + c
                                                        if (z.abs() > 2) {
                                                            colorsArray[index] = palette[it % palette.size]
                                                            return@repeat
                                                        }
                                                    }
                                                }

                                                repeat(xPoints) { x ->
                                                    val index = x + pY * xPoints

                                                    linePaint.color = colorsArray[index]

                                                    canvas.drawPoint(
                                                        pointsArray[2 * index],
                                                        pointsArray[2 * index + 1],
                                                        linePaint
                                                    )
                                                }
                                            }
                                        }
                                    }                        /////------------------------


                                    */
            /*
                                    drawallpoints


                                    repeat(yPoints) { y ->
                                        repeat(xPoints) { x ->
                                            val index = x + y * xPoints

                                            linePaint.color = colorsArray[index]

                                            canvas.drawPoint(
                                                pointsArray[2 * index],
                                                pointsArray[2 * index + 1],
                                                linePaint
                                            )
                                        }
                                    }*//*


                        */
            /*
                                                            repeat(yPoints) { y ->
                                                                repeat(xPoints) { x ->
                                                                    val index = x + y * xPoints
                                                                    //correct
                                                                    linePaint.strokeWidth = 64f


                                                                    linePaint.color = colorsArray[index]
                                                                    canvas.drawPoint(
                                                                        pointsArray[2 * index],
                                                                        pointsArray[2 * index + 1],
                                                                        linePaint
                                                                    )
                                                                }
                                                            }

                                     *//*


                    }
                    Log.d("tttt", " DoneALLpixelsIn: $tm ms")

                }
            }
*/


            // React to surface dimension changes
            surface.onChanged { newWidth, newHeight ->
                w = newWidth
                h = newHeight
                fullRect = Rect(0, 0, w, h)
            }

            // Cleanup if needed
            surface.onDestroyed {
                bitmap?.recycle()
                bitmap = null
            }


            // Render loop, automatically cancelled on surface destruction
            while (true) {
                withFrameNanos { time ->

                    surface.useCanvas(fullRect) {
                        drawColor(Color.Black.toArgb())
                        linePaint.strokeWidth = 1f

                        bitmap?.let {
                            drawBitmap(it, 0f, 0f, linePaint)
                        }


                        //drawPoints(pointsArray, linePaint)


                    }


                    /*
                                        surface.useCanvas(fullRect) {


                                            drawColor(Color.Black.toArgb())
                                            val timeMs = time / 1_000_000L
                                            val t = 0.5f + 0.5f * sin(timeMs / 1_000.0f)
                                            //drawColor(lerp(Color.Blue, Color.Green, t).toArgb())
                                            //  Log.d("eee->","$timeMs")
                                            val x = t * w
                                            val y = h / 2f
                                            linePaint.color = Color.Green.toArgb()
                                            linePaint.strokeWidth = 200f
                                            drawPoint(x, y, linePaint)

                                            drawPoints(pointsArray, linePaint)
                                            //drawPicture(picture)
                                        }
                                        */
                }
            }
        }
    }
}

private fun countIterations(
    cRe: Float,
    cIm: Float,
    maxIterations: Int,
): Int {
    var iterations = 0
    var zR = cRe.toDouble()
    var zI = cIm.toDouble()
    while (iterations < maxIterations) {
        val zRp = zR
        zR = zRp * zRp - zI * zI + cRe
        zI = zRp * zI + zI * zRp + cIm
        val zAbs: Double = sqrt(zR * zR + zI * zI)

        iterations++
        if (zAbs > 2) {
            break
        }
    }
    return iterations
}

fun drawPalette(w: Int, h: Int, canvas: Canvas, palette: IntArray) {

    val paint = Paint()
    val palw = w / 20
    val palh = h / 3
    repeat(palh) {
        val t = it / palh.toFloat()
        val c = palette[(t * palette.size).toInt()]
        canvas.drawLine(
            0f,
            it.toFloat(),
            palw.toFloat(),
            it.toFloat(),
            paint.apply {
                color = c
            }
        )
    }
}

private suspend fun fillPoints(
    w: Int,
    h: Int,
    sc: Int,
    step: Int,
    yPoints: Int,
    xPoints: Int,
    pointsArray: FloatArray,
    colorsArray: IntArray,
    palette: IntArray,
) {
    val shiftx = when (sc) {
        1 -> 0f
        else -> if ((sc - 1) % 3 == 0) 0f else step / 2f
    }

    val shifty = when (sc) {
        1 -> 0f
        else -> if (sc % 3 == 0) 0f else step / 2f
    }

    repeat(yPoints) { pY ->
        repeat(xPoints) { pX ->
            val index = pX + pY * xPoints
            val x = shiftx + (pX * step).toFloat()
            val y = shifty + (pY * step).toFloat()

            pointsArray[2 * index] = x
            pointsArray[2 * index + 1] = y

            val centerX = w / 2
            val centerY = h / 2

            val re = (x - centerX) / scale - ofsx
            val im = (y - centerY) / (-scale) - ofsy
            val c = re + im * i
            var z = c

            colorsArray[index] = Color.Black.toArgb()
            repeat(palette.size) {
                z = z * z + c
                if (z.abs() > 2) {
                    colorsArray[index] = palette[it % palette.size]
                    return@repeat
                }
            }
        }
    }
}

private fun fillALLPoints(
    w: Int,
    h: Int,
    yPoints: Int,
    xPoints: Int,
    pointsArray: FloatArray,
    colorsArray: IntArray,
    palette: IntArray,
) {
    Log.d(
        "tttt",
        "w = $w h = $h xPoints = $xPoints yPoints = $yPoints, pointsArray.size = ${pointsArray.size} colorsArray.size = ${colorsArray.size} palette.size = ${palette.size}"
    )


    repeat(yPoints) { pY ->
        if (pY % 50 == 0) {
            Log.d("tttt", "pY= $pY")
        }

        repeat(xPoints) { pX ->
            val index = pX + pY * xPoints
            val x = pX.toFloat() + 0.5f
            val y = pY.toFloat() + 0.5f

            pointsArray[2 * index] = x
            pointsArray[2 * index + 1] = y

            val scale = 165_000
            val centerX = w / 2
            val centerY = h / 2

            val ofsx = 1.2809f
            val ofsy = -0.061f

            val re = (x - centerX) / scale - ofsx
            val im = (y - centerY) / (-scale) - ofsy
            val c = re + im * i
            var z = c

            colorsArray[index] = Color.Black.toArgb()
            repeat(palette.size) {
                z = z * z + c
                if (z.abs() > 2) {
                    colorsArray[index] = palette[it % palette.size]
                    return@repeat
                }
            }
        }
    }
}
