package by.agentges.mandelbro

class PassPoints(val xNum: Int, val yNum: Int, private val points: Array<ColoredPoint>) {
    val pointsNumber = xNum * yNum
    fun size() = pointsNumber
    operator fun get(index: Int): ColoredPoint = points[index]
    operator fun get(ix: Int, iy: Int) = this[ix + iy * xNum]
}