package by.agentges.mandelbro

import android.graphics.Canvas
import android.graphics.Rect
import android.view.Surface

fun Surface.useCanvas(inOutDirty: Rect?, block: Canvas.() -> Unit) {
    val canvas = lockCanvas(inOutDirty)
    try {
        block(canvas)
    } finally {
        unlockCanvasAndPost(canvas)
    }
}