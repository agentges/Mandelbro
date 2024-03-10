package by.agentges.mandelbro

import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
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
        val rectPaint = RestoreablePaint(
            Paint().apply {
                color = Color.Yellow.toArgb()
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
        )
        val textPaint = Paint().apply {
            color = Color.White.toArgb()
            textSize = 24f
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
                    surface.useCanvas(null) {
                        drawColor(Color.Black.toArgb())

                        //draw full rect
                        rectPaint.usePaint(
                            color = Color.White.toArgb(),
                            strokeWidth = 10f
                        ) { paint ->
                            drawRect(viewModel.fullRect, paint)
                        }

                        //draw x axis
                        rectPaint.usePaint(
                            color = Color.Red.toArgb(),
                            strokeWidth = 16f
                        ) {
                            drawLine(
                                viewModel.fullRect.left.toFloat(),
                                viewModel.fullRect.centerY() + viewModel.offsy.toFloat(),
                                viewModel.fullRect.left + viewModel.fullRect.width().toFloat(),
                                viewModel.fullRect.centerY() + viewModel.offsy.toFloat(),
                                it
                            )
                            //draw y axis
                            drawLine(
                                viewModel.fullRect.centerX() + viewModel.offsx.toFloat(),
                                viewModel.fullRect.top.toFloat(),
                                viewModel.fullRect.centerX() + viewModel.offsx.toFloat(),
                                viewModel.fullRect.top + viewModel.fullRect.height().toFloat(),
                                it
                            )
                        }


                        //draw all tile rects
                        viewModel.tileRects.forEach { tile ->
                            tile.draw(this, rectPaint)
                            drawText(
                                viewModel.screenRectToCartesian(tile.rect)
                                    .let { "${it.left.toFloat()}, ${it.top.toFloat()}" },
                                tile.rect.left.toFloat(),
                                tile.rect.top.toFloat(),
                                textPaint
                            )
                        }

                        rectPaint.usePaint(
                            color = Color.Green.toArgb(),
                            strokeWidth = 32f
                        ) { paint ->
                            repeat(21) { step ->
                                viewModel.cartesianPointToScreen(PointF((step - 10).toFloat(), (step - 10).toFloat()))
                                    .let { point ->
                                        drawPoint(point.x.toFloat(), point.y.toFloat(), paint)
                                    }
                            }
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

