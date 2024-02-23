package by.agentges.mandelbro

import kotlin.math.roundToInt

class Palette(size: Int, init: (Float) -> Int) {

    private val colors = IntArray(size) {
        init(it.toFloat() / (size - 1))
    }

    val size = colors.size

    operator fun get(t: Float) = colors[(t * colors.lastIndex).roundToInt()]
    operator fun get(index: Int) = colors[index]
}