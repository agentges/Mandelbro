package by.agentges.mandelbro

/**
 * Represts a point in the complex plane with a color
 * @param x x coordinate in screen coordinates
 * @param y y coordinate in screen coordinates
 * @param re real part of the complex number
 * @param im imaginary part of the complex number
 * @param color color of the point in range 0..1 or -1 (-1 means not escaped after maxIterations)
 */
data class ColoredPoint(val x: Float, val y: Float, val re: Double, val im: Double, var color: Float)