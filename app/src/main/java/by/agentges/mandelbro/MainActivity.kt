package by.agentges.mandelbro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
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
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

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


        var pointsArray: FloatArray = FloatArray(2 * 1 * 1)
        var colorsArray: IntArray = IntArray(1 * 1)

        val paletteSize = 2048
        val palette = IntArray(paletteSize) {
            val t = it.toFloat() / paletteSize
            Color(
                CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
                CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
            ).toArgb()
        }

        val paletteB = IntArray(paletteSize) {
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

                    //Draw iterative points
                    var sc = 1
                    step = 512
                    while (step > 2) {
                        val scDoneIn = measureTimeMillis {
                            xPoints = ceil(w.toFloat() / step - 0.5f).toInt() + 1
                            yPoints = ceil(h.toFloat() / step - 0.5f).toInt() + 1
                            numPoints = xPoints * yPoints
                            pointsArray = FloatArray(2 * numPoints) { 0f }
                            colorsArray = IntArray(numPoints) { 0 }

                            //calculate
                            fillPoints2(w, h, sc, step, yPoints, xPoints, pointsArray)

                            repeat(yPoints) { y ->
                                repeat(xPoints) { x ->
                                    val index = x + y * xPoints

                                    val re = (pointsArray[2 * index] - centerX) / scale - ofsx
                                    val im = (pointsArray[2 * index + 1] - centerY) / (-scale) - ofsy

                                    val maxIterations = palette.size
                                    val iterations =
                                        countMandelbrotIterations(re, im, maxIterations)

                                    colorsArray[index] = if (iterations == maxIterations) {
                                        Color.Black.toArgb()
                                    } else {
                                        palette[iterations % palette.size]
                                    }


                                }

                                //draw

                                repeat(xPoints) { x ->
                                    val index = x + y * xPoints
                                    //correct
                                    linePaint.strokeWidth = when (sc) {
                                        1 -> step / 1f
                                        else -> step / 2f
                                    }

                                    //experimental
//                                    linePaint.strokeWidth = 2f


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

                            //  delay(500)

                        }

                        Log.d("tttt", " DoneIn: $scDoneIn ms   NewValues: sc=$sc step=$step")
                    }
                    Log.d("tttt", "DONE iters")

                    ////////////////////////////////////////////////////////////////////////////

                    //DRAW each pixel
                    step = 1
                    xPoints = w / step + 1
                    yPoints = h / step + 1
                    numPoints = xPoints * yPoints
                    colorsArray = IntArray(numPoints) { 0 }

                    linePaint.strokeWidth = 1f

                    val splitNumY = 7
                    val tm = measureTimeMillis {
                        repeat(splitNumY) { splitY ->
                            Log.d("tttt", "split ${splitY + 1} of $splitNumY")
                            Log.d("tttt", "calc colors")
                            repeat(yPoints) { pYsplitted ->
                                if (pYsplitted % splitNumY == 0) {
                                    val pY = (pYsplitted + splitY).coerceAtMost(yPoints - 1)
                                    repeat(xPoints) { pX ->
                                        val index = pX + pY * xPoints

                                        val x = pX.toFloat() + 0.5f
                                        val y = pY.toFloat() + 0.5f

                                        val re = (x - centerX) / scale - ofsx
                                        val im = (y - centerY) / (-scale) - ofsy

                                        val maxIterations = paletteB.size
                                        val iterations =
                                            countMandelbrotIterations(re, im, maxIterations)

                                        colorsArray[index] = if (iterations == maxIterations) {
                                            Color.Black.toArgb()
                                        } else {
                                            paletteB[iterations % palette.size]
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
                    }
                }
            }
        }
    }
}

private fun countMandelbrotIterations(
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

private fun fillPoints(
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

            val maxIterations = palette.size

            val iterations = countMandelbrotIterations(re, im, maxIterations)

            colorsArray[index] = if (iterations == maxIterations) {
                Color.Black.toArgb()
            } else {
                palette[iterations % palette.size]
            }
        }
    }
}

private fun fillPoints2(
    w: Int,
    h: Int,
    sc: Int,
    step: Int,
    yPoints: Int,
    xPoints: Int,
    pointsArray: FloatArray,
) {
    val shiftx = when (sc) {
        1 -> 0f
        else -> if ((sc - 1) % 3 == 0) 0f else step / 2f
    }

    val shifty = when (sc) {
        1 -> 0f
        else -> if (sc % 3 == 0) 0f else step / 2f
    }

    val centerX = w / 2
    val centerY = h / 2

    repeat(yPoints) { pY ->
        repeat(xPoints) { pX ->
            val index = pX + pY * xPoints
            val x = shiftx + (pX * step).toFloat()
            val y = shifty + (pY * step).toFloat()

            pointsArray[2 * index] = x
            pointsArray[2 * index + 1] = y
        }
    }
}
