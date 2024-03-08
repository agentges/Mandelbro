package by.agentges.mandelbro

import android.graphics.Paint
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun RectPainting(viewModel: RectViewModel, modifier: Modifier = Modifier) {

    AndroidExternalSurface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        viewModel.onDragStart(offset)
                    },
                    onDragEnd = {
                        viewModel.onDragEnd()

                    },
                    onDragCancel = {
                        viewModel.onDragEnd()
                    },
                    onDrag = { _, dragAmount ->
                        viewModel.onDrag(dragAmount)
                    }
                )
            }

    ) {
        // Resources can be initialized/cached here
        val bitmapPaint = Paint()
        val rectPaint = Paint().apply {
            color = Color.Yellow.toArgb()
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint().apply {
            color = Color.White.toArgb()
            textSize = 48f
        }

        // A surface is available, we can start rendering
        onSurface { surface, width, height ->

            viewModel.onSurfaceCreated(width, height)

            // React to surface dimension changes
            surface.onChanged { newWidth, newHeight ->
                viewModel.onSurfaceSizeChanged(newWidth, newHeight)
            }

            // Cleanup if needed
            surface.onDestroyed {
                viewModel.onSurfaceDestroyed()
            }

            // Render loop, automatically cancelled on surface destruction
            while (true) {
                withFrameNanos { _ ->
                    surface.useCanvas(viewModel.fullRect) {
                        drawColor(Color.Black.toArgb())

                        //draw x axis
                        rectPaint.color = Color.Red.toArgb()
                        rectPaint.strokeWidth = 16f
                        drawLine(
                            0f,
                            viewModel.h / 2f + viewModel.offsy,
                            viewModel.w.toFloat(),
                            viewModel.h / 2f + viewModel.offsy,
                            rectPaint
                        )
                        //draw y axis
                        drawLine(
                            viewModel.w / 2f + viewModel.offsx,
                            0f,
                            viewModel.w / 2f + viewModel.offsx,
                            viewModel.h.toFloat(),
                            rectPaint
                        )

                        //draw first tile rect
                        rectPaint.style = Paint.Style.FILL

                        drawRect(viewModel.tileRects[0].rect, rectPaint)
                        rectPaint.strokeWidth = 1f
                        rectPaint.style = Paint.Style.STROKE



                        //Draw text 1
                        val x = viewModel.tileRects[0].rect.left
                        val y = viewModel.tileRects[0].rect.top
                        drawText("x: $x, y: $y", x.toFloat(), y.toFloat(), textPaint)

                        //Draw text 2
                        val x2 = viewModel.tileRects[0].rect.right
                        val y2 = viewModel.tileRects[0].rect.bottom
                        val text = viewModel.tileRects[0].toCartesian(
                            viewModel.fullRect.centerX(),
                            viewModel.fullRect.centerY(),
                            viewModel.offsx,
                            viewModel.offsy,
                            viewModel.scale
                        ).let{
                            "ox: ${viewModel.offsx}, oy: ${viewModel.offsy}, x: ${it.right}, y: ${it.bottom}"
                        }

                        drawText(text, x2.toFloat(), y2.toFloat(), textPaint)


                        viewModel.tileRects[0].toCartesian(
                            viewModel.fullRect.centerX(),
                            viewModel.fullRect.centerY(),
                            viewModel.offsx,
                            viewModel.offsy,
                            viewModel.scale
                        ).let {
                            // Log.d("RectPainting", "tileRects[0] = $it")
                        }




                        //draw all tile rects
                        viewModel.tileRects.forEach { tile ->
                            rectPaint.color = tile.color.toArgb()
                            drawRect(tile.rect, rectPaint)
                        }

                        /*
                                                synchronized(viewModel) {
                                                    bitmapPaint.color = Color.DarkGray.toArgb()
                                                    drawRect(viewModel.dst, bitmapPaint)

                                                    viewModel.bitmap?.let { bitmap ->
                                                        drawBitmap(
                                                            bitmap,
                                                            viewModel.src,
                                                            viewModel.dst,
                                                            bitmapPaint
                                                        )
                                                    }
                                                }
                        */
                    }
                }
            }
        }
    }
}

