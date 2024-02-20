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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.log2
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

@Volatile
var tapHandled: Boolean = true

@Volatile
var stepRowsDone = AtomicInteger()

@Volatile
var stepRowsCount = AtomicInteger()

//iterative drawing ofsx=0.7371992563215526 ofsy=-0.13215904203621656 scale=37926437.2436581550

@Composable
fun Painting(modifier: Modifier = Modifier) {
    var ofsx: Double by rememberSaveable { mutableStateOf(0.7371992563215526) }
    var ofsy: Double by rememberSaveable { mutableStateOf(-0.13215904203621656) }
    var scale: Double by rememberSaveable { mutableStateOf(37926437.2436581550) }
    var job: Job? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    val palette8 = Palette(4096) { t ->
        Color(
            CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
            CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
            CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
        ).toArgb()
    }

    val pSize = 10_000
    val palettes = Array<Palette>(10) {
        when (it) {
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
                    CubicBezierEasing(0f, 0.5f, 0.1f, 1f).transform(t),
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                ).toArgb()
            }

            2 -> Palette(pSize) { t ->
                Color(
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                    CubicBezierEasing(0.0f, 0.0f, 0.0f, 0.0f).transform(t),
                    CubicBezierEasing(0f, 0f, 0.1f, 1f).transform(t),
                ).toArgb()
            }

            3 -> Palette(pSize) { t ->
                Color(
                    1f - t,
                    1f - t,
                    1f - t,
                ).toArgb()
            }

            4 -> Palette(pSize) { t ->
                Color.hsv(
                    t * 360f,
                    1f,
                    1f,
                ).toArgb()
            }

            else -> Palette(pSize) { Color.Red.toArgb() }
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
                //detectDragGestures { change, dragAmount ->
//                    Log.d("tttt", "change: $change dragAmount: $dragAmount")
//                }
                detectTapGestures(
                    onTap = { offset ->
                        if (!tapHandled) {
                            Log.d("tttt", "TAP not handled")
                            return@detectTapGestures
                        }


                        tapHandled = false
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
                            val relaunchJobTime = measureTimeMillis {
                                Log.d("tttt", "cancel job...")
                                job?.cancel()
                                Log.d("tttt", "waiting job...")
                                job?.join()
                                Log.d("tttt", "launch job..")
                                job = launch { drawPicture(canvas, w ?: 0, h ?: 0, scale, ofsx, ofsy, palettes) }
                            }
                            Log.d("tttt", "relaunchJobTime: $relaunchJobTime ms")
                            tapHandled = true
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

suspend fun drawPicture(
    canvas: Canvas?,
    w: Int,
    h: Int,
    scale: Double,
    ofsx: Double,
    ofsy: Double,
    palettes: Array<Palette>
) {
    withContext(Dispatchers.Default) {

        //Draw iterative points
        var pass = 1
        var stepSize = 256                  
        while (stepSize > 2) {
            val passDuration = measureTimeMillis {
                val points = createPointsForPass(w ?: 0, h ?: 0, stepSize, pass, scale, ofsx, ofsy)
              //  delay(1000)
               // canvas?.drawColor(Color.Black.toArgb())

                stepRowsCount.set(points.yNum)
                stepRowsDone.set(0)

                val lineJobs = Array<Job?>(points.yNum) { null }

                for (y in 0 until points.yNum) {
                    lineJobs[y] = launch {
                        // Calculate line colors
                        for (x in 0 until points.xNum) {
                            val pointC = points[x, y]
                            val re = pointC.re
                            val im = pointC.im

                            val iterationsPercent = countMandelbrotIterations(re, im)
                            pointC.color = iterationsPercent

                            if (!isActive) {
                                Log.d("tttt", "break 2 Pass=$pass step=$stepSize x=$x y=$y")
                                break
                            }
                        }

                        if (isActive) {
                            //Draw line
                            val lPaint = Paint().apply {
                                strokeWidth = when (pass) {
                                    1 -> stepSize.toFloat()
                                    else -> stepSize / 2f
                                }
                            }
                            for (x in 0 until points.xNum) {
                                val pointC = points[x, y]
                                lPaint.color = if (pointC.color < 0f) {
                                    Color.Black.toArgb()
                                } else {
                                    palettes[4][pointC.color]
                                }

                                canvas?.drawPoint(pointC.x, pointC.y, lPaint)

                                if (!isActive) {
                                    Log.d("tttt", "break 3 Pass=$pass step=$stepSize x=$x y=$y")
                                    break
                                }
                            }
                        } else {
                            Log.d("tttt", "break 2.5 no draw Pass=$pass step=$stepSize y=$y")
                        }

                        val c = stepRowsDone.incrementAndGet()
                        val c1 = stepRowsCount.get()
                        val percent = c.toFloat() / c1
                        val percentDone: Int = (percent * 100).toInt()

                        val doLog =
                            ((percent * 1000).toInt() - percentDone * 10) < 1 && percentDone % 5 == 0

                        if (doLog) {
                            Log.d("tttt", "Step $percentDone% ready")
                        }
                    }
                    if (!isActive) {
                        Log.d("tttt", "break stop creating jobs Pass=$pass step=$stepSize y=$y of ${points.yNum}")
                        break
                    }
                }
                lineJobs.forEach { it?.join() }
            }
            if (stepSize < 256) {
                Log.d("tttt", "Pass=$pass step=$stepSize>>> DoneIn: $passDuration ms")
            }
            //update iteration variables
            pass++
            if (pass > 4 && (pass - 2) % 3 == 0) {
                stepSize /= 2
            }
            if (!isActive) {
                Log.d("tttt", "break 4")
                break
            }
            //  delay(1000)
        }
        val scaleStr = String.format("%.10f", scale)
        Log.d("tttt", "DONE iterative drawing ofsx=$ofsx ofsy=$ofsy scale=$scaleStr")
        //canvas?.drawColor(Color.Red.toArgb())
//        drawPalette(w, h, canvas!!, palette)
//        drawGuidelines(canvas, w, h, centerX, centerY, scale, ofsx, ofsy)

        ////////////////////////////////////////
        ///DRAW ALL
        ////////////////////////////////////////
        val drawAllPoints = true

        if (drawAllPoints) {
            val centerX = w / 2
            val centerY = h / 2
            val xPoints = w
            val yPoints = h
            val numPoints = xPoints * yPoints
            val colorsArray = FloatArray(numPoints) { 0.0f }
            val pointPaint = Paint().apply { strokeWidth = 1f }

            for (pY in 0 until yPoints) {
                for (pX in 0 until xPoints) {
                    val index = pX + pY * xPoints

                    //make points in screen coordinates
                    val x = pX.toFloat() + 0.0f
                    val y = pY.toFloat() + 0.5f
                    //store points in complex  plane coordinates
                    val re = (x - centerX) / scale - ofsx
                    val im = (y - centerY) / (-scale) - ofsy

                    val iterationsPercent = countMandelbrotIterations(re, im, 10_00)
                    colorsArray[index] = iterationsPercent
                    if (!isActive) {
                        Log.d("tttt", "break all calc at x=$x y=$pY")
                        break
                    }
                }

                if (!isActive) {
                    Log.d("tttt", "break all skip draw at y=$pY")
                    break
                }

                for (x in 0 until xPoints) {
                    val index = x + pY * xPoints
                    pointPaint.color = if (colorsArray[index] < 0f) {
                        Color.Black.toArgb()
                    } else {
                        palettes[3][colorsArray[index]]
                    }

                    canvas?.drawPoint(
                        x.toFloat() + 0.5f,
                        pY.toFloat() + 0.5f,
                        pointPaint,
                    )
                    if (!isActive) {
                        Log.d("tttt", "break all draw at x=$x y=$pY")
                        break
                    }
                }

                if (!isActive) {
                    Log.d("tttt", "break all at y=$pY")
                    break
                }
            }
            Log.d("tttt", "All points drawn")
        }
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
 * -1.0 means the point did not escape after maxIterations.
 * Other values are the percentage of iterations that were completed before the point escaped.
 */
private fun countMandelbrotIterations(
    cRe: Double,
    cIm: Double,
    maxIterations: Int = 1_00,
): Float {
    var iterations = 0
    var zR = cRe
    var zI = cIm
    var zAbs: Double = 0.0
    while (iterations < maxIterations) {
        val zRp = zR
        zR = zRp * zRp - zI * zI + cRe
        zI = zRp * zI + zI * zRp + cIm
        zAbs = sqrt(zR * zR + zI * zI)

        iterations++
        if (zAbs > 2) {
            break
        }
    }

    return if (iterations == maxIterations) {
        -1f
    } else {
        // Normalizing to reduce bands of color
        val iters = iterations + 1 - ln(log2(zAbs))
        (iters / maxIterations).toFloat()
    }
}

/**
 * Create points for a pass in screen coordinates
 * @param w width of the canvas
 * @param h height of the canvas
 * @param stepSize size of the step
 * @param pass pass number
 * @return array of points
 */
fun createPointsForPass(
    w: Int, h: Int, stepSize: Int, pass: Int, scale: Double,
    ofsx: Double,
    ofsy: Double,
): PassPoints {

    val centerX = w / 2
    val centerY = h / 2

    val xPoints = ceil(w.toFloat() / stepSize - 0.5f).toInt() + 1
    val yPoints = ceil(h.toFloat() / stepSize - 0.5f).toInt() + 1
    val numPoints = xPoints * yPoints

    val shiftx = when (pass) {
        1 -> 0f
        else -> if ((pass - 1) % 3 == 0) 0f else stepSize / 2f
    }

    val shifty = when (pass) {
        1 -> 0f
        else -> if (pass % 3 == 0) 0f else stepSize / 2f
    }

    val points = Array(2 * numPoints) { index ->
        val pX = index % xPoints
        val pY = index / xPoints
        //make points in screen coordinates
        val x = shiftx + (pX * stepSize).toFloat()
        val y = shifty + (pY * stepSize).toFloat()
        //store points in complex  plane coordinates
        val re = (x - centerX) / scale - ofsx
        val im = (y - centerY) / (-scale) - ofsy
        ColoredPoint(x, y, re, im, 0.0f)
    }

    return PassPoints(xPoints, yPoints, points)
}


class PassPoints(val xNum: Int, val yNum: Int, private val points: Array<ColoredPoint>) {
    val pointsNumber = xNum * yNum
    fun size() = pointsNumber
    operator fun get(index: Int): ColoredPoint = points[index]
    operator fun get(ix: Int, iy: Int) = this[ix + iy * xNum]
}

/**
 * Represts a point in the complex plane with a color
 * @param x x coordinate in screen coordinates
 * @param y y coordinate in screen coordinates
 * @param re real part of the complex number
 * @param im imaginary part of the complex number
 * @param color color of the point in range 0..1 or -1 (-1 means not escaped after maxIterations)
 */
data class ColoredPoint(val x: Float, val y: Float, val re: Double, val im: Double, var color: Float)


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