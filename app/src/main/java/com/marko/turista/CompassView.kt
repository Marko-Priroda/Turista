package com.marko.turista

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Funkčný mapový kompas s čistým vizuálom inšpirovaným Organic Maps. */
class CompassView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private var azimuth = 0f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(238, 255, 255, 255)
        style = Paint.Style.FILL
        setShadowLayer(10f * density, 0f, 3f * density, Color.argb(90, 0, 0, 0))
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(210, 215, 217)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(75, 84, 88)
        strokeCap = Paint.Cap.ROUND
    }
    private val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(47, 55, 58)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val northPaint = Paint(cardinalPaint).apply { color = Color.rgb(225, 55, 55) }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(47, 55, 58)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(105, 112, 115)
        textAlign = Paint.Align.CENTER
    }
    private val redNeedlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(229, 57, 53)
        style = Paint.Style.FILL
    }
    private val darkNeedlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(65, 72, 75)
        style = Paint.Style.FILL
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        contentDescription = "Kompas"
    }

    fun setAzimuth(angle: Float) {
        azimuth = (angle + 360f) % 360f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        // Reserve a complete caption area inside the view, including font descenders.
        val footer = 70f * density
        val outerRadius = minOf(width / 2f - 16f * density, (height - footer - 16f * density) / 2f).coerceAtLeast(24f * density)
        val radius = (outerRadius - 13f * density).coerceAtLeast(12f * density)
        val centerY = 8f * density + outerRadius

        canvas.drawCircle(centerX, centerY, radius + 13f * density, backgroundPaint)
        canvas.drawCircle(centerX, centerY, radius + 13f * density, borderPaint)
        canvas.save()
        canvas.rotate(-azimuth, centerX, centerY)

        for (degree in 0 until 360 step 5) {
            val angle = Math.toRadians(degree.toDouble())
            val major = degree % 30 == 0
            val cardinal = degree % 90 == 0
            val inner = radius - when {
                cardinal -> 17f * density
                major -> 12f * density
                else -> 6f * density
            }
            tickPaint.strokeWidth = if (major) 2f * density else 1f * density
            canvas.drawLine(
                centerX + sin(angle).toFloat() * inner,
                centerY - cos(angle).toFloat() * inner,
                centerX + sin(angle).toFloat() * radius,
                centerY - cos(angle).toFloat() * radius,
                tickPaint
            )
        }

        cardinalPaint.textSize = 17f * density
        northPaint.textSize = 18f * density
        drawCardinal(canvas, "S", 0, centerX, centerY, radius, northPaint)
        drawCardinal(canvas, "V", 90, centerX, centerY, radius, cardinalPaint)
        drawCardinal(canvas, "J", 180, centerX, centerY, radius, cardinalPaint)
        drawCardinal(canvas, "Z", 270, centerX, centerY, radius, cardinalPaint)
        canvas.restore()

        drawNeedle(canvas, centerX, centerY, radius * 0.55f)
        valuePaint.textSize = 24f * density
        subtitlePaint.textSize = 12f * density
        canvas.drawText("${azimuth.toInt()}°", centerX, centerY + radius + 37f * density, valuePaint)
        canvas.drawText(directionName(azimuth), centerX, centerY + radius + 54f * density, subtitlePaint)

        val marker = Path().apply {
            moveTo(centerX, centerY - radius - 9f * density)
            lineTo(centerX - 6f * density, centerY - radius + 2f * density)
            lineTo(centerX + 6f * density, centerY - radius + 2f * density)
            close()
        }
        canvas.drawPath(marker, redNeedlePaint)
    }

    private fun drawNeedle(canvas: Canvas, cx: Float, cy: Float, length: Float) {
        val north = Path().apply {
            moveTo(cx, cy - length); lineTo(cx - 8f * density, cy); lineTo(cx + 8f * density, cy); close()
        }
        val south = Path().apply {
            moveTo(cx, cy + length); lineTo(cx - 8f * density, cy); lineTo(cx + 8f * density, cy); close()
        }
        canvas.drawPath(north, redNeedlePaint)
        canvas.drawPath(south, darkNeedlePaint)
        canvas.drawCircle(cx, cy, 8f * density, darkNeedlePaint)
        canvas.drawCircle(cx, cy, 3.5f * density, centerPaint)
    }

    private fun drawCardinal(canvas: Canvas, text: String, degree: Int, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val angle = Math.toRadians(degree.toDouble())
        val textRadius = radius - 34f * density
        val x = cx + sin(angle).toFloat() * textRadius
        val y = cy - cos(angle).toFloat() * textRadius - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, x, y, paint)
    }

    private fun directionName(value: Float): String = when {
        value >= 337.5f || value < 22.5f -> "Sever"
        value < 67.5f -> "Severovýchod"
        value < 112.5f -> "Východ"
        value < 157.5f -> "Juhovýchod"
        value < 202.5f -> "Juh"
        value < 247.5f -> "Juhozápad"
        value < 292.5f -> "Západ"
        else -> "Severozápad"
    }
}
