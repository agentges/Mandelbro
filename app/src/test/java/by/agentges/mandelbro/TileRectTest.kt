package by.agentges.mandelbro

import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import kotlin.math.roundToInt

@RunWith(MockitoJUnitRunner::class)
class TileRectTest {


    val l = 500
    val t = 500
    val r = 628
    val b = 628


    val centerX = 500f
    val centerY = 500f
    val offsx = 0f
    val offsy = 0f
    val scale = 256f


    @Mock
    lateinit var mockedRect:Rect


    @Before
    fun setUp() {
        mockedRect.left=centerX.toInt()
        mockedRect.top=centerY.toInt()
        mockedRect.right=centerX.toInt() + scale.toInt()
        mockedRect.bottom=centerY.toInt() + scale.toInt()
        `when`(mockedRect.width()).thenReturn(mockedRect.right - mockedRect.left)
        `when`(mockedRect.height()).thenReturn(mockedRect.bottom - mockedRect.top)
    }


    @Test
    fun `to cartesian 0 0 1 -1`() {

//        var mockedRect = Rect(
//            centerX.toInt(),
//            centerY.toInt(),
//            centerX.toInt() + scale.toInt(),
//            centerY.toInt() + scale.toInt()
//        )
//        `when`(mockedRect.width()).thenReturn(mockedRect.right - mockedRect.left)
//        `when`(mockedRect.height()).thenReturn(mockedRect.bottom - mockedRect.top)
        val cartesianRect = TileRect(mockedRect, Color.Black)
            .toCartesian(centerX, centerY, offsx, offsy, scale)

        assert(cartesianRect.left.roundToInt() == 0)
        assert(cartesianRect.top.roundToInt() == 0)
        assert(cartesianRect.right.roundToInt() == 1)
        assert(cartesianRect.bottom.roundToInt() == -1)

    }
}
