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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import by.agentges.mandelbro.ui.theme.MandelbroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt
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

//var scale = 500_000
//val ofsx: Double = 1.3726414996685
//val ofsy: Double = -0.08599607739010
//var scale: Double by rememberSaveable { mutableStateOf(29_366_273_430_054_752.0) }
//var ofsx: Double by rememberSaveable { mutableStateOf(0.7478316811474198) }
//var ofsy: Double by rememberSaveable { mutableStateOf(-0.08896944832810841) }


@Composable
fun Painting(modifier: Modifier = Modifier) {
    var scale: Double by rememberSaveable { mutableStateOf(300.0) }
    var ofsx: Double by rememberSaveable { mutableStateOf(0.5) }
    var ofsy: Double by rememberSaveable { mutableStateOf(0.0) }
    var job: Job? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    val palette8 = Palette(2048) { t ->
        Color(
            CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
            CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
            CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
        ).toArgb()
    }

    val pSize = 2048
    val palettes = Array<Palette>(3) {
        when(it){
            0 -> Palette(pSize) { t ->
                Color(
                    CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                ).toArgb()
            }
            1 -> Palette(pSize) { t ->
                Color(
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                    CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                ).toArgb()
            }
            else -> Palette(pSize) { t ->
                Color(
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                    CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
                ).toArgb()
            }
        }
    }


    var bitmap: Bitmap? = null
    var canvas: Canvas? = null
    var w: Int? = null
    var h: Int? = null

    AndroidExternalSurface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (offset.x < w!! / 4 && offset.y > h!! / 4 * 3) {
                            scale /= 1.1
                            Log.d("tttt", "TAP scale:$scale")
                        } else {
                            val re = (offset.x - w!! / 2) / scale - ofsx
                            val im = (offset.y - h!! / 2) / (-scale) - ofsy
                            Log.d("tttt", "TAP offs:$re,$im")

                            ofsx = -re
                            ofsy = -im

                            scale *= 1.1
                        }


                        scope.launch {
                            job?.cancel()
                            job?.join()
                            job = launch { drawPicture(canvas, w ?: 0, h ?: 0, scale, ofsx, ofsy, palettes) }
                        }
                    })
            }

    ) {
        // Resources can be initialized/cached here
        val linePaint = Paint()

        // A surface is available, we can start rendering
        onSurface { surface, width, height ->
            w = width
            h = height
            var fullRect = Rect(0, 0, w ?: 0, h ?: 0)

            if (bitmap == null)
                bitmap = Bitmap.createBitmap(w ?: 0, h ?: 0, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!)


            scope.launch {
                job?.cancel()
                job?.join()
                job = launch { drawPicture(canvas, w ?: 0, h ?: 0, scale, ofsx, ofsy, palettes) }
            }


            // React to surface dimension changes
            surface.onChanged { newWidth, newHeight ->
                w = newWidth
                h = newHeight
                fullRect = Rect(0, 0, w ?: 0, h ?: 0)
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

suspend fun drawPicture(canvas: Canvas?, w: Int, h: Int, scale: Double, ofsx: Double, ofsy: Double, palettes: Array<Palette>) {
    withContext(Dispatchers.Default) {

        val centerX = w / 2
        val centerY = h / 2

        //Draw iterative points
        var pass = 1
        var stepSize = 256
        while (stepSize > 2) {
            val passDuration = measureTimeMillis {
                val points = createPointsForPass(w ?: 0, h ?: 0, stepSize, pass)
                val colorsArray = IntArray(points.pointsNumber) { 0 }

                val lineJobs = Array(points.yNum) { y ->
                    launch {
                        // Calculate line colors
                        for (x in 0 until points.xNum) {
                            if(!isActive) break
                            val index = x + y * points.xNum
                            val re = (points[2 * index] - centerX) / scale - ofsx
                            val im = (points[2 * index + 1] - centerY) / (-scale) - ofsy

                            val iterationsPercent = countMandelbrotIterations(re, im)
                            colorsArray[index] = if (iterationsPercent == 1f) {
                                Color.Black.toArgb()
                            } else {
                                palettes[2][iterationsPercent]
                            }
                        }


                        //Draw line
                        val lPaint = Paint()

                        for (x in 0 until points.xNum) {
                            if(!isActive) break
                            val index = x + y * points.xNum

                            lPaint.strokeWidth = when (pass) {
                                1 -> stepSize / 1f
                                else -> stepSize / 2f
                            }

                            lPaint.color = colorsArray[index]
                            canvas?.drawPoint(points[2 * index], points[2 * index + 1], lPaint)
                        }
                    }
                }
                lineJobs.forEach { it.join() }
            }
            if (stepSize < 64) {
                Log.d("tttt", "Pass=$pass step=$stepSize>>> DoneIn: $passDuration ms")
            }
            //update iteration variables
            pass++
            if (pass > 4 && (pass - 2) % 3 == 0) {
                stepSize /= 2
            }

            //  delay(1000)
        }
        val scaleStr = String.format("%.10f", scale)
        Log.d("tttt", "DONE iterative drawing ofsx=$ofsx ofsy=$ofsy scale=$scaleStr")
        //canvas?.drawColor(Color.Red.toArgb())
//        drawPalette(w, h, canvas!!, palette)
//        drawGuidelines(canvas, w, h, centerX, centerY, scale, ofsx, ofsy)
    }
}

private fun drawGuidelines(
    canvas: Canvas,
    w: Int,
    h: Int,
    centerX: Int,
    centerY: Int,
    scale: Double,
    ofsx: Double,
    ofsy: Double
) {
    canvas.drawLine(
        0f,
        h / 4 * 3f,
        w / 4f,
        h / 4 * 3f,
        Paint().apply { color = Color.White.toArgb() })
    canvas.drawLine(
        w / 4f,
        h / 4 * 3f,
        w / 4f,
        h.toFloat(),
        Paint().apply { color = Color.White.toArgb() })

    repeat(3) {
        val xx = (it - 1).toFloat()
        val ax = centerX + (xx * scale + ofsx).toFloat()
        canvas.drawLine(
            ax,
            0f,
            ax,
            h.toFloat(),
            Paint().apply { color = Color.White.toArgb() })
    }

    repeat(3) {
        val yy = (it - 1).toFloat()
        val ay = centerY - (yy * scale + ofsy).toFloat()
        canvas.drawLine(
            0f,
            ay,
            w.toFloat(),
            ay,
            Paint().apply { color = Color.White.toArgb() })
    }
}

/**
 * Count the number of iterations for a point in the Mandelbrot set.
 * Returns the percentage of iterations that were completed before the point escaped.
 * 1.0 means the point did not escape after maxIterations.
 * Other values are the percentage of iterations that were completed before the point escaped.
 */
private fun countMandelbrotIterations(
    cRe: Double,
    cIm: Double,
    maxIterations: Int = 2000,
): Float {
    var iterations = 0
    var zR = cRe
    var zI = cIm
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
    return if (iterations == maxIterations) 1f else iterations.toFloat() / maxIterations
}


/**
 * Create points for a pass in screen coordinates
 * @param w width of the canvas
 * @param h height of the canvas
 * @param stepSize size of the step
 * @param pass pass number
 * @return array of points
 */
fun createPointsForPass(w: Int, h: Int, stepSize: Int, pass: Int): PassPoints {
    val xPoints = ceil(w.toFloat() / stepSize - 0.5f).toInt() + 1
    val yPoints = ceil(h.toFloat() / stepSize - 0.5f).toInt() + 1
    val numPoints = xPoints * yPoints
    //Log.d("tttt", "Create points For Pass : $xPoints x $yPoints = $numPoints")
    val points = FloatArray(2 * numPoints) { 0f }

    val shiftx = when (pass) {
        1 -> 0f
        else -> if ((pass - 1) % 3 == 0) 0f else stepSize / 2f
    }

    val shifty = when (pass) {
        1 -> 0f
        else -> if (pass % 3 == 0) 0f else stepSize / 2f
    }

    repeat(yPoints) { pY ->
        repeat(xPoints) { pX ->
            val index = pX + pY * xPoints
            val x = shiftx + (pX * stepSize).toFloat()
            val y = shifty + (pY * stepSize).toFloat()

            points[2 * index] = x
            points[2 * index + 1] = y
        }
    }

    return PassPoints(xPoints, yPoints, points)
}


class PassPoints(val xNum: Int, val yNum: Int, private val points: FloatArray) {
    val pointsNumber = xNum * yNum
    operator fun get(index: Int): Float = points[index]
}

class Palette(size: Int, init: (Float) -> Int) {

    private val colors = IntArray(size) {
        init(it.toFloat() / (size - 1))
    }

    val size = colors.size

    operator fun get(t: Float) = colors[(t * colors.lastIndex).roundToInt()]
    operator fun get(index: Int) = colors[index]
}

fun drawPalette(w: Int, h: Int, canvas: Canvas, palette: Palette) {

    val paint = Paint()
    val palw = w / 20
    val palh = h / 3
    repeat(palh) {
        val t = it / palh.toFloat()
        val c = palette[t]
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

fun IntArray.toStr(): String {
    var s = ""
    for (i1 in this) {
        s += "$i1,"
    }
    return "[$s]"
}