package com.jnetai.roguereel

import android.content.Context
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat

class IconGenerator {
    companion object {
        fun createAdaptiveIcon(context: Context): Drawable {
            val bgLayer = ColorDrawable(0xFF0A0A1A.toInt())
            val fgLayer = object : Drawable() {
                override fun draw(canvas: Canvas) {
                    val w = bounds.width().toFloat()
                    val h = bounds.height().toFloat()
                    val cx = w / 2
                    val cy = h / 2
                    val r = w * 0.3f

                    val ringPaint = Paint().apply {
                        color = 0xFF00FF88.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = w * 0.04f
                        alpha = 180
                    }
                    canvas.drawCircle(cx, cy, r, ringPaint)
                    canvas.drawCircle(cx, cy, r * 0.6f, ringPaint)

                    val playerPaint = Paint().apply {
                        color = 0xFF00CCFF.toInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, cy, r * 0.15f, playerPaint)

                    val linePaint = Paint().apply {
                        color = 0xFF00FF88.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = w * 0.02f
                    }
                    for (i in 0 until 8) {
                        val angle = Math.toRadians((i * 45).toDouble())
                        val sx = cx + Math.cos(angle) * r * 0.45f
                        val sy = cy + Math.sin(angle) * r * 0.45f
                        val ex = cx + Math.cos(angle) * r * 0.8f
                        val ey = cy + Math.sin(angle) * r * 0.8f
                        canvas.drawLine(sx.toFloat(), sy.toFloat(), ex.toFloat(), ey.toFloat(), linePaint)
                    }
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.graphics.drawable.AdaptiveIconDrawable(bgLayer, fgLayer)
            } else {
                fgLayer
            }
        }
    }
}
