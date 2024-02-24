package by.agentges.mandelbro

import android.graphics.Paint
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
fun ChessPainting(viewModel: ChessViewModel, modifier: Modifier = Modifier) {

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
        Log.d("tttt", "Init resources...")
        val bitmapPaint = Paint()

        // A surface is available, we can start rendering
        onSurface { surface, width, height ->
            Log.d("tttt", "OnSurface...")

            viewModel.onSurfaceCreated(width, height)

            // React to surface dimension changes
            surface.onChanged { newWidth, newHeight ->
                Log.d("tttt", "OnSurface>Changed")
                viewModel.onSurfaceSizeChanged(newWidth, newHeight)
            }

            // Cleanup if needed
            surface.onDestroyed {
                Log.d("tttt", "OnSurface>Destroyed")
                viewModel.onSurfaceDestroyed()
            }

            // Render loop, automatically cancelled on surface destruction
            while (true) {
                withFrameNanos { _ ->
                    surface.useCanvas(viewModel.fullRect) {
                        drawColor(Color.Blue.toArgb())

                        synchronized(viewModel) {
                            bitmapPaint.color = Color.Cyan.toArgb()
                            drawRect(viewModel.src, bitmapPaint)
                            bitmapPaint.color = Color.DarkGray.toArgb()
                            drawRect(viewModel.dest, bitmapPaint)

                            viewModel.bitmap?.let { bitmap ->
                                drawBitmap(
                                    bitmap,
                                    viewModel.src,
                                    viewModel.dest,
                                    bitmapPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

